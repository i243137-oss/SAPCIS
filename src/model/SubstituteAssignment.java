package model;

import java.util.UUID;

/**
 * // GRASP Pattern: Creator
 * Created by ClassSession (UC-08).
 */
public class SubstituteAssignment {
    private String assignmentId;
    private String originalTeacherId;
    private String substituteTeacherId;
    private String sessionId;
    private String status;

    public SubstituteAssignment() {}

    public SubstituteAssignment(String assignmentId, String originalTeacherId, String substituteTeacherId, String sessionId, String status) {
        this.assignmentId = assignmentId;
        this.originalTeacherId = originalTeacherId;
        this.substituteTeacherId = substituteTeacherId;
        this.sessionId = sessionId;
        this.status = status;
    }

    public static SubstituteAssignment create(String originalTeacherId, String substituteId) {
        String newId = UUID.randomUUID().toString();
        return new SubstituteAssignment(newId, originalTeacherId, substituteId, "unknown_session", "PENDING_ACCEPTANCE");
    }

    /**
     * // GoF Pattern: Observer
     * Fires inside notifySubstituteAndStudents to alert stakeholders
     */
    public void notifySubstituteAndStudents() {
        System.out.println("Observer Pattern: Notifying substitute " + substituteTeacherId + " and students about session " + sessionId);
    }

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }

    public String getOriginalTeacherId() { return originalTeacherId; }
    public void setOriginalTeacherId(String originalTeacherId) { this.originalTeacherId = originalTeacherId; }

    public String getSubstituteTeacherId() { return substituteTeacherId; }
    public void setSubstituteTeacherId(String substituteTeacherId) { this.substituteTeacherId = substituteTeacherId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
