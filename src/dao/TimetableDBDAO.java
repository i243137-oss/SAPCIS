package dao;

import db.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TimetableDBDAO {

    public void updateSessionStatus(String classId, String status, String dataValue) throws SQLException {
        String query = "UPDATE timetable_db SET dataValue = ? WHERE sessionId = ? AND dataType = ?";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, dataValue);
            stmt.setString(2, classId);
            stmt.setString(3, status);
            stmt.executeUpdate();
        }
    }

    public List<String> fetchEnrolledStudents(String classId) throws SQLException {
        List<String> students = new ArrayList<>();
        String query = "SELECT dataValue FROM timetable_db WHERE sessionId = ? AND dataType = 'STUDENT_ENROLLMENT'";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(rs.getString("dataValue"));
                }
            }
        }
        return students;
    }
}
