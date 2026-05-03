package dao;

import db.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomRepositoryDAO {

    public boolean checkRoomAvailability(int capacity) throws SQLException {
        String query = "SELECT COUNT(*) FROM classrooms WHERE capacity >= ?";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, capacity);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public List<String> queryAvailableRooms() throws SQLException {
        List<String> availableRooms = new ArrayList<>();
        String query = "SELECT roomId FROM classrooms";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                availableRooms.add(rs.getString("roomId"));
            }
        }
        return availableRooms;
    }
}
