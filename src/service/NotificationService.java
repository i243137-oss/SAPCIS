package service;

import dao.NotificationDAO;
import model.Notification;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * // GRASP Pattern: Pure Fabrication + GoF Observer
 * Used in UC-01, UC-11
 */
public class NotificationService {
    private NotificationDAO notificationDAO;

    public NotificationService() {
        this.notificationDAO = new NotificationDAO();
    }

    public void notifyStudents(String classId, String status, String estimatedTime) {
        System.out.println("NotificationService: Notifying students for class " + classId + " regarding " + status + " " + estimatedTime);
    }

    public void pushAlerts(List<String> studentList, String message) {
        // GoF Observer execution point (msg 13)
        System.out.println("GoF Observer: Pushing alerts to " + studentList.size() + " students.");
        for (String studentId : studentList) {
            Notification n = new Notification(UUID.randomUUID().toString(), message, new Date());
            n.dispatch();
            try {
                notificationDAO.savePushRecord(n, studentId);
            } catch (Exception e) {
                System.err.println("Error saving notification: " + e.getMessage());
            }
        }
    }

    public void notifyPartiesAndLogAudit(String sessionId, String newStatus, String emergencyReason) {
        System.out.println("GRASP Pure Fabrication: notifyPartiesAndLogAudit for session " + sessionId);
        System.out.println("Audit Logged: Status " + newStatus + " due to " + emergencyReason);
        // Notifications sent
    }
}
