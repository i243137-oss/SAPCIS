package model;

import java.time.LocalTime;
import java.util.UUID;

/**
 * // GRASP Pattern: Information Expert
 * Owns all session state. Used in UC-01, 02, 04, 08, 09, 11
 */
public class ClassSession {
    private String sessionId;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String timetableSlot;
    private String roomNumber;

    public ClassSession() {}

    public ClassSession(String sessionId, LocalTime startTime, LocalTime endTime, String status, String timetableSlot, String roomNumber) {
        this.sessionId = sessionId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.timetableSlot = timetableSlot;
        this.roomNumber = roomNumber;
    }

    public String getSessionDetails() {
        return "Session: " + sessionId + ", Slot: " + timetableSlot + ", Room: " + roomNumber + ", Status: " + status;
    }

    public String getLiveStatus() {
        return this.status;
    }

    public boolean checkAdminOverrides() {
        System.out.println("Checking admin overrides for session: " + sessionId);
        return false;
    }

    public void updateRoom(String newRoomId) {
        this.roomNumber = newRoomId;
        System.out.println("Room updated to " + newRoomId + " for session " + sessionId);
    }

    public void setSessionStatus(String newStatus, String emergencyReason) {
        this.status = newStatus;
        System.out.println("Status of session " + sessionId + " set to " + newStatus + " due to: " + emergencyReason);
    }

    public void setSubstituteTeacher(String substituteId) {
        System.out.println("Substitute teacher " + substituteId + " assigned to session " + sessionId);
    }

    public void filterSessions() {
        System.out.println("Filtering sessions inside ClassSession for " + sessionId);
    }

    /**
     * // GRASP Pattern: Creator
     * ClassSession creates SubstituteAssignment because it holds original and substitute IDs.
     */
    public SubstituteAssignment create(String originalTeacherId, String substituteId) {
        System.out.println("Creating SubstituteAssignment via ClassSession Creator pattern.");
        String newAssignmentId = UUID.randomUUID().toString();
        return new SubstituteAssignment(newAssignmentId, originalTeacherId, substituteId, this.sessionId, "PENDING_ACCEPTANCE");
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimetableSlot() { return timetableSlot; }
    public void setTimetableSlot(String timetableSlot) { this.timetableSlot = timetableSlot; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
}
