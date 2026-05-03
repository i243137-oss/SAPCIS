package model;

/**
 * // GRASP Pattern: Creator — created by RoomSwapController (UC-02)
 */
public class ScheduleAdjustmentRequest {
    private String assignmentId;
    private String classId;
    private String reason;
    private String requestType;
    private int capacity;
    private String status;

    public ScheduleAdjustmentRequest() {}

    public ScheduleAdjustmentRequest(String classId, String reason, int capacity, String status) {
        // Constructor used in UC-02 room swap flow
        this.assignmentId = java.util.UUID.randomUUID().toString();
        this.classId = classId;
        this.reason = reason;
        this.requestType = "ROOM_SWAP";
        this.capacity = capacity;
        this.status = status;
    }

    public ScheduleAdjustmentRequest(String assignmentId, String classId, String reason, String requestType, int capacity, String status) {
        this.assignmentId = assignmentId;
        this.classId = classId;
        this.reason = reason;
        this.requestType = requestType;
        this.capacity = capacity;
        this.status = status;
    }

    public void logToAdminDashboard() {
        System.out.println("Logging Schedule Adjustment Request to Admin Dashboard. Request ID: " + assignmentId);
    }

    // Getters and setters
    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
