package dao;

import db.DBConnection;
import model.ClassSession;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassSessionDAO {
    
    public ClassSession getSessionDetails(String sessionId) throws SQLException {
        String query = "SELECT * FROM class_sessions WHERE sessionId = ?";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new ClassSession(
                        rs.getString("sessionId"),
                        rs.getTime("startTime").toLocalTime(),
                        rs.getTime("endTime").toLocalTime(),
                        rs.getString("status"),
                        rs.getString("timetableSlot"),
                        rs.getString("roomNumber")
                    );
                }
            }
        }
        return null;
    }

    public void updateSessionStatus(String sessionId, String newStatus) throws SQLException {
        String query = "UPDATE class_sessions SET status = ? WHERE sessionId = ?";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, sessionId);
            stmt.executeUpdate();
        }
    }

    public void updateRoom(String sessionId, String newRoomId) throws SQLException {
        String query = "UPDATE class_sessions SET roomNumber = ? WHERE sessionId = ?";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newRoomId);
            stmt.setString(2, sessionId);
            stmt.executeUpdate();
        }
    }

    public List<String> fetchEnrolledStudents(String classId) throws SQLException {
        List<String> studentIds = new ArrayList<>();
        String query = "SELECT dataValue FROM timetable_db WHERE sessionId = ? AND dataType = 'ENROLLMENT'";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    studentIds.add(rs.getString("dataValue"));
                }
            }
        }
        return studentIds;
    }
}
