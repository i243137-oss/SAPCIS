package dao;

import db.DBConnection;
import model.ClassSession;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    public List<ClassSession> getEnrolledSections(String studentId) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String query = "SELECT cs.* FROM class_sessions cs " +
                       "JOIN timetable_db t ON cs.sessionId = t.sessionId " +
                       "WHERE t.dataValue = ? AND t.dataType = 'STUDENT_ENROLLMENT'";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(new ClassSession(
                        rs.getString("sessionId"),
                        rs.getTime("startTime").toLocalTime(),
                        rs.getTime("endTime").toLocalTime(),
                        rs.getString("status"),
                        rs.getString("timetableSlot"),
                        rs.getString("roomNumber")
                    ));
                }
            }
        }
        return sessions;
    }

    public List<ClassSession> filterSessions(String studentId, String status) throws SQLException {
        List<ClassSession> filtered = new ArrayList<>();
        String query = "SELECT cs.* FROM class_sessions cs " +
                       "JOIN timetable_db t ON cs.sessionId = t.sessionId " +
                       "WHERE t.dataValue = ? AND t.dataType = 'STUDENT_ENROLLMENT' AND cs.status = ?";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, studentId);
            stmt.setString(2, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    filtered.add(new ClassSession(
                        rs.getString("sessionId"),
                        rs.getTime("startTime").toLocalTime(),
                        rs.getTime("endTime").toLocalTime(),
                        rs.getString("status"),
                        rs.getString("timetableSlot"),
                        rs.getString("roomNumber")
                    ));
                }
            }
        }
        return filtered;
    }
}
