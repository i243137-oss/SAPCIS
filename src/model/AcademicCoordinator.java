package model;

import java.util.List;

public class AcademicCoordinator extends User {
    private String coordinatorId;
    private String departmentManaged;
    private String approveRequestMessage;

    public AcademicCoordinator() {
        super();
    }

    public AcademicCoordinator(String uid, String name, String email, String role, String password, List<String> notificationList, int level,
                               String coordinatorId, String departmentManaged, String approveRequestMessage) {
        super(uid, name, email, role, password, notificationList, level);
        this.coordinatorId = coordinatorId;
        this.departmentManaged = departmentManaged;
        this.approveRequestMessage = approveRequestMessage;
    }

    public void approveRequest(String requestId) {
        System.out.println("Request " + requestId + " approved by Coordinator: " + this.name);
    }

    public void generateImportRequest() {
        System.out.println("Generating import request by Coordinator: " + this.name);
    }

    public void verifyTeacher(String teacherId) {
        System.out.println("Verifying teacher " + teacherId + " by Coordinator: " + this.name);
    }

    public String getCoordinatorId() { return coordinatorId; }
    public void setCoordinatorId(String coordinatorId) { this.coordinatorId = coordinatorId; }

    public String getDepartmentManaged() { return departmentManaged; }
    public void setDepartmentManaged(String departmentManaged) { this.departmentManaged = departmentManaged; }

    public String getApproveRequestMessage() { return approveRequestMessage; }
    public void setApproveRequestMessage(String approveRequestMessage) { this.approveRequestMessage = approveRequestMessage; }
}
