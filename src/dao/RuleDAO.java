package dao;

import db.DBConnection;
import model.Rule;
import java.sql.*;

public class RuleDAO {

    public void create(Rule rule) throws SQLException {
        String query = "INSERT INTO rules (ruleId, ruleName, description, type, value, isActive) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, rule.getRuleId());
            stmt.setString(2, rule.getRuleName());
            stmt.setString(3, rule.getDescription());
            stmt.setString(4, rule.getType());
            stmt.setString(5, rule.getValue());
            stmt.setBoolean(6, rule.isActive());
            stmt.executeUpdate();
        }
    }

    public void applyRule(String ruleId) throws SQLException {
        String query = "UPDATE rules SET isActive = 1 WHERE ruleId = ?";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, ruleId);
            stmt.executeUpdate();
        }
    }
}
