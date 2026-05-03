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
 *  OverrideController (UC-11: Emergency Status Override & Room Swap)
 * -----------------------------------------------------------------------------
 *  Backend domain controller following GRASP patterns.
 *  Coordinates with ClassSession entity (teacher_assignments table),
 *  Teacher entity (users table), and NotificationService.
 *
 *  Called by AdminDashboardController — the UI controller NEVER runs SQL.
 * =============================================================================
 */
public class OverrideController {

    /**
     * Searches a teacher's sessions for a given day from teacher_assignments.
     * Returns rows with: assignmentId, courseCode, courseName, roomId, roomName,
     *                     startTime, endTime, sectionName, status.
     *
     * Status comes from the class_sessions table if a matching session exists,
     * otherwise defaults to "SCHEDULED".
     */
    public List<Map<String, String>> searchActiveSession(String teacherUid, String day) throws SQLException {
        List<Map<String, String>> rows = new ArrayList<>();

        // Fetch assignment details + any manually-set status from class_sessions.
        // Live ONGOING/UPCOMING is computed in Java from the real clock (see below).
        String sql =
            "SELECT ta.assignmentId, ta.courseCode, c.courseName, "
          + "       ta.roomId, cr.roomName, "
          + "       ta.startTime, ta.endTime, ta.sectionName, "
          + "       COALESCE(("
          + "           SELECT TOP 1 cs.status FROM class_sessions cs "
          + "           WHERE cs.courseId = ta.courseCode "
          + "             AND CAST(cs.startTime AS VARCHAR(5)) = CAST(ta.startTime AS VARCHAR(5))"
          + "             AND cs.sectionId IN ("
          + "                 SELECT sectionId FROM sections "
          + "                 WHERE UPPER(LTRIM(RTRIM(sectionName))) = UPPER(LTRIM(RTRIM(ta.sectionName)))"
          + "             )"
          + "       ), 'UPCOMING') AS dbStatus "
          + "FROM teacher_assignments ta "
          + "JOIN courses c ON ta.courseCode = c.courseCode "
          + "JOIN classrooms cr ON ta.roomId = cr.roomId "
          + "WHERE ta.teacherUid = ? AND ta.dayOfWeek = ? "
          + "ORDER BY ta.startTime";

        // Current time for live status computation
        java.time.LocalTime now = java.time.LocalTime.now();
        // Is today the same day as the searched day?
        String todayName = java.time.LocalDate.now()
                .getDayOfWeek()
                .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
        boolean isToday = todayName.equalsIgnoreCase(day);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, teacherUid);
            s.setString(2, day);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("assignmentId", String.valueOf(rs.getInt("assignmentId")));
                    row.put("courseCode",   rs.getString("courseCode"));
                    row.put("courseName",   rs.getString("courseName") == null ? "" : rs.getString("courseName").trim());
                    row.put("roomId",       rs.getString("roomId") == null ? "" : rs.getString("roomId").trim());
                    row.put("roomName",     rs.getString("roomName") == null ? "" : rs.getString("roomName").trim());
                    row.put("sectionName",  rs.getString("sectionName") == null ? "" : rs.getString("sectionName").trim());

                    // Trim times to HH:mm
                    String startStr = rs.getString("startTime");
                    String endStr   = rs.getString("endTime");
                    if (startStr != null && startStr.length() > 5) startStr = startStr.substring(0, 5);
                    if (endStr   != null && endStr.length()   > 5) endStr   = endStr.substring(0, 5);
                    row.put("startTime", startStr == null ? "" : startStr.trim());
                    row.put("endTime",   endStr   == null ? "" : endStr.trim());

                    // Compute live status:
                    // - CANCELLED / DELAYED are sticky (manually set by admin/teacher)
                    // - If today matches the day:
                    //     now >= start AND now < end  → ONGOING
                    //     now >= end                  → UPCOMING (class already finished today)
                    //     now < start                 → UPCOMING
                    // - If not today → UPCOMING
                    String dbStatus = rs.getString("dbStatus");
                    if (dbStatus == null) dbStatus = "UPCOMING";
                    dbStatus = dbStatus.trim().toUpperCase();

                    String liveStatus;
                    if ("CANCELLED".equals(dbStatus) || "DELAYED".equals(dbStatus)) {
                        liveStatus = dbStatus; // sticky manual override
                    } else if (isToday && startStr != null && endStr != null && !startStr.isEmpty() && !endStr.isEmpty()) {
                        try {
                            java.time.LocalTime classStart = java.time.LocalTime.parse(startStr);
                            java.time.LocalTime classEnd   = java.time.LocalTime.parse(endStr);
                            if (!now.isBefore(classStart) && now.isBefore(classEnd)) {
                                liveStatus = "ONGOING";
                            } else {
                                liveStatus = "UPCOMING";
                            }
                        } catch (Exception ex) {
                            liveStatus = "UPCOMING";
                        }
                    } else {
                        liveStatus = "UPCOMING";
                    }
                    row.put("status", liveStatus);
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    /**
     * Overrides the status of a session in the class_sessions table.
     *
     * If a matching class_sessions row exists (by courseId + startTime + roomNumber),
     * UPDATE its status. Otherwise INSERT a new row.
     *
     * IMPORTANT: After a successful override, this method MUST call
     * NotificationService.notifyStudents() to alert enrolled students.
     * (See comment block inside.)
     *
     * @param assignmentId  The teacher_assignments.assignmentId
     * @param newStatus     CANCELLED, DELAYED, ON-TIME
     * @param reason        Admin-provided emergency reason
     */
    public void overrideStatus(int assignmentId, String newStatus, String reason) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // Step 1: Get the assignment details
            String courseCode = "", roomId = "", startTime = "", endTime = "", sectionId = "", teacherUid = "";
            try (PreparedStatement s = conn.prepareStatement(
                    "SELECT courseCode, roomId, startTime, endTime, sectionName, teacherUid "
                  + "FROM teacher_assignments WHERE assignmentId = ?")) {
                s.setInt(1, assignmentId);
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        courseCode = rs.getString("courseCode");
                        roomId = rs.getString("roomId");
                        startTime = rs.getString("startTime");
                        endTime = rs.getString("endTime");
                        sectionId = rs.getString("sectionName");
                        teacherUid = rs.getString("teacherUid");
                    } else {
                        throw new SQLException("Assignment not found: " + assignmentId);
                    }
                }
            }

            // Step 2: Try UPDATE existing class_session, else INSERT new one
            int updated = 0;
            try (PreparedStatement u = conn.prepareStatement(
                    "UPDATE class_sessions SET status = ? "
                  + "WHERE courseId = ? AND roomNumber = ? AND startTime = ?")) {
                u.setString(1, newStatus);
                u.setString(2, courseCode);
                u.setString(3, roomId);
                u.setString(4, startTime);
                updated = u.executeUpdate();
            }

            if (updated == 0) {
                // No existing session row → INSERT a new one
                String sessionId = "EMER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO class_sessions (sessionId, startTime, endTime, status, "
                      + "timetableSlot, roomNumber, courseId, sectionId) "
                      + "VALUES (?, ?, ?, ?, ?, ?, ?, NULL)")) {
                    ins.setString(1, sessionId);
                    ins.setString(2, startTime);
                    ins.setString(3, endTime);
                    ins.setString(4, newStatus);
                    ins.setString(5, "Override");
                    ins.setString(6, roomId);
                    ins.setString(7, courseCode);
                    ins.executeUpdate();
                }
            }

            // Step 3: Log a notification for the override
            String notifId = "NOTIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String message = "EMERGENCY: " + courseCode + " on " + roomId
                           + " status changed to " + newStatus + ". Reason: " + reason;
            try (PreparedStatement n = conn.prepareStatement(
                    "INSERT INTO notifications (notificationId, message, timestamp, targetUserId, notificationType) "
                  + "VALUES (?, ?, GETDATE(), ?, 'EMERGENCY_OVERRIDE')")) {
                n.setString(1, notifId);
                n.setString(2, message);
                n.setString(3, teacherUid); // notify the teacher
                n.executeUpdate();
            }

            // =====================================================================
            // TODO: Call NotificationService.notifyStudents(courseCode, sectionId, message)
            // This Pure Fabrication service should query all students enrolled in
            // this course+section and insert a notification row for each student.
            // Example:
            //   NotificationService notifService = new NotificationService();
            //   notifService.notifyStudents(courseCode, sectionId, message);
            // =====================================================================
        }
    }

    /**
     * Changes the day and/or time of a teacher_assignment (UC-11 Emergency Override).
     * Syncs BOTH teacher_assignments AND class_sessions so that:
     *   - Teacher "My Schedule" tab reflects the new time immediately
     *   - Student timetable/dashboard reflects the new time immediately
     *   - Substitute management shows the correct updated slot
     *
     * @param assignmentId  teacher_assignments.assignmentId
     * @param newDay        New day of week (Monday, Tuesday, etc.) — null to keep current
     * @param newStartTime  New start time in HH:mm format — null to keep current
     * @param newEndTime    New end time in HH:mm format — null to keep current
     */
    public void changeSchedule(int assignmentId, String newDay, String newStartTime, String newEndTime) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // ── Step 1: Read current assignment details ──────────────────
                String courseCode = "", roomId = "", oldStartTime = "", oldEndTime = "",
                       sectionName = "", teacherUid = "", oldDay = "";
                try (PreparedStatement s = conn.prepareStatement(
                        "SELECT courseCode, roomId, startTime, endTime, sectionName, teacherUid, dayOfWeek "
                      + "FROM teacher_assignments WHERE assignmentId = ?")) {
                    s.setInt(1, assignmentId);
                    try (ResultSet rs = s.executeQuery()) {
                        if (rs.next()) {
                            courseCode  = rs.getString("courseCode");
                            roomId      = rs.getString("roomId");
                            oldStartTime = rs.getString("startTime");
                            oldEndTime   = rs.getString("endTime");
                            sectionName  = rs.getString("sectionName");
                            teacherUid   = rs.getString("teacherUid");
                            oldDay       = rs.getString("dayOfWeek");
                        } else {
                            throw new SQLException("Assignment not found: " + assignmentId);
                        }
                    }
                }
                // Trim to HH:mm
                if (oldStartTime != null && oldStartTime.length() > 5) oldStartTime = oldStartTime.substring(0, 5);
                if (oldEndTime   != null && oldEndTime.length()   > 5) oldEndTime   = oldEndTime.substring(0, 5);

                // Resolve final values (merge new with existing)
                String finalDay   = (newDay       != null && !newDay.isEmpty())       ? newDay       : oldDay;
                String finalStart = (newStartTime != null && !newStartTime.isEmpty()) ? newStartTime : oldStartTime;
                String finalEnd   = (newEndTime   != null && !newEndTime.isEmpty())   ? newEndTime   : oldEndTime;

                // ── Step 2: Validate — no section conflict at target day/time ─
                // Rule: a section cannot have two classes at the same time on the same day.
                // Check teacher_assignments for any other assignment in the same section
                // on the target day that overlaps with [finalStart, finalEnd].
                try (PreparedStatement chk = conn.prepareStatement(
                        "SELECT ta.courseCode, ta.startTime, ta.endTime " +
                        "FROM teacher_assignments ta " +
                        "WHERE ta.sectionName = ? " +
                        "  AND ta.dayOfWeek   = ? " +
                        "  AND ta.assignmentId <> ? " +
                        "  AND ta.startTime < CAST(? AS TIME) " +
                        "  AND ta.endTime   > CAST(? AS TIME)")) {
                    chk.setString(1, sectionName);
                    chk.setString(2, finalDay);
                    chk.setInt(3, assignmentId);
                    chk.setString(4, finalEnd);   // existing.start < newEnd
                    chk.setString(5, finalStart); // existing.end   > newStart
                    try (ResultSet rs = chk.executeQuery()) {
                        if (rs.next()) {
                            String conflictCourse = rs.getString("courseCode");
                            String conflictStart  = rs.getString("startTime");
                            String conflictEnd    = rs.getString("endTime");
                            if (conflictStart != null && conflictStart.length() > 5) conflictStart = conflictStart.substring(0, 5);
                            if (conflictEnd   != null && conflictEnd.length()   > 5) conflictEnd   = conflictEnd.substring(0, 5);
                            conn.rollback();
                            throw new SQLException(
                                "Section conflict: Section " + sectionName + " already has " +
                                conflictCourse + " on " + finalDay + " at " +
                                conflictStart + "–" + conflictEnd +
                                ". Please choose a different time slot.");
                        }
                    }
                }

                // Also validate teacher conflict (teacher cannot teach two classes at same time)
                try (PreparedStatement chk2 = conn.prepareStatement(
                        "SELECT ta.courseCode, ta.sectionName, ta.startTime, ta.endTime " +
                        "FROM teacher_assignments ta " +
                        "WHERE ta.teacherUid   = ? " +
                        "  AND ta.dayOfWeek    = ? " +
                        "  AND ta.assignmentId <> ? " +
                        "  AND ta.startTime < CAST(? AS TIME) " +
                        "  AND ta.endTime   > CAST(? AS TIME)")) {
                    chk2.setString(1, teacherUid);
                    chk2.setString(2, finalDay);
                    chk2.setInt(3, assignmentId);
                    chk2.setString(4, finalEnd);
                    chk2.setString(5, finalStart);
                    try (ResultSet rs = chk2.executeQuery()) {
                        if (rs.next()) {
                            String conflictCourse  = rs.getString("courseCode");
                            String conflictSection = rs.getString("sectionName");
                            String conflictStart   = rs.getString("startTime");
                            String conflictEnd     = rs.getString("endTime");
                            if (conflictStart != null && conflictStart.length() > 5) conflictStart = conflictStart.substring(0, 5);
                            if (conflictEnd   != null && conflictEnd.length()   > 5) conflictEnd   = conflictEnd.substring(0, 5);
                            conn.rollback();
                            throw new SQLException(
                                "Teacher conflict: Teacher already has " +
                                conflictCourse + " [" + conflictSection + "] on " + finalDay +
                                " at " + conflictStart + "–" + conflictEnd +
                                ". Please choose a different time slot.");
                        }
                    }
                }

                // ── Step 3: Update teacher_assignments ───────────────────────
                StringBuilder sql = new StringBuilder("UPDATE teacher_assignments SET ");
                List<String> parts = new ArrayList<>();
                parts.add("dayOfWeek = ?");
                parts.add("startTime = CAST(? AS TIME)");
                parts.add("endTime   = CAST(? AS TIME)");
                sql.append(String.join(", ", parts));
                sql.append(" WHERE assignmentId = ?");

                try (PreparedStatement s = conn.prepareStatement(sql.toString())) {
                    s.setString(1, finalDay);
                    s.setString(2, finalStart);
                    s.setString(3, finalEnd);
                    s.setInt(4, assignmentId);
                    s.executeUpdate();
                }

                // ── Step 3: Sync class_sessions ──────────────────────────────
                // Match by courseId + roomNumber + old startTime (the natural key).
                // timetableSlot stores the full day name + time, e.g. "Monday 08:30"
                // which is what the Teacher dashboard and Student dashboard display.
                String newSlot = finalDay + " " + finalStart; // e.g. "Monday 08:30"

                // Update by courseId + roomNumber + old start time
                try (PreparedStatement u = conn.prepareStatement(
                        "UPDATE class_sessions "
                      + "SET startTime     = CAST(? AS TIME), "
                      + "    endTime       = CAST(? AS TIME), "
                      + "    timetableSlot = ? "
                      + "WHERE courseId    = ? "
                      + "  AND roomNumber  = ? "
                      + "  AND CAST(startTime AS VARCHAR(5)) = ?")) {
                    u.setString(1, finalStart);
                    u.setString(2, finalEnd);
                    u.setString(3, newSlot);
                    u.setString(4, courseCode);
                    u.setString(5, roomId);
                    u.setString(6, oldStartTime);
                    u.executeUpdate(); // best-effort — may be 0 rows if no matching session
                }

                // Also update by courseId + sectionName (fallback for sessions
                // that don't have roomNumber set but do have sectionId matching sectionName)
                try (PreparedStatement u2 = conn.prepareStatement(
                        "UPDATE class_sessions "
                      + "SET startTime     = CAST(? AS TIME), "
                      + "    endTime       = CAST(? AS TIME), "
                      + "    timetableSlot = ? "
                      + "WHERE courseId    = ? "
                      + "  AND sectionId IN ("
                      + "      SELECT sectionId FROM sections WHERE sectionName = ?"
                      + "  ) "
                      + "  AND CAST(startTime AS VARCHAR(5)) = ?")) {
                    u2.setString(1, finalStart);
                    u2.setString(2, finalEnd);
                    u2.setString(3, newSlot);
                    u2.setString(4, courseCode);
                    u2.setString(5, sectionName);
                    u2.setString(6, oldStartTime);
                    u2.executeUpdate(); // best-effort
                }

                // ── Step 4: Notify teacher ───────────────────────────────────
                String notifId = "NOTIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                String message = "SCHEDULE CHANGE: " + courseCode
                        + " [" + sectionName + "] rescheduled"
                        + " → " + finalDay + " " + finalStart + "–" + finalEnd;
                try (PreparedStatement n = conn.prepareStatement(
                        "INSERT INTO notifications (notificationId, message, timestamp, targetUserId, notificationType) "
                      + "VALUES (?, ?, GETDATE(), ?, 'SCHEDULE_CHANGE')")) {
                    n.setString(1, notifId);
                    n.setString(2, message);
                    n.setString(3, teacherUid);
                    n.executeUpdate();
                }

                // ── Step 5: Notify enrolled students ────────────────────────
                // Find all students enrolled in sessions for this course+sectionName.
                // Cast TEXT dataValue to VARCHAR to allow DISTINCT (TEXT is not comparable).
                String studentNotifSql =
                    "SELECT DISTINCT CAST(t.dataValue AS VARCHAR(100)) AS studentUid "
                  + "FROM timetable_db t "
                  + "JOIN class_sessions cs ON cs.sessionId = t.sessionId "
                  + "WHERE t.dataType = 'STUDENT_ENROLLMENT' "
                  + "  AND cs.courseId = ? "
                  + "  AND cs.sectionId IN ("
                  + "      SELECT sectionId FROM sections "
                  + "      WHERE UPPER(LTRIM(RTRIM(sectionName))) = UPPER(LTRIM(RTRIM(?)))"
                  + "  )";
                try (PreparedStatement ps = conn.prepareStatement(studentNotifSql)) {
                    ps.setString(1, courseCode);
                    ps.setString(2, sectionName);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String studentUid = rs.getString("studentUid");
                            if (studentUid == null || studentUid.trim().isEmpty()) continue;
                            String sNotifId = "NOTIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                            try (PreparedStatement sn = conn.prepareStatement(
                                    "INSERT INTO notifications (notificationId, message, timestamp, targetUserId, notificationType) "
                                  + "VALUES (?, ?, GETDATE(), ?, 'SCHEDULE_CHANGE')")) {
                                sn.setString(1, sNotifId);
                                sn.setString(2, message);
                                sn.setString(3, studentUid.trim());
                                sn.executeUpdate();
                            }
                        }
                    }
                }

                conn.commit();

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Swaps the room for a teacher_assignment.
     * The caller MUST validate room availability (NOT IN overlap check)
     * BEFORE calling this method.
     *
     * @param assignmentId  teacher_assignments.assignmentId
     * @param newRoomId     The new room to assign
     */
    public void swapRoom(int assignmentId, String newRoomId) throws SQLException {
        String sql = "UPDATE teacher_assignments SET roomId = ? WHERE assignmentId = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, newRoomId);
            s.setInt(2, assignmentId);
            int rows = s.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Assignment not found: " + assignmentId);
            }
        }

        // Also update class_sessions if a matching row exists
        // (best-effort — the class_sessions table may not always have a row)
        try (Connection conn = DBConnection.getConnection()) {
            // Get the old assignment details to find the class_session
            String courseCode = "", startTime = "", oldRoomId = "";
            try (PreparedStatement s = conn.prepareStatement(
                    "SELECT courseCode, startTime, roomId FROM teacher_assignments WHERE assignmentId = ?")) {
                // Note: roomId is already updated above, so this returns newRoomId
                s.setInt(1, assignmentId);
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        courseCode = rs.getString("courseCode");
                        startTime = rs.getString("startTime");
                    }
                }
            }
            if (!courseCode.isEmpty()) {
                try (PreparedStatement u = conn.prepareStatement(
                        "UPDATE class_sessions SET roomNumber = ? WHERE courseId = ? AND startTime = ?")) {
                    u.setString(1, newRoomId);
                    u.setString(2, courseCode);
                    u.setString(3, startTime);
                    u.executeUpdate(); // best-effort
                }
            }
        }
    }
}
