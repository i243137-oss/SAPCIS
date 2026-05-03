package dao;

import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 *  SessionRepository (Information Expert — GRASP)
 * -----------------------------------------------------------------------------
 *  Responsible for fetching raw timetable data from the database via JDBC.
 *  Used by ReportController → ReportGenerator to build the UC-12 reports.
 *
 *  Each method returns a List of Maps, where each Map represents one row
 *  with column-name → value entries. This keeps the DAO generic.
 * =============================================================================
 */
public class SessionRepository {

    /**
     * Teacher Weekly Timetable — fetches all assignments for a specific teacher.
     *
     * SQL joins teacher_assignments → courses (for course name)
     *                               → classrooms (for room name)
     * Ordered by dayOfWeek (Mon→Sat) and startTime.
     *
     * @param teacherUid  The teacher's UID (e.g. "T-CS-001")
     * @return List of row-maps with keys: dayOfWeek, startTime, endTime,
     *         courseCode, courseName, sectionName, roomId, roomName, deptId, batchId
     */
    public List<Map<String, String>> getTeacherWeeklyTimetable(String teacherUid) throws SQLException {
        String sql =
            "SELECT ta.dayOfWeek, ta.startTime, ta.endTime, "
          + "       ta.courseCode, c.courseName, "
          + "       ta.sectionName, ta.deptId, ta.batchId, "
          + "       ta.roomId, cr.roomName "
          + "FROM teacher_assignments ta "
          + "JOIN courses c ON ta.courseCode = c.courseCode "
          + "JOIN classrooms cr ON ta.roomId = cr.roomId "
          + "WHERE ta.teacherUid = ? "
          + "ORDER BY "
          + "  CASE ta.dayOfWeek "
          + "    WHEN 'Monday'    THEN 1 "
          + "    WHEN 'Tuesday'   THEN 2 "
          + "    WHEN 'Wednesday' THEN 3 "
          + "    WHEN 'Thursday'  THEN 4 "
          + "    WHEN 'Friday'    THEN 5 "
          + "    WHEN 'Saturday'  THEN 6 "
          + "    ELSE 7 END, "
          + "  ta.startTime";

        return executeQuery(sql, teacherUid);
    }

    /**
     * Room Weekly Timetable — fetches all assignments for a specific room.
     *
     * SQL joins teacher_assignments → courses (course name)
     *                               → users (teacher name)
     * Ordered by dayOfWeek (Mon→Sat) and startTime.
     *
     * @param roomId  The room ID (e.g. "R-101")
     * @return List of row-maps with keys: dayOfWeek, startTime, endTime,
     *         courseCode, courseName, teacherUid, teacherName, sectionName, deptId, batchId
     */
    public List<Map<String, String>> getRoomWeeklyTimetable(String roomId) throws SQLException {
        String sql =
            "SELECT ta.dayOfWeek, ta.startTime, ta.endTime, "
          + "       ta.courseCode, c.courseName, "
          + "       ta.teacherUid, u.name AS teacherName, "
          + "       ta.sectionName, ta.deptId, ta.batchId "
          + "FROM teacher_assignments ta "
          + "JOIN courses c ON ta.courseCode = c.courseCode "
          + "JOIN users u ON ta.teacherUid = u.uid "
          + "WHERE ta.roomId = ? "
          + "ORDER BY "
          + "  CASE ta.dayOfWeek "
          + "    WHEN 'Monday'    THEN 1 "
          + "    WHEN 'Tuesday'   THEN 2 "
          + "    WHEN 'Wednesday' THEN 3 "
          + "    WHEN 'Thursday'  THEN 4 "
          + "    WHEN 'Friday'    THEN 5 "
          + "    WHEN 'Saturday'  THEN 6 "
          + "    ELSE 7 END, "
          + "  ta.startTime";

        return executeQuery(sql, roomId);
    }

    /**
     * Classroom Utilization — counts how many assignment slots each room has.
     * Returns roomId, roomName, totalSlots (number of weekly sessions).
     */
    public List<Map<String, String>> getClassroomUtilization() throws SQLException {
        String sql =
            "SELECT cr.roomId, cr.roomName, cr.capacity, "
          + "       COUNT(ta.assignmentId) AS totalSlots "
          + "FROM classrooms cr "
          + "LEFT JOIN teacher_assignments ta ON cr.roomId = ta.roomId "
          + "GROUP BY cr.roomId, cr.roomName, cr.capacity "
          + "ORDER BY totalSlots DESC";

        return executeQuery(sql, null);
    }

    /**
     * Faculty Load Summary — counts sessions and distinct courses per teacher.
     */
    public List<Map<String, String>> getFacultyLoadSummary() throws SQLException {
        String sql =
            "SELECT u.uid, u.name, "
          + "       COUNT(ta.assignmentId) AS totalSessions, "
          + "       COUNT(DISTINCT ta.courseCode) AS distinctCourses "
          + "FROM users u "
          + "LEFT JOIN teacher_assignments ta ON u.uid = ta.teacherUid "
          + "WHERE u.role = 'Teacher' "
          + "GROUP BY u.uid, u.name "
          + "ORDER BY totalSessions DESC";

        return executeQuery(sql, null);
    }

    // ==================================================================
    //  Generic query executor
    // ==================================================================

    /**
     * Executes a parameterized SELECT and returns results as a list of maps.
     * @param sql    The SQL query (may contain one ? placeholder)
     * @param param  The single string parameter to bind (null if no params)
     */
    private List<Map<String, String>> executeQuery(String sql, String param) throws SQLException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            if (param != null) s.setString(1, param);
            try (ResultSet rs = s.executeQuery()) {
                int colCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        String colName = rs.getMetaData().getColumnLabel(i);
                        String val = rs.getString(i);
                        if (val != null) {
                            val = val.trim();
                            // Truncate time values to HH:mm (strip seconds/nanoseconds)
                            if (colName.toLowerCase().contains("time") && val.length() > 5
                                    && val.charAt(2) == ':') {
                                val = val.substring(0, 5);
                            }
                        } else {
                            val = "";
                        }
                        row.put(colName, val);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
