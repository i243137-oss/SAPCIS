package controller;

import dao.StudentDAO;
import db.DBConnection;
import model.ClassSession;
import model.ScheduleEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

/**
 * // GRASP Pattern: Controller + Information Expert (Student, DailySchedule, ClassSession)
 * Used in UC-04.
 *
 * Phase 5 additions: rich schedule queries that join across
 * class_sessions / courses / sections / users (teacher) / classrooms /
 * timetable_db so the Student Dashboard can populate its "Today's Live
 * Status" sub-tabs and 5 day-of-week TableViews directly.
 */
public class DashboardController {

    private final StudentDAO studentDAO;

    // Status constants used throughout UC-04.
    public static final String ST_ONGOING   = "ONGOING";
    public static final String ST_UPCOMING  = "UPCOMING";
    public static final String ST_DELAYED   = "DELAYED";
    public static final String ST_CANCELLED = "CANCELLED";

    public DashboardController() {
        this.studentDAO = new StudentDAO();
    }

    /**
     * Produces a SQL expression that normalises any supported spelling of a
     * department (short code, full name, any casing, trimmed) into its
     * canonical 2-character code: CS / SE / IT / EE / AI / DS / CE / UNK.
     *
     * Used by the dashboard JOIN so that a student whose profile stores
     * {@code dept = 'CS'} and a mapping row whose {@code cta.dept =
     * 'Computer Science'} still match each other.
     */
    private static String DEPT_CODE_SQL(String column) {
        String u = "UPPER(LTRIM(RTRIM(" + column + ")))";
        return "(CASE " +
                "WHEN " + u + " IN ('CS','COMPUTER SCIENCE','COMP SCI','COMP. SCI.','COMP-SCI') THEN 'CS' " +
                "WHEN " + u + " IN ('SE','SOFTWARE ENGINEERING','SOFTWARE ENG','SOFT ENG','SW ENG') THEN 'SE' " +
                "WHEN " + u + " IN ('IT','INFORMATION TECHNOLOGY','INFO TECH','INFO. TECH.') THEN 'IT' " +
                "WHEN " + u + " IN ('EE','ELECTRICAL ENGINEERING','ELEC ENG') THEN 'EE' " +
                "WHEN " + u + " IN ('CE','COMPUTER ENGINEERING','COMP ENG') THEN 'CE' " +
                "WHEN " + u + " IN ('AI','ARTIFICIAL INTELLIGENCE') THEN 'AI' " +
                "WHEN " + u + " IN ('DS','DATA SCIENCE') THEN 'DS' " +
                "ELSE " + u +
                " END)";
    }

    public void accessDashboard() {
        System.out.println("DashboardController: Dashboard accessed.");
    }

    // ------------------------------------------------------------------
    //  Legacy API (kept for backward compatibility)
    // ------------------------------------------------------------------

    public List<ClassSession> getEnrolledSections(String studentId) {
        try {
            return studentDAO.getEnrolledSections(studentId);
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public String getLiveStatus(String sessionId) {
        ClassSession s = new ClassSession();
        s.setSessionId(sessionId);
        s.checkAdminOverrides(); // UC-11 integration point
        return s.getLiveStatus();
    }

    public void selectTab(String studentId, String tabName) {
        try {
            switch (tabName) {
                case "Ongoing":   studentDAO.filterSessions(studentId, ST_ONGOING);   break;
                case "Delayed":   studentDAO.filterSessions(studentId, ST_DELAYED);   break;
                case "Cancelled": studentDAO.filterSessions(studentId, ST_CANCELLED); break;
                case "Upcoming":  studentDAO.filterSessions(studentId, ST_UPCOMING);  break;
            }
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    public void showCurrentClassDetails(String subject, String room, String time) {
        System.out.println("Showing details: " + subject + " at " + time + " in " + room);
    }

    // ------------------------------------------------------------------
    //  NEW: Rich, denormalised queries for the Student Dashboard
    // ------------------------------------------------------------------

    /**
     * Full weekly timetable for the given student, flattened into
     * {@link ScheduleEntry} rows (subject, teacher name, room, time, day,
     * status). Driven by the timetable_db enrollment marker.
     *
     * IMPORTANT robustness notes:
     *   • timetableSlot / startTime / endTime / roomNumber / teacherId may be
     *     stored on {@code class_sessions} OR on {@code sections} depending
     *     on how the data was seeded. We COALESCE across both so no column
     *     comes back null.
     *   • If {@code timetable_db} has no STUDENT_ENROLLMENT rows for this
     *     student yet (seed-data gap), we fall back to {@code getFullScheduleBySection}
     *     which returns every session that belongs to the student's section.
     */
    /**
     * Full weekly timetable for the given student.
     *
     * SOURCE OF TRUTH: teacher_assignments table (same as TeacherController).
     * This ensures admin schedule changes are immediately visible to students.
     *
     * The student's section determines which teacher_assignments rows to show.
     * Status is fetched from class_sessions as a correlated subquery (best-effort).
     * Teacher name comes from course_teacher_assignments → users.
     */
    public List<ScheduleEntry> getFullSchedule(String studentId) {
        List<ScheduleEntry> rows = new ArrayList<>();
        if (studentId == null) return rows;

        // Read from teacher_assignments — always up-to-date after admin changes.
        // Match by student's section name (sectionName column in teacher_assignments).
        // Status comes from class_sessions via correlated subquery (matches by courseId+startTime only,
        //   since roomId may differ after a room swap).
        // Teacher name: use ta.teacherUid directly (avoids duplicate rows from course_teacher_assignments).
        final String sql =
                "SELECT ta.assignmentId, " +
                "       ta.dayOfWeek, " +
                "       ta.startTime, " +
                "       ta.endTime, " +
                "       ta.courseCode, " +
                "       ta.sectionName, " +
                "       COALESCE(c.courseName, ta.courseCode)  AS subject, " +
                "       COALESCE(cr.roomName,  ta.roomId)      AS room, " +
                "       COALESCE(uT.name, '-')                 AS teacherName, " +
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
                "FROM students st " +
                "JOIN teacher_assignments ta " +
                "   ON UPPER(LTRIM(RTRIM(ta.sectionName))) = UPPER(LTRIM(RTRIM(st.section))) " +
                "LEFT JOIN courses    c   ON c.courseCode  = ta.courseCode " +
                "LEFT JOIN classrooms cr  ON cr.roomId     = ta.roomId " +
                "LEFT JOIN users      uT  ON uT.uid        = ta.teacherUid " +
                "WHERE st.uid = ? " +
                "ORDER BY ta.dayOfWeek, ta.startTime";

        // Current time for live status computation
        java.time.LocalTime now = java.time.LocalTime.now();
        String todayName = java.time.LocalDate.now()
                .getDayOfWeek()
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(buildEntryFromTA(rs, todayName, now));
                }
            }
        } catch (SQLException e) {
            System.err.println("getFullSchedule failed: " + e.getMessage());
        }

        System.out.println("[Dashboard] getFullSchedule(uid=" + studentId + ") -> "
                + rows.size() + " row(s) via teacher_assignments.");
        return rows;
    }

    /**
     * Fallback: kept for backward compatibility but now also uses teacher_assignments.
     * Called when getFullSchedule returns empty (should not happen with correct seed data).
     */
    public List<ScheduleEntry> getFullScheduleBySection(String studentId) {
        // Delegate to the main method — both now use teacher_assignments
        return getFullSchedule(studentId);
    }
    /**
     * Shared ResultSet -> ScheduleEntry mapper for teacher_assignments-based queries.
     */
    private ScheduleEntry buildEntryFromTA(ResultSet rs,
                                           String todayName,
                                           java.time.LocalTime now) throws SQLException {
        String subject     = rs.getString("subject");
        String teacher     = rs.getString("teacherName");
        String room        = rs.getString("room");
        String day         = rs.getString("dayOfWeek");
        String dbStatus    = rs.getString("status");
        String assignId    = String.valueOf(rs.getInt("assignmentId"));
        Time   startTime   = rs.getTime("startTime");
        Time   endTime     = rs.getTime("endTime");

        String slot = day + " " + formatHHmm(startTime);
        String time = formatTimeRange(startTime, endTime, slot);

        // Compute live status from real clock
        String st = computeLiveStatus(dbStatus, day, startTime, endTime, todayName, now);

        return new ScheduleEntry(assignId, subject, teacher, room, time, day, st);
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
        if (dbStatus == null) dbStatus = ST_UPCOMING;
        String s = dbStatus.trim().toUpperCase();
        if (ST_CANCELLED.equals(s) || ST_DELAYED.equals(s)) return s;

        if (todayName != null && todayName.equalsIgnoreCase(classDay)
                && startTime != null && endTime != null) {
            java.time.LocalTime classStart = startTime.toLocalTime();
            java.time.LocalTime classEnd   = endTime.toLocalTime();
            if (!now.isBefore(classStart) && now.isBefore(classEnd)) {
                return ST_ONGOING;
            }
        }
        return ST_UPCOMING;
    }

    /**
     * Shared ResultSet -> ScheduleEntry mapper used by both the
     * enrollment-driven query and the section-fallback query.
     */
    private ScheduleEntry buildEntry(ResultSet rs) throws SQLException {
        String subject   = rs.getString("subject");
        String teacher   = rs.getString("teacherName");
        String room      = rs.getString("room");
        String slot      = rs.getString("slot");
        String status    = rs.getString("status");
        String sessionId = rs.getString("sessionId");
        Time   startTime = rs.getTime("startTime");
        Time   endTime   = rs.getTime("endTime");

        String time = formatTimeRange(startTime, endTime, slot);
        String day  = deriveDay(slot);

        // Default status when the DB leaves it null.
        String st = status == null || status.trim().isEmpty()
                ? ST_UPCOMING
                : status.trim().toUpperCase();

        return new ScheduleEntry(sessionId, subject, teacher, room, time, day, st);
    }

    /**
     * Returns the weekly schedule filtered to a specific day
     * (e.g. "Monday", "Tuesday", ...).
     */
    public List<ScheduleEntry> getScheduleForDay(String studentId, String day) {
        List<ScheduleEntry> all = getFullSchedule(studentId);
        List<ScheduleEntry> dayRows = new ArrayList<>();
        if (day == null) return dayRows;

        String target = day.trim().toLowerCase();
        for (ScheduleEntry e : all) {
            if (e.getDay() != null && e.getDay().trim().toLowerCase().startsWith(target.substring(0, 3))) {
                dayRows.add(e);
            }
        }
        return dayRows;
    }

    /**
     * Returns only TODAY's sessions whose status matches the requested one
     * ("ONGOING", "UPCOMING", "DELAYED", "CANCELLED").
     */
    public List<ScheduleEntry> getTodaysSessionsByStatus(String studentId, String status) {
        List<ScheduleEntry> rows = new ArrayList<>();
        if (status == null) return rows;

        String today  = currentDayOfWeek();
        String target = status.trim().toUpperCase();

        for (ScheduleEntry e : getScheduleForDay(studentId, today)) {
            if (target.equalsIgnoreCase(e.getStatus())) {
                rows.add(e);
            }
        }
        return rows;
    }

    /**
     * Returns every "critical" session scheduled for TODAY — i.e. those whose
     * status is CANCELLED or DELAYED. The Student Dashboard renders these as
     * a red/orange banner at the top of the screen.
     */
    public List<ScheduleEntry> getCriticalAlertsForToday(String studentId) {
        List<ScheduleEntry> alerts = new ArrayList<>();
        String today = currentDayOfWeek();

        for (ScheduleEntry e : getScheduleForDay(studentId, today)) {
            String st = e.getStatus() == null ? "" : e.getStatus().toUpperCase();
            if (ST_CANCELLED.equals(st) || ST_DELAYED.equals(st)) {
                alerts.add(e);
            }
        }
        return alerts;
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    /**
     * Maps {@code java.time.DayOfWeek} to our dashboard tab names.
     */
    private String currentDayOfWeek() {
        switch (java.time.LocalDate.now().getDayOfWeek()) {
            case MONDAY:    return "Monday";
            case TUESDAY:   return "Tuesday";
            case WEDNESDAY: return "Wednesday";
            case THURSDAY:  return "Thursday";
            case FRIDAY:    return "Friday";
            case SATURDAY:  return "Saturday";
            case SUNDAY:    return "Sunday";
            default:        return "Monday";
        }
    }

    /**
     * Best-effort extraction of the day portion from a timetableSlot string.
     * Accepts values like "Mon 09:00", "Monday 09:00-10:30", "TUE/10:00".
     */
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
}
