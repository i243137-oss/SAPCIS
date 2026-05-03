package dao;

import db.DBConnection;
import model.Notification;
import java.sql.*;

public class NotificationDAO {

    public void savePushRecord(Notification notification, String targetUserId) throws SQLException {
        String query = "INSERT INTO notifications (notificationId, message, timestamp, targetUserId, notificationType) VALUES (?, ?, ?, ?, 'PUSH')";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, notification.getNotificationId());
            stmt.setString(2, notification.getMessage());
            stmt.setTimestamp(3, new java.sql.Timestamp(notification.getTimestamp().getTime()));
            stmt.setString(4, targetUserId);
            stmt.executeUpdate();
        }
    }
}
