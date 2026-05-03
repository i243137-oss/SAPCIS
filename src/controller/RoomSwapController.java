package controller;

import dao.RoomRepository;
import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * =============================================================================
 *  RoomSwapController  — GRASP: Use-Case Controller + Creator
 * -----------------------------------------------------------------------------
 *  Orchestrates UC-02 "Request a Room Change" exactly as specified in the
 *  Sequence Diagram:
 *
 *  1. UI calls requestRoomSwap(sessionId, reason, capacity)
 *  2. Fetch session details (dayOfWeek, startTime, endTime) from DB.
 *  3. Delegate availability check to RoomRepository (Information Expert).
 *  4. If no room available → throw RoomUnavailableException (UI shows Alert).
 *  5. UPDATE class_sessions SET status = 'SWAP_PENDING' WHERE id = sessionId.
 *  6. INSERT into schedule_adjustment_requests (Creator pattern).
 *  7. Return the generated requestId to the UI for confirmation.
 * =============================================================================
 */
public class RoomSwapController {

    /** Thrown when no room satisfies the capacity + availability constraints. */
    public static class RoomUnavailableException extends Exception {
        public RoomUnavailableException(String message) { super(message); }
    }

    private final RoomRepository roomRepo = new RoomRepository();

    // ==================================================================
    //  Primary UC-02 method
    // ==================================================================

    /**
     * Orchestrates the full Room Swap request workflow.
     *
     * @param sessionId   the teacher_assignments.assignmentId (int as String)
     * @param reason      free-text reason entered by the teacher
     * @param capacity    minimum required capacity for the new room
     * @return the generated schedule_adjustment_requests.requestId (UUID string)
     * @throws RoomUnavailableException if no room meets capacity + availability
     * @throws SQLException             on any DB error
     */
    public String requestRoomSwap(String sessionId, String reason, int capacity)
            throws RoomUnavailableException, SQLException {

        // ── Step 1: Fetch session details ──────────────────────────────
        Map<String, String> session = fetchSessionDetails(sessionId);
        if (session == null) {
            throw new SQLException("Session not found: " + sessionId);
        }

        String dayOfWeek = session.get("dayOfWeek");
        String startTime = session.get("startTime");
        String endTime   = session.get("endTime");

        // Normalise HH:mm:ss → HH:mm for comparison
        if (startTime != null && startTime.length() > 5) startTime = startTime.substring(0, 5);
        if (endTime   != null && endTime.length()   > 5) endTime   = endTime.substring(0, 5);

        // ── Step 2: Check room availability (Information Expert) ───────
        List<Map<String, String>> available =
                roomRepo.checkRoomAvailability(capacity, dayOfWeek, startTime, endTime);

        if (available.isEmpty()) {
            throw new RoomUnavailableException(
                "No rooms available for capacity ≥ " + capacity
                + " on " + dayOfWeek + " " + startTime + "–" + endTime + ".\n"
                + "Try a lower capacity or a different time slot.");
        }

        // ── Steps 3 & 4: Update session status + Insert request ────────
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Step 3: Mark session as SWAP_PENDING
                updateSessionStatus(conn, sessionId, "SWAP_PENDING");

                // Step 4: Creator — RoomSwapController creates the request record
                String requestId = createRequest(conn, sessionId, reason, capacity, "PENDING");

                conn.commit();
                return requestId;

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ==================================================================
    //  Step 1 helper — fetch session details
    // ==================================================================

    /**
     * Fetches session details.
     *
     * Supports two ID formats:
     *   (a) Numeric string (e.g. "4") → teacher_assignments.assignmentId
     *       This is what the Teacher Dashboard passes since it reads from
     *       teacher_assignments directly.
     *   (b) Non-numeric string (e.g. "SES-MON-CS101-A") → class_sessions.sessionId
     *       Legacy path for sessions seeded via class_sessions.
     *
     * Always returns: dayOfWeek, startTime, endTime, courseCode, sectionName, roomId.
     */
    private Map<String, String> fetchSessionDetails(String sessionId) throws SQLException {
        boolean isNumeric = sessionId != null && sessionId.matches("\\d+");

        // ── Path A: numeric → look up teacher_assignments ─────────────────
        if (isNumeric) {
            String sql =
                "SELECT ta.assignmentId, ta.dayOfWeek, ta.startTime, ta.endTime, " +
                "       ta.courseCode, ta.sectionName, ta.roomId " +
                "FROM teacher_assignments ta " +
                "WHERE ta.assignmentId = ?";
            try (Connection c = DBConnection.getConnection();
                 PreparedStatement s = c.prepareStatement(sql)) {
                s.setInt(1, Integer.parseInt(sessionId));
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        Map<String, String> row = new java.util.LinkedHashMap<>();
                        row.put("sessionId",   sessionId);
                        row.put("dayOfWeek",   rs.getString("dayOfWeek"));
                        String st = rs.getString("startTime");
                        String et = rs.getString("endTime");
                        if (st != null && st.length() > 5) st = st.substring(0, 5);
                        if (et != null && et.length() > 5) et = et.substring(0, 5);
                        row.put("startTime",   st);
                        row.put("endTime",     et);
                        row.put("courseCode",  rs.getString("courseCode"));
                        row.put("sectionName", rs.getString("sectionName"));
                        row.put("roomId",      rs.getString("roomId"));
                        return row;
                    }
                }
            }
            return null; // assignmentId not found
        }

        // ── Path B: non-numeric → look up class_sessions ──────────────────
        String sql =
            "SELECT cs.sessionId, " +
            "       cs.timetableSlot AS dayOfWeek, " +
            "       cs.startTime, cs.endTime, " +
            "       cs.courseId, cs.sectionId, " +
            "       cs.roomNumber AS roomId " +
            "FROM class_sessions cs " +
            "WHERE cs.sessionId = ?";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, sessionId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> row = new java.util.LinkedHashMap<>();
                    row.put("sessionId",  rs.getString("sessionId"));
                    row.put("dayOfWeek",  deriveDay(rs.getString("dayOfWeek")));
                    String st = rs.getString("startTime");
                    String et = rs.getString("endTime");
                    if (st != null && st.length() > 5) st = st.substring(0, 5);
                    if (et != null && et.length() > 5) et = et.substring(0, 5);
                    row.put("startTime",   st);
                    row.put("endTime",     et);
                    row.put("courseCode",  rs.getString("courseId"));
                    row.put("sectionName", rs.getString("sectionId"));
                    row.put("roomId",      rs.getString("roomId"));
                    return row;
                }
            }
        }
        return null;
    }

    /** Converts a timetableSlot prefix or full day name to a canonical day string. */
    private static String deriveDay(String slot) {
        if (slot == null) return "";
        String s = slot.trim().toLowerCase();
        if (s.startsWith("mon")) return "Monday";
        if (s.startsWith("tue")) return "Tuesday";
        if (s.startsWith("wed")) return "Wednesday";
        if (s.startsWith("thu")) return "Thursday";
        if (s.startsWith("fri")) return "Friday";
        if (s.startsWith("sat")) return "Saturday";
        if (s.startsWith("sun")) return "Sunday";
        return slot; // return as-is if unrecognised
    }

    // ==================================================================
    //  Step 3 — UPDATE class_sessions status
    // ==================================================================

    /**
     * Sets the session status to 'SWAP_PENDING' in class_sessions.
     *
     * For numeric IDs (teacher_assignments.assignmentId), there may be no
     * matching class_sessions row — that is fine; the swap request is still
     * recorded in schedule_adjustment_requests and the admin can act on it.
     * For legacy string sessionIds, updates class_sessions directly.
     */
    private void updateSessionStatus(Connection conn, String sessionId, String status)
            throws SQLException {
        boolean isNumeric = sessionId != null && sessionId.matches("\\d+");
        if (isNumeric) {
            // Update class_sessions by courseId + startTime (best-effort, may be 0 rows)
            String sql =
                "UPDATE class_sessions SET status = ? " +
                "WHERE courseId = (SELECT courseCode FROM teacher_assignments WHERE assignmentId = ?) " +
                "  AND CAST(startTime AS VARCHAR(5)) = (" +
                "      SELECT CAST(startTime AS VARCHAR(5)) FROM teacher_assignments WHERE assignmentId = ?)";
            try (PreparedStatement s = conn.prepareStatement(sql)) {
                s.setString(1, status);
                s.setInt(2, Integer.parseInt(sessionId));
                s.setInt(3, Integer.parseInt(sessionId));
                s.executeUpdate(); // best-effort — 0 rows is OK
            }
        } else {
            String sql = "UPDATE class_sessions SET status = ? WHERE sessionId = ?";
            try (PreparedStatement s = conn.prepareStatement(sql)) {
                s.setString(1, status);
                s.setString(2, sessionId);
                s.executeUpdate();
            }
        }
    }

    // ==================================================================
    //  Step 4 — INSERT into schedule_adjustment_requests  (Creator)
    // ==================================================================

    /**
     * GRASP Creator: RoomSwapController creates and persists a
     * ScheduleAdjustmentRequest record.
     *
     * Columns:
     *   requestId    VARCHAR  — UUID generated here
     *   classId      VARCHAR  — FK to teacher_assignments.assignmentId
     *   reason       VARCHAR
     *   requestType  VARCHAR  — 'ROOM_SWAP'
     *   capacity     INT
     *   status       VARCHAR  — 'PENDING'
     *
     * @return the generated requestId
     */
    public String createRequest(Connection conn, String classId,
                                String reason, int capacity, String status)
            throws SQLException {

        String requestId = "SAR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String sql =
            "INSERT INTO schedule_adjustment_requests " +
            "  (requestId, classId, reason, requestType, capacity, status) " +
            "VALUES (?, ?, ?, 'ROOM_SWAP', ?, ?)";

        try (PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, requestId);
            s.setString(2, classId);
            s.setString(3, reason);
            s.setInt(4, capacity);
            s.setString(5, status);
            s.executeUpdate();
        }
        return requestId;
    }

    // ==================================================================
    //  Standalone createRequest (for external callers without a Connection)
    // ==================================================================

    /**
     * Overload that opens its own connection — useful for testing or
     * standalone inserts outside a transaction.
     */
    public String createRequest(String classId, String reason,
                                int capacity, String status) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return createRequest(conn, classId, reason, capacity, status);
        }
    }
}
