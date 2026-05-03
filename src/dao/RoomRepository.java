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
 *  RoomRepository  — GRASP: Information Expert
 * -----------------------------------------------------------------------------
 *  Owns all queries about classrooms and their availability.
 *  Called by {@link controller.RoomSwapController} to verify that at least one
 *  room with the required capacity is free during a given time block.
 *
 *  Availability logic uses the continuous-block NOT-IN overlap query:
 *      A room is BUSY if any existing assignment satisfies:
 *          startTime < requestedEnd  AND  endTime > requestedStart
 *  A room is FREE if it does NOT appear in that sub-query.
 * =============================================================================
 */
public class RoomRepository {

    /**
     * Returns all classrooms that:
     *   1. Have capacity >= {@code minCapacity}
     *   2. Are completely free during [startTime, endTime) on {@code dayOfWeek}
     *
     * @param minCapacity  minimum seating capacity required
     * @param dayOfWeek    e.g. "Monday"
     * @param startTime    HH:mm string
     * @param endTime      HH:mm string
     * @return list of maps with keys: roomId, roomName, capacity
     * @throws SQLException on DB error
     */
    public List<Map<String, String>> checkRoomAvailability(
            int minCapacity, String dayOfWeek, String startTime, String endTime)
            throws SQLException {

        String sql =
            "SELECT cr.roomId, cr.roomName, cr.capacity " +
            "FROM classrooms cr " +
            "WHERE cr.capacity >= ? " +
            "  AND cr.roomId NOT IN ( " +
            "      SELECT ta.roomId " +
            "      FROM teacher_assignments ta " +
            "      WHERE ta.dayOfWeek = ? " +
            "        AND ta.startTime < ? " +
            "        AND ta.endTime   > ? " +
            "  ) " +
            "ORDER BY cr.capacity ASC, cr.roomId ASC";

        List<Map<String, String>> results = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, minCapacity);
            s.setString(2, dayOfWeek);
            s.setString(3, endTime);    // ta.startTime < requestedEnd
            s.setString(4, startTime);  // ta.endTime   > requestedStart
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("roomId",   rs.getString("roomId"));
                    row.put("roomName", rs.getString("roomName"));
                    row.put("capacity", rs.getString("capacity"));
                    results.add(row);
                }
            }
        }
        return results;
    }

    /**
     * Convenience: returns true if at least one room satisfies the criteria.
     */
    public boolean hasAvailableRoom(
            int minCapacity, String dayOfWeek, String startTime, String endTime)
            throws SQLException {
        return !checkRoomAvailability(minCapacity, dayOfWeek, startTime, endTime).isEmpty();
    }
}
