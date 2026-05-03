package dao;

import db.DBConnection;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SessionRepositoryDAO {

    public Object fetchHistoricalData(String reportType, String dateRange) throws SQLException {
        System.out.println("Fetching historical data for " + reportType + " within " + dateRange);
        return new Object(); 
    }

    public Map<String, Integer> aggregateSessionStates() throws SQLException {
        Map<String, Integer> states = new HashMap<>();
        String query = "SELECT status, COUNT(*) as count FROM class_sessions GROUP BY status";
        try (Connection conn = DBConnection.getInstance();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                states.put(rs.getString("status"), rs.getInt("count"));
            }
        }
        return states;
    }
}
