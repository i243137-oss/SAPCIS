package dao;

import db.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassroomDAO {

    public String getCurrentConstraints(String roomId) throws SQLException {
        String query = "SELECT capacity, hasProjector FROM classrooms WHERE roomId = ?";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, roomId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int capacity = rs.getInt("capacity");
                    boolean hasProjector = rs.getBoolean("hasProjector");
                    return "Capacity: " + capacity + ", Projector: " + hasProjector;
                }
            }
        }
        return "No constraints found";
    }

    public List<String> getRuleCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        String query = "SELECT DISTINCT type FROM rules";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("type"));
            }
        }
        return categories;
    }
}
