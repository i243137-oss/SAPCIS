package model;

import java.util.Date;

/**
 * // GoF Pattern: Observer participant
 */
public class Notification {
    private String notificationId;
    private String message;
    private Date timestamp;

    public Notification() {}

    public Notification(String notificationId, String message, Date timestamp) {
        this.notificationId = notificationId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public void dispatch() {
        System.out.println("Dispatching notification: " + message);
        // Observer pattern executing at msg 13 in UC-01
    }

    // Getters and setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
