package controller;

import db.DBConnection;
import model.ClassSession;
import model.ScheduleEntry;
import service.NotificationService;
import dao.TimetableDBDAO;
import dao.ClassSessionDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * // GRASP Pattern: Controller + Pure Fabrication (NotificationService) + GoF Observer Pattern
 * Used in UC-01 (Teacher Dashboard — report delays / cancellations).
 *
 * Phase 5 additions:
 *   • getScheduleForTeacher(teacherUid) — returns the teacher's weekly
 *     timetable as {@link ScheduleEntry} rows so the FXML TableViews can
 *     bind directly. Resolves per (dept, batch, section, course) using
 *     course_teacher_assignments (same table used by the Student Dashboard).
 *   • reportCancellation(...) — thin wrapper over updateSessionStatus so
 *     the UI can push a CANCELLED status AND trigger the Observer notify.
 *
 * The reporting methods use the GoF Observer Pattern via
 * {@link NotificationService#pushAlerts}, which itself only pushes to the
 * subset of enrolled students whose {@code smartAlertsSubscribed} flag is
 * true (subscription-aware broadcast).
 */
public class TeacherController {

    private final NotificationService notificationService;
    private final TimetableDBDAO      timetableDB;
    private final ClassSessionDAO     classSessionDAO;

    public TeacherController() {
        this.notificationService = new NotificationService();
        this.timetableDB         = new TimetableDBDAO();
        this.classSessionDAO     = new ClassSessionDAO();
    }

    // ------------------------------------------------------------------
    //  Legacy API (kept for backward compatibility)
    // ------------------------------------------------------------------

    public void openDashboard() {
        System.out.println("TeacherController: Dashboard opened.");
    }

    public void selectClass(String classId) {
        System.out.println("TeacherController: Class " + classId + " selected.");
    }

    public ClassSession getSessionDetails(String classId) throws Exception {
        return classSessionDAO.getSessionDetails(classId);
    }

    public void showDelayConfirmation() {
        System.out.println("TeacherController: Delay confirmation shown.");
    }

    // ------------------------------------------------------------------
    //  Status-update API (UC-01 main flow)
    // ------------------------------------------------------------------

    /**
     * Marks the given class session as {@code DELAYED} with the supplied
     * estimated-time / reason and fires the Observer-pattern alert cascade.
     *
     * <pre>
     *   GoF Observer Pattern Triggered:
     *     NotificationService.pushAlerts(students, message)
     *     iterates every enrolled student and dispatches ONLY to those
     *     with Smart-Alerts subscription enabled.
     * </pre>
     */
    public void reportDelay(String classId, String estimatedTime) {
        try {
            // PRIMARY: update the class_sessions.status column (this is what
            // the dashboard reads to display the current status).
            classSessionDAO.updateSessionStatus(classId, "DELAYED");

            // SECONDARY: also write to timetable_db for legacy compatibility.
            timetableDB.updateSessionStatus(classId, "DELAYED", estimatedTime);
            notificationService.notifyStudents(classId, "DELAYED", estimatedTime);

            List<String> studentList = timetableDB.fetchEnrolledStudents(classId);
            notificationService.pushAlerts(
                    studentList,
                    "Class " + classId + " is DELAYED. Est time: " + estimatedTime);

            showDelayConfirmation();
        } catch (Exception e) {
            System.err.println("Error reporting delay: " + e.getMessage());
        }
    }

    /**
     * Marks the given class session as {@code CANCELLED} with the supplied
     * reason and fires the Observer-pattern alert cascade (subscription-aware).
     */
    public void reportCancellation(String classId, String reason) {
        try {
            // PRIMARY: update the class_sessions.status column (this is what
            // the dashboard reads to display the current status).
            classSessionDAO.updateSessionStatus(classId, "CANCELLED");

            // SECONDARY: also write to timetable_db for legacy compatibility.
            timetableDB.updateSessionStatus(classId, "CANCELLED", reason);
            notificationService.notifyStudents(classId, "CANCELLED", reason);

            List<String> studentList = timetableDB.fetchEnrolledStudents(classId);
            notificationService.pushAlerts(
                    studentList,
                    "Class " + classId + " has been CANCELLED. Reason: " + reason);

            System.out.println("TeacherController: Cancellation confirmation shown.");
        } catch (Exception e) {
            System.err.println("Error reporting cancellation: " + e.getMessage());
        }
    }

    /**
     * Generic status updater used by the Teacher Dashboard "Submit Update" button.
     *
     * The {@code sessionId} parameter may be either:
     *   (a) A real class_sessions.sessionId (legacy path, e.g. "SES-MON-CS101-A")
     *   (b) A teacher_assignments.assignmentId cast to String (e.g. "3")
     *       — this happens when the schedule is loaded from teacher_assignments.
     *
     * This method handles both cases:
     *   1. Try to update class_sessions directly by sessionId.
     *   2. If that updates 0 rows (or the ID is numeric), look up the
     *      teacher_assignment by assignmentId and update class_sessions
     *      by (courseId + sectionName + startTime).
     *   3. If still no class_session row exists, INSERT one so the status
     *      is persisted and visible on all dashboards.
     */
    public void updateSessionStatus(String sessionId, String newStatus, String reasonOrEta) {
        if (sessionId == null || newStatus == null) return;
        String status = newStatus.trim().toUpperCase();

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int updated = 0;

                // ── Path A: try direct sessionId update ──────────────────────
                boolean isNumeric = sessionId.matches("\\d+");
                if (!isNumeric) {
                    try (PreparedStatement u = conn.prepareStatement(
                            "UPDATE class_sessions SET status = ? WHERE sessionId = ?")) {
                        u.setString(1, status);
                        u.setString(2, sessionId);
                        updated = u.executeUpdate();
                    }
                }

                // ── Path B: look up by assignmentId ──────────────────────────
                if (updated == 0) {
                    // Resolve assignment details
                    String courseCode = null, sectionName = null, startTime = null,
                           endTime = null, roomId = null, teacherUid = null, dayOfWeek = null;

                    String lookupSql = isNumeric
                        ? "SELECT courseCode, sectionName, startTime, endTime, roomId, teacherUid, dayOfWeek FROM teacher_assignments WHERE assignmentId = ?"
                        : "SELECT courseCode, sectionName, startTime, endTime, roomId, teacherUid, dayOfWeek FROM teacher_assignments WHERE assignmentId = (SELECT TOP 1 assignmentId FROM teacher_assignments WHERE courseCode = (SELECT TOP 1 courseId FROM class_sessions WHERE sessionId = ?))";

                    try (PreparedStatement q = conn.prepareStatement(
                            "SELECT courseCode, sectionName, startTime, endTime, roomId, teacherUid, dayOfWeek " +
                            "FROM teacher_assignments WHERE assignmentId = ?")) {
                        if (isNumeric) {
                            q.setInt(1, Integer.parseInt(sessionId));
                        } else {
                            q.setString(1, sessionId);
                        }
                        try (ResultSet rs = q.executeQuery()) {
                            if (rs.next()) {
                                courseCode  = rs.getString("courseCode");
                                sectionName = rs.getString("sectionName");
                                startTime   = rs.getString("startTime");
                                endTime     = rs.getString("endTime");
                                roomId      = rs.getString("roomId");
                                teacherUid  = rs.getString("teacherUid");
                                dayOfWeek   = rs.getString("dayOfWeek");
                                if (startTime != null && startTime.length() > 5) startTime = startTime.substring(0, 5);
                                if (endTime   != null && endTime.length()   > 5) endTime   = endTime.substring(0, 5);
                            }
                        }
                    }

                    if (courseCode != null) {
                        // Try UPDATE by courseId + sectionName + startTime
                        try (PreparedStatement u = conn.prepareStatement(
                                "UPDATE class_sessions SET status = ? " +
                                "WHERE courseId = ? " +
                                "  AND CAST(startTime AS VARCHAR(5)) = ? " +
                                "  AND sectionId IN (" +
                                "      SELECT sectionId FROM sections " +
                                "      WHERE UPPER(LTRIM(RTRIM(sectionName))) = UPPER(LTRIM(RTRIM(?)))" +
                                "  )")) {
                            u.setString(1, status);
                            u.setString(2, courseCode);
                            u.setString(3, startTime);
                            u.setString(4, sectionName);
                            updated = u.executeUpdate();
                        }

                        // If still 0 rows — INSERT a new class_session row
                        if (updated == 0 && startTime != null) {
                            String newSessionId = "SES-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                            String slot = (dayOfWeek != null ? dayOfWeek : "") + " " + startTime;
                            try (PreparedStatement ins = conn.prepareStatement(
                                    "INSERT INTO class_sessions (sessionId, startTime, endTime, status, timetableSlot, roomNumber, courseId, sectionId) " +
                                    "SELECT ?, CAST(? AS TIME), CAST(? AS TIME), ?, ?, ?, ?, s.sectionId " +
                                    "FROM sections s " +
                                    "WHERE UPPER(LTRIM(RTRIM(s.sectionName))) = UPPER(LTRIM(RTRIM(?)))")) {
                                ins.setString(1, newSessionId);
                                ins.setString(2, startTime);
                                ins.setString(3, endTime != null ? endTime : startTime);
                                ins.setString(4, status);
                                ins.setString(5, slot);
                                ins.setString(6, roomId != null ? roomId : "");
                                ins.setString(7, courseCode);
                                ins.setString(8, sectionName);
                                ins.executeUpdate();
                                updated = 1;
                            }
                        }
                    }
                }

                // ── Notifications ─────────────────────────────────────────────
                if (updated > 0 && ("DELAYED".equals(status) || "CANCELLED".equals(status))) {
                    // Notify via legacy path (best-effort)
                    try {
                        timetableDB.updateSessionStatus(sessionId, status, reasonOrEta);
                        notificationService.notifyStudents(sessionId, status, reasonOrEta);
                        List<String> students = timetableDB.fetchEnrolledStudents(sessionId);
                        notificationService.pushAlerts(students,
                                "Class " + sessionId + " is " + status +
                                (reasonOrEta != null && !reasonOrEta.isEmpty() ? ". " + reasonOrEta : ""));
                    } catch (Exception ignored) { /* best-effort */ }
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw new RuntimeException(ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.err.println("updateSessionStatus(" + status + ") failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  Schedule query (UC-01 read-side)
    // ------------------------------------------------------------------

    /**
     * Returns the weekly schedule for the given teacher.  A row is included
     * if the teacher is linked to the (dept, batch, section, course) tuple
     * via the {@code course_teacher_assignments} table, OR — as a legacy
     * fallback — via the single {@code sections.teacherId} column.
     */
    /**
     * Returns the weekly schedule for the given teacher.
     *
     * SOURCE OF TRUTH: teacher_assignments table.
     * This ensures that when admin changes a class time/day via Emergency Override,
     * the teacher's schedule is immediately updated (no stale class_sessions data).
     *
     * Status is fetched from class_sessions as a correlated subquery (best-effort).
     * If no matching class_session exists, status defaults to "UPCOMING".
     */
    public List<ScheduleEntry> getScheduleForTeacher(String teacherUid) {
        List<ScheduleEntry> rows = new ArrayList<>();
        if (teacherUid == null) return rows;

        // Read directly from teacher_assignments — always up-to-date after admin changes.
        // Status comes from class_sessions via correlated subquery.
        // Match by courseId + sectionName + startTime (NOT roomId — roomId changes after room swap).
        final String sql =
                "SELECT ta.assignmentId, " +
                "       ta.dayOfWeek, " +
                "       ta.startTime, " +
                "       ta.endTime, " +
                "       ta.sectionName, " +
                "       ta.courseCode, " +
                "       COALESCE(c.courseName, ta.courseCode) AS subject, " +
                "       COALESCE(cr.roomName,  ta.roomId)     AS room, " +
                "       COALESCE((" +
                "           SELECT TOP 1 cs.status " +
                "           FROM class_sessions cs " +
                "           WHERE cs.courseId = ta.courseCode " +
                "             AND CAST(cs.startTime AS VARCHAR(5)) = CAST(ta.startTime AS VARCHAR(5))" +
                "             AND cs.sectionId IN (" +
                "                 SELECT sectionId FROM sections " +
                "                 WHERE UPPER(LTRIM(RTRIM(sectionName))) = UPPER(LTRIM(RTRIM(ta.sectionName)))" +
                "             )" +
                "       ), 'UPCOMING') AS status " +
                "FROM teacher_assignments ta " +
                "LEFT JOIN courses    c  ON c.courseCode = ta.courseCode " +
                "LEFT JOIN classrooms cr ON cr.roomId    = ta.roomId " +
                "WHERE ta.teacherUid = ? " +
                "ORDER BY ta.dayOfWeek, ta.startTime";

        // Current time for live status computation
        java.time.LocalTime now = java.time.LocalTime.now();
        String todayName = java.time.LocalDate.now()
                .getDayOfWeek()
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teacherUid);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String assignmentId = String.valueOf(rs.getInt("assignmentId"));
                    String subject   = rs.getString("subject");
                    String room      = rs.getString("room");
                    String day       = rs.getString("dayOfWeek");
                    String dbStatus  = rs.getString("status");
                    String secLabel  = rs.getString("sectionName");
                    Time   startTime = rs.getTime("startTime");
                    Time   endTime   = rs.getTime("endTime");

                    String slot = day + " " + formatHHmm(startTime);
                    String time = formatTimeRange(startTime, endTime, slot);

                    // Compute live status from real clock
                    String st = computeLiveStatus(dbStatus, day, startTime, endTime, todayName, now);

                    rows.add(new ScheduleEntry(assignmentId, subject, secLabel, room, time, day, st));
                }
            }
        } catch (SQLException e) {
            System.err.println("getScheduleForTeacher failed: " + e.getMessage());
        }

        System.out.println("[TeacherDashboard] getScheduleForTeacher(uid="
                + teacherUid + ") -> " + rows.size() + " row(s).");
        return rows;
    }

    /**
     * Sub-list of {@link #getScheduleForTeacher} filtered by weekday name.
     */
    public List<ScheduleEntry> getScheduleForTeacherOn(String teacherUid, String day) {
        List<ScheduleEntry> all = getScheduleForTeacher(teacherUid);
        List<ScheduleEntry> out = new ArrayList<>();
        if (day == null || day.length() < 3) return out;
        String target = day.trim().toLowerCase().substring(0, 3);
        for (ScheduleEntry e : all) {
            if (e.getDay() != null && e.getDay().trim().toLowerCase().startsWith(target)) {
                out.add(e);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    //  Helpers (private)
    // ------------------------------------------------------------------

    private String deriveDay(String slot) {
        if (slot == null) return "";
        String s = slot.trim().toLowerCase();
        if (s.startsWith("mon")) return "Monday";
        if (s.startsWith("tue")) return "Tuesday";
        if (s.startsWith("wed")) return "Wednesday";
        if (s.startsWith("thu")) return "Thursday";
        if (s.startsWith("fri")) return "Friday";
        if (s.startsWith("sat")) return "Saturday";
        if (s.startsWith("sun")) return "Sunday";
        return "";
    }

    private String formatTimeRange(Time start, Time end, String slot) {
        if (start != null && end != null) {
            return start.toString().substring(0, 5) + " – " + end.toString().substring(0, 5);
        }
        return slot == null ? "" : slot;
    }

    /** Formats a java.sql.Time as HH:mm (e.g. "08:30"). */
    private String formatHHmm(Time t) {
        if (t == null) return "00:00";
        String s = t.toString(); // "HH:mm:ss"
        return s.length() >= 5 ? s.substring(0, 5) : s;
    }

    /**
     * Computes the live display status from the real clock:
     *   - CANCELLED / DELAYED are sticky (manually set — keep them)
     *   - If today == classDay AND now is between start and end → ONGOING
     *   - Otherwise → UPCOMING
     */
    private String computeLiveStatus(String dbStatus, String classDay,
                                     Time startTime, Time endTime,
                                     String todayName, java.time.LocalTime now) {
        if (dbStatus == null) dbStatus = "UPCOMING";
        String s = dbStatus.trim().toUpperCase();
        // Sticky manual overrides
        if ("CANCELLED".equals(s) || "DELAYED".equals(s)) return s;

        // Live clock check
        if (todayName != null && todayName.equalsIgnoreCase(classDay)
                && startTime != null && endTime != null) {
            java.time.LocalTime classStart = startTime.toLocalTime();
            java.time.LocalTime classEnd   = endTime.toLocalTime();
            if (!now.isBefore(classStart) && now.isBefore(classEnd)) {
                return "ONGOING";
            }
        }
        return "UPCOMING";
    }
}
