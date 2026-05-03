package dao;

import db.DBConnection;
import model.SubstituteAssignment;
import java.sql.*;

public class SubstituteAssignmentDAO {

    public void create(SubstituteAssignment sa) throws SQLException {
        String query = "INSERT INTO substitute_assignments (assignmentId, originalTeacherId, substituteTeacherId, sessionId, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, sa.getAssignmentId());
            stmt.setString(2, sa.getOriginalTeacherId());
            stmt.setString(3, sa.getSubstituteTeacherId());
            stmt.setString(4, sa.getSessionId());
            stmt.setString(5, sa.getStatus());
            stmt.executeUpdate();
        }
    }

    public void updateStatus(String assignmentId, String newStatus) throws SQLException {
        String query = "UPDATE substitute_assignments SET status = ? WHERE assignmentId = ?";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, assignmentId);
            stmt.executeUpdate();
        }
    }

    public void revertToUnassigned(String sessionId) throws SQLException {
        String query1 = "UPDATE class_sessions SET status = 'UNASSIGNED' WHERE sessionId = ?";
        String query2 = "UPDATE substitute_assignments SET status = 'UNASSIGNED' WHERE sessionId = ?";
        
        try (Connection conn = DBConnection.getInstance()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt1 = conn.prepareStatement(query1);
                 PreparedStatement stmt2 = conn.prepareStatement(query2)) {
                stmt1.setString(1, sessionId);
                stmt1.executeUpdate();
                
                stmt2.setString(1, sessionId);
                stmt2.executeUpdate();
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
