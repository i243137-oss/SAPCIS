package dao;

import db.DBConnection;
import model.SubstituteAssignment;
import java.sql.*;

public class SubstituteAssignmentDAO {

    public void create(SubstituteAssignment sa) throws SQLException {
        String query = "INSERT INTO substitute_assignments (substituteId, assignmentId, originalTeacherUid, substituteTeacherUid, status, reason, createdAt) VALUES (?, ?, ?, ?, ?, ?, GETDATE())";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, sa.getAssignmentId()); // Unique ID (substituteId)
            // Parse numeric assignmentId if possible, or fallback
            int assignId = 1;
            try {
                if (sa.getSessionId() != null && sa.getSessionId().matches("\\d+")) {
                    assignId = Integer.parseInt(sa.getSessionId());
                }
            } catch (Exception ignored) {}
            stmt.setInt(2, assignId);
            stmt.setString(3, sa.getOriginalTeacherId());
            stmt.setString(4, sa.getSubstituteTeacherId());
            String status = sa.getStatus();
            if (status == null || (!status.equals("PENDING") && !status.equals("ACCEPTED") && !status.equals("REJECTED") && !status.equals("REQUESTED_BY_TEACHER"))) {
                status = "PENDING";
            }
            stmt.setString(5, status);
            stmt.setString(6, "Substitute teacher request");
            stmt.executeUpdate();
        }
    }

    public void updateStatus(String substituteId, String newStatus) throws SQLException {
        String query = "UPDATE substitute_assignments SET status = ?, respondedAt = GETDATE() WHERE substituteId = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, substituteId);
            stmt.executeUpdate();
        }
    }

    public void revertToUnassigned(String sessionId) throws SQLException {
        String query1 = "UPDATE class_sessions SET status = 'UNASSIGNED' WHERE sessionId = ?";
        String query2 = "UPDATE substitute_assignments SET status = 'REJECTED', respondedAt = GETDATE() WHERE assignmentId IN (SELECT assignmentId FROM teacher_assignments WHERE sessionId = ?)";
        
        try (Connection conn = DBConnection.getConnection()) {
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

