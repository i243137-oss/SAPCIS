package controller;

import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * =============================================================================
 *  SubstituteController (UC-08: Assign a Substitute Teacher)
 * -----------------------------------------------------------------------------
 *  GRASP: Controller — coordinates the substitute workflow.
 *  GRASP: Creator — creates substitute_assignments records.
 *
 *  Two-Step Handshake:
 *   1. Admin proposes a substitute → status = PENDING
 *   2. Target Teacher accepts/rejects → status = ACCEPTED/REJECTED
 *
 *  Called by AdminDashboardController (propose) and TeacherDashboardController
 *  (accept/reject). NO SQL lives in the UI layer.
 * =============================================================================
 */
public class SubstituteController {

    /**
     * Finds ALL teachers who are free at the given day/time slot.
     *
     * Returns two tiers (sorted):
     *   1. "Qualified" teachers — those who already teach this courseCode
     *      (labelled with "★ Qualified")
     *   2. "Available" teachers — any other teacher who is free at that slot
     *      (labelled with "Available")
     *
     * A teacher is "free" if they have NO overlapping assignment on that day:
     *   NOT EXISTS (startTime < newEnd AND endTime > newStart)
     *
     * This ensures the admin always sees candidates even when no one is
     * formally qualified for the course.
     */
    public List<Map<String, String>> getFreeTeachers(String courseCode, String dayOfWeek,
                                                      String startTime, String endTime,
                                                      String excludeTeacherUid) throws SQLException {
        List<Map<String, String>> teachers = new ArrayList<>();

        // All teachers who are free at this slot (not the original teacher)
        String sql =
            "SELECT DISTINCT u.uid, u.name, "
          + "  CASE WHEN EXISTS ("
          + "    SELECT 1 FROM teacher_assignments ta2 "
          + "    WHERE ta2.teacherUid = u.uid AND ta2.courseCode = ?"
          + "  ) THEN 1 ELSE 0 END AS isQualified "
          + "FROM users u "
          + "WHERE u.role = 'Teacher' "
          + "  AND u.uid <> ? "
          + "  AND u.uid NOT IN ("
          + "      SELECT ta.teacherUid FROM teacher_assignments ta "
          + "      WHERE ta.dayOfWeek = ? "
          + "        AND CAST(ta.startTime AS TIME) < CAST(? AS TIME) "
          + "        AND CAST(ta.endTime   AS TIME) > CAST(? AS TIME)"
          + "  ) "
          + "ORDER BY isQualified DESC, u.name";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, courseCode);          // for qualification check
            s.setString(2, excludeTeacherUid);   // not the original teacher
            s.setString(3, dayOfWeek);           // overlap check day
            s.setString(4, endTime + ":00");     // overlap: existing.start < new.end
            s.setString(5, startTime + ":00");   // overlap: existing.end > new.start
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("uid",  rs.getString("uid"));
                    boolean qualified = rs.getInt("isQualified") == 1;
                    String label = (qualified ? "★ " : "") + rs.getString("name")
                                 + (qualified ? " (Qualified)" : " (Available)");
                    row.put("name", label);
                    teachers.add(row);
                }
            }
        }
        return teachers;
    }

    /**
     * Admin proposes a substitute teacher → creates a PENDING record.
     * GRASP Creator pattern: SubstituteController creates the assignment record.
     *
     * @return the generated substituteId
     */
    public String proposeSubstitute(int assignmentId, String originalTeacherUid,
                                     String substituteTeacherUid, String reason) throws SQLException {
        String subId = "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sql = "INSERT INTO substitute_assignments "
                   + "(substituteId, assignmentId, originalTeacherUid, substituteTeacherUid, "
                   + " status, reason, createdAt) "
                   + "VALUES (?, ?, ?, ?, 'PENDING', ?, GETDATE())";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, subId);
            s.setInt(2, assignmentId);
            s.setString(3, originalTeacherUid);
            s.setString(4, substituteTeacherUid);
            s.setString(5, reason);
            s.executeUpdate();
        }
        return subId;
    }

    /**
     * Teacher accepts a substitute request.
     * Updates status to ACCEPTED and optionally updates the class_sessions
     * to reflect the temporary teacher change.
     */
    public void acceptSubstitute(String substituteId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            // 1. Update substitute_assignments status
            try (PreparedStatement s = c.prepareStatement(
                    "UPDATE substitute_assignments SET status = 'ACCEPTED', respondedAt = GETDATE() "
                  + "WHERE substituteId = ?")) {
                s.setString(1, substituteId);
                s.executeUpdate();
            }

            // 2. Get assignment details to update class_sessions if needed
            // (The substitute is now handling this session)
            // This is informational — the actual timetable still shows original teacher
            // but class_sessions can track the substitute via a note or separate column.
        }
    }

    /**
     * Teacher rejects a substitute request.
     * The Admin can then see the rejection and reopen the search.
     */
    public void rejectSubstitute(String substituteId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "UPDATE substitute_assignments SET status = 'REJECTED', respondedAt = GETDATE() "
                   + "WHERE substituteId = ?")) {
            s.setString(1, substituteId);
            s.executeUpdate();
        }
    }

    /**
     * Gets all pending substitute requests (for Admin view — shows sessions
     * where teachers requested a sub or where status updates indicate need).
     */
    public List<Map<String, String>> getAllSubstituteRequests() throws SQLException {
        List<Map<String, String>> rows = new ArrayList<>();
        String sql =
            "SELECT sa.substituteId, sa.assignmentId, sa.status, sa.reason, "
          + "       sa.originalTeacherUid, u1.name AS originalName, "
          + "       sa.substituteTeacherUid, u2.name AS substituteName, "
          + "       ta.courseCode, ta.dayOfWeek, ta.startTime, ta.endTime, ta.roomId "
          + "FROM substitute_assignments sa "
          + "JOIN users u1 ON sa.originalTeacherUid = u1.uid "
          + "LEFT JOIN users u2 ON sa.substituteTeacherUid = u2.uid "
          + "JOIN teacher_assignments ta ON sa.assignmentId = ta.assignmentId "
          + "ORDER BY "
          + "  CASE sa.status WHEN 'PENDING' THEN 1 WHEN 'REJECTED' THEN 2 ELSE 3 END, "
          + "  sa.createdAt DESC";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            int colCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String colName = rs.getMetaData().getColumnLabel(i);
                    String val = rs.getString(i);
                    if (val != null) { val = val.trim(); if (colName.toLowerCase().contains("time") && val.length() > 5 && val.charAt(2) == ':') val = val.substring(0, 5); } else { val = ""; }
                    row.put(colName, val);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Gets sessions that need a substitute:
     * teacher_assignments whose class_sessions status = 'CANCELLED' or 'DELAYED',
     * AND don't yet have an ACCEPTED substitute.
     */
    public List<Map<String, String>> getSessionsNeedingSub() throws SQLException {
        List<Map<String, String>> rows = new ArrayList<>();
        // Shows sessions needing a substitute from TWO sources:
        // 1. Sessions whose class_sessions status = CANCELLED or DELAYED
        // 2. Sessions where the teacher explicitly requested a substitute (REQUESTED_BY_TEACHER)
        // Excludes sessions that already have an ACCEPTED substitute.
        String sql =
            "SELECT DISTINCT ta.assignmentId, ta.teacherUid, u.name AS teacherName, "
          + "       ta.courseCode, c.courseName, ta.dayOfWeek, "
          + "       ta.startTime, ta.endTime, ta.roomId, ta.sectionName, "
          + "       CASE "
          + "         WHEN EXISTS (SELECT 1 FROM substitute_assignments sa2 "
          + "                      WHERE sa2.assignmentId = ta.assignmentId "
          + "                        AND sa2.status = 'REQUESTED_BY_TEACHER') "
          + "           THEN 'REQUESTED BY TEACHER' "
          + "         ELSE COALESCE(("
          + "           SELECT TOP 1 cs.status FROM class_sessions cs "
          + "           WHERE cs.courseId = ta.courseCode AND cs.roomNumber = ta.roomId "
          + "             AND cs.startTime = ta.startTime"
          + "         ), 'SCHEDULED') "
          + "       END AS sessionStatus "
          + "FROM teacher_assignments ta "
          + "JOIN users u ON ta.teacherUid = u.uid "
          + "JOIN courses c ON ta.courseCode = c.courseCode "
          + "WHERE ("
          + "    COALESCE(("
          + "        SELECT TOP 1 cs2.status FROM class_sessions cs2 "
          + "        WHERE cs2.courseId = ta.courseCode AND cs2.roomNumber = ta.roomId "
          + "          AND cs2.startTime = ta.startTime"
          + "    ), 'SCHEDULED') IN ('CANCELLED', 'DELAYED') "
          + "    OR "
          + "    EXISTS (SELECT 1 FROM substitute_assignments sa3 "
          + "            WHERE sa3.assignmentId = ta.assignmentId "
          + "              AND sa3.status = 'REQUESTED_BY_TEACHER')"
          + ") "
          + "AND ta.assignmentId NOT IN ("
          + "    SELECT sa.assignmentId FROM substitute_assignments sa WHERE sa.status = 'ACCEPTED'"
          + ") "
          + "ORDER BY ta.teacherUid, ta.startTime";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            int colCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String colName = rs.getMetaData().getColumnLabel(i);
                    String val = rs.getString(i);
                    if (val != null) { val = val.trim(); if (colName.toLowerCase().contains("time") && val.length() > 5 && val.charAt(2) == ':') val = val.substring(0, 5); } else { val = ""; }
                    row.put(colName, val);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Returns all teacher_assignments for a given teacher (for the "My Sessions" table).
     * Used by the teacher to pick which session they need a substitute for.
     */
    public List<Map<String, String>> getMyAssignments(String teacherUid) throws SQLException {
        List<Map<String, String>> rows = new ArrayList<>();
        String sql =
            "SELECT ta.assignmentId, ta.courseCode, c.courseName, ta.sectionName, "
          + "       ta.dayOfWeek, ta.startTime, ta.endTime, ta.roomId "
          + "FROM teacher_assignments ta "
          + "JOIN courses c ON c.courseCode = ta.courseCode "
          + "WHERE ta.teacherUid = ? "
          + "ORDER BY "
          + "  CASE ta.dayOfWeek "
          + "    WHEN 'Monday' THEN 1 WHEN 'Tuesday' THEN 2 WHEN 'Wednesday' THEN 3 "
          + "    WHEN 'Thursday' THEN 4 WHEN 'Friday' THEN 5 ELSE 6 END, "
          + "  ta.startTime";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, teacherUid);
            try (ResultSet rs = s.executeQuery()) {
                int colCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        String colName = rs.getMetaData().getColumnLabel(i);
                        String val = rs.getString(i);
                        if (val != null) { val = val.trim(); if (colName.toLowerCase().contains("time") && val.length() > 5 && val.charAt(2) == ':') val = val.substring(0, 5); } else { val = ""; }
                        row.put(colName, val);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    /**
     * Teacher requests a substitute for one of their own sessions.
     * Creates a PENDING record with originalTeacherUid = substituteTeacherUid = teacherUid
     * (placeholder — admin will assign the actual substitute later).
     * Status is set to 'REQUESTED_BY_TEACHER' so admin can distinguish it.
     *
     * @return the generated substituteId
     */
    public String requestSubstituteForSelf(int assignmentId, String teacherUid,
                                            String reason) throws SQLException {
        String subId = "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sql = "INSERT INTO substitute_assignments "
                   + "(substituteId, assignmentId, originalTeacherUid, substituteTeacherUid, "
                   + " status, reason, createdAt) "
                   + "VALUES (?, ?, ?, ?, 'REQUESTED_BY_TEACHER', ?, GETDATE())";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, subId);
            s.setInt(2, assignmentId);
            s.setString(3, teacherUid);
            s.setString(4, teacherUid);   // placeholder — admin will assign actual sub
            s.setString(5, reason);
            s.executeUpdate();
        }
        return subId;
    }

    /**
     * Gets pending substitute requests for a specific teacher (their inbox).
     */
    public List<Map<String, String>> getPendingRequestsForTeacher(String teacherUid) throws SQLException {
        List<Map<String, String>> rows = new ArrayList<>();
        String sql =
            "SELECT sa.substituteId, sa.assignmentId, sa.reason, sa.status, "
          + "       sa.originalTeacherUid, u1.name AS originalName, "
          + "       ta.courseCode, c.courseName, ta.dayOfWeek, "
          + "       ta.startTime, ta.endTime, ta.roomId "
          + "FROM substitute_assignments sa "
          + "JOIN users u1 ON sa.originalTeacherUid = u1.uid "
          + "JOIN teacher_assignments ta ON sa.assignmentId = ta.assignmentId "
          + "JOIN courses c ON ta.courseCode = c.courseCode "
          + "WHERE sa.substituteTeacherUid = ? AND sa.status = 'PENDING' "
          + "ORDER BY sa.createdAt DESC";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, teacherUid);
            try (ResultSet rs = s.executeQuery()) {
                int colCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        String colName = rs.getMetaData().getColumnLabel(i);
                        String val = rs.getString(i);
                        if (val != null) { val = val.trim(); if (colName.toLowerCase().contains("time") && val.length() > 5 && val.charAt(2) == ':') val = val.substring(0, 5); } else { val = ""; }
                        row.put(colName, val);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
