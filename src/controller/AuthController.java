package controller;

import db.DBConnection;
import model.Student;
import model.StudentData;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * // GRASP Pattern: Controller
 * Handles Student Registration and Auto-Enrollment via raw JDBC Transactions.
 * BUG FIX: login() was using try-with-resources on getInstance() which closed the shared connection.
 * Now uses getConnection() for independent connections.
 */
public class AuthController {

    /**
     * Registers a student and automatically enrolls them in courses mapped to their section.
     * Uses a single transaction for atomicity.
     */
    public boolean registerStudent(StudentData data) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insert into users table
            String userQuery = "INSERT INTO users (uid, name, email, role, password) VALUES (?, ?, ?, 'Student', ?)";
            try (PreparedStatement userStmt = conn.prepareStatement(userQuery)) {
                userStmt.setString(1, data.getUid());
                userStmt.setString(2, data.getName());
                userStmt.setString(3, data.getEmail());
                userStmt.setString(4, data.getPassword());
                userStmt.executeUpdate();
            }

            // 2. Insert into students table
            String studentQuery = "INSERT INTO students (uid, rollNo, batch, dept, section) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement studentStmt = conn.prepareStatement(studentQuery)) {
                studentStmt.setString(1, data.getUid());
                studentStmt.setString(2, data.getRollNo());
                studentStmt.setString(3, data.getBatch());
                studentStmt.setString(4, data.getDept());
                studentStmt.setString(5, data.getSection());
                studentStmt.executeUpdate();
            }

            // 3. Auto-Enrollment Logic
            String mappingQuery = "SELECT courseId FROM section_course_assignments WHERE section = ?";
            List<String> courseIds = new ArrayList<>();
            try (PreparedStatement mappingStmt = conn.prepareStatement(mappingQuery)) {
                mappingStmt.setString(1, data.getSection());
                try (ResultSet rs = mappingStmt.executeQuery()) {
                    while (rs.next()) {
                        courseIds.add(rs.getString("courseId"));
                    }
                }
            }

            // For each course, find the matching class_sessions and enroll
            // the student in timetable_db.
            //
            // IMPORTANT: class_sessions.sectionId holds the sections table's
            // sectionId (PK), not the human-readable sectionName. A student
            // just picked a sectionName like "A" on the signup form, so we
            // must JOIN through sections to resolve it.
            //
            // We also LEFT JOIN so that sessions which haven't been bound to
            // a concrete sections row yet (but whose sectionId happens to
            // equal the sectionName directly) are still picked up.
            String sessionQuery =
                    "SELECT DISTINCT cs.sessionId " +
                    "FROM class_sessions cs " +
                    "LEFT JOIN sections s ON s.sectionId = cs.sectionId " +
                    "WHERE cs.courseId = ? " +
                    "  AND (LTRIM(RTRIM(s.sectionName)) = ? " +
                    "    OR LTRIM(RTRIM(cs.sectionId))  = ?)";

            String enrollQuery = "INSERT INTO timetable_db (entryId, sessionId, dataType, dataValue) VALUES (?, ?, 'STUDENT_ENROLLMENT', ?)";

            int enrolledCount = 0;
            for (String courseId : courseIds) {
                try (PreparedStatement sessionStmt = conn.prepareStatement(sessionQuery)) {
                    sessionStmt.setString(1, courseId);
                    sessionStmt.setString(2, data.getSection());
                    sessionStmt.setString(3, data.getSection());
                    try (ResultSet rs = sessionStmt.executeQuery()) {
                        while (rs.next()) {
                            String sessionId = rs.getString("sessionId");
                            try (PreparedStatement enrollStmt = conn.prepareStatement(enrollQuery)) {
                                enrollStmt.setString(1, UUID.randomUUID().toString());
                                enrollStmt.setString(2, sessionId);
                                enrollStmt.setString(3, data.getUid());
                                enrollStmt.executeUpdate();
                                enrolledCount++;
                            }
                        }
                    }
                }
            }
            System.out.println("Auto-enrolled " + enrolledCount
                    + " class sessions for " + data.getName()
                    + " (section=" + data.getSection()
                    + ", courses=" + courseIds.size() + ").");

            conn.commit();
            System.out.println("Registration and Auto-Enrollment successful for: " + data.getName());
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * Fetches all batch years from the batches table (admin-managed).
     * Falls back to hardcoded defaults only if the table is empty.
     */
    public List<String> getBatches() {
        List<String> batches = fetchDistinct(
                "SELECT batchYear FROM batches ORDER BY batchYear DESC", "batchYear");
        if (batches.isEmpty()) {
            batches.add("2025");
            batches.add("2024");
            batches.add("2023");
            batches.add("2022");
        }
        return batches;
    }

    /**
     * Fetches departments that have at least one section for the given batch.
     * Uses batch_dept_sections → departments join so only relevant depts appear.
     * Falls back to all departments if none found for this batch.
     */
    public List<String> getDepartmentsForBatch(String batch) {
        if (batch == null || batch.trim().isEmpty()) return new ArrayList<>();
        List<String> depts = new ArrayList<>();
        // Find the batchId for this batchYear first
        String sql =
            "SELECT DISTINCT d.deptName "
          + "FROM departments d "
          + "JOIN batch_dept_sections bds ON bds.deptId = d.deptId "
          + "JOIN batches b ON b.batchId = bds.batchId "
          + "WHERE b.batchYear = ? "
          + "ORDER BY d.deptName ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, batch.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String v = rs.getString("deptName");
                    if (v != null && !v.trim().isEmpty()) depts.add(v.trim());
                }
            }
        } catch (SQLException e) {
            System.err.println("getDepartmentsForBatch(" + batch + ") failed: " + e.getMessage());
        }
        // Fallback: return all departments
        if (depts.isEmpty()) {
            depts = fetchDistinct("SELECT deptName FROM departments ORDER BY deptName ASC", "deptName");
        }
        return depts;
    }

    /**
     * @deprecated Use getDepartmentsForBatch(batch) for cascaded loading.
     */
    public List<String> getDepartments() {
        List<String> depts = fetchDistinct(
                "SELECT deptName FROM departments ORDER BY deptName ASC", "deptName");
        if (depts.isEmpty()) {
            depts.add("Computer Science");
            depts.add("Software Engineering");
            depts.add("Information Technology");
            depts.add("Artificial Intelligence");
        }
        return depts;
    }

    /**
     * Fetches ALL distinct section names present in the system.
     * Prefer {@link #getSectionsFor(String, String)} which returns only the
     * sections that belong to a specific batch+department.
     */
    public List<String> getSections() {
        List<String> list = fetchDistinct(
                "SELECT DISTINCT sectionName AS section FROM sections WHERE sectionName IS NOT NULL AND LTRIM(RTRIM(sectionName)) <> '' ORDER BY sectionName ASC",
                "section");
        if (list.isEmpty()) {
            list = fetchDistinct(
                    "SELECT DISTINCT section FROM students WHERE section IS NOT NULL AND LTRIM(RTRIM(section)) <> '' ORDER BY section ASC",
                    "section");
        }
        if (list.isEmpty()) {
            list.add("A");
            list.add("B");
            list.add("C");
            list.add("D");
        }
        return list;
    }

    /**
     * Returns only the sections that are associated with the given
     * batch + department combination. Each section belongs to a specific
     * batch of a specific department, so the dropdown must cascade.
     *
     * If the combination has no existing students yet (brand new batch),
     * a sensible default list of A..D is returned so registration can still
     * proceed and seed the data.
     */
    /**
     * Returns sections from batch_dept_sections for the given batchYear + deptName.
     * This is the authoritative source — admin-managed, not student-derived.
     */
    public List<String> getSectionsFor(String batchYear, String deptName) {
        if (batchYear == null || deptName == null
                || batchYear.trim().isEmpty() || deptName.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> sections = new ArrayList<>();
        // Join batches (batchYear) + departments (deptName) → batch_dept_sections
        String sql =
            "SELECT bds.sectionName "
          + "FROM batch_dept_sections bds "
          + "JOIN batches b ON b.batchId = bds.batchId "
          + "JOIN departments d ON d.deptId = bds.deptId "
          + "WHERE b.batchYear = ? AND d.deptName = ? "
          + "ORDER BY bds.sectionName ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, batchYear.trim());
            stmt.setString(2, deptName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String s = rs.getString("sectionName");
                    if (s != null && !s.trim().isEmpty()) sections.add(s.trim());
                }
            }
        } catch (SQLException e) {
            System.err.println("getSectionsFor(" + batchYear + "," + deptName + ") failed: " + e.getMessage());
        }

        // Fallback: if admin hasn't set up sections yet, offer A-D
        if (sections.isEmpty()) {
            sections.add("A");
            sections.add("B");
            sections.add("C");
            sections.add("D");
        }
        return sections;
    }

    /**
     * Shared helper for running a one-column SELECT and returning a list.
     */
    private List<String> fetchDistinct(String sql, String columnAlias) {
        List<String> values = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String v = rs.getString(columnAlias);
                if (v != null && !v.trim().isEmpty()) {
                    values.add(v.trim());
                }
            }
        } catch (SQLException e) {
            System.err.println("fetchDistinct failed [" + sql + "]: " + e.getMessage());
        }
        return values;
    }

    /**
     * Fetches the full User record (as a concrete {@link Student} instance —
     * any role works, it just carries uid/name/email/role) for the given
     * email. Used by the UI layer to populate {@code utils.UserSession}
     * after a successful login.
     */
    public User loadUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return null;
        String sql = "SELECT uid, name, email, role, password FROM users "
                   + "WHERE LTRIM(RTRIM(email)) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // We use Student as a concrete carrier for the abstract
                    // User — consumers should only rely on User getters.
                    Student u = new Student();
                    u.setUid(rs.getString("uid"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    u.setRole(rs.getString("role"));
                    u.setPassword(rs.getString("password"));
                    return u;
                }
            }
        } catch (SQLException e) {
            System.err.println("loadUserByEmail failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Login validation — returns role on success, null on failure.
     */
    public String login(String email, String password) {
        // Added TRIM() to handle any accidental spaces in the DB columns
        String query = "SELECT uid, name, role FROM users WHERE LTRIM(RTRIM(email)) = ? AND LTRIM(RTRIM(password)) = ?";
        
        System.out.println("Attempting login for: [" + email + "]");
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email.trim());
            stmt.setString(2, password.trim());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    System.out.println("Login Successful! Found user with role: " + role);
                    return role;
                } else {
                    System.out.println("Login Failed: No user found with those credentials in the 'users' table.");
                }
            }
        } catch (SQLException e) {
            System.err.println("DATABASE CONNECTION ERROR: " + e.getMessage());
            System.err.println("Please check your DBConnection.java settings (URL, User, Password).");
        }
        return null;
    }
}
