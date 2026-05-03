package model;

import java.util.List;

/**
 * // GRASP Pattern: Information Expert (UC-08, UC-11)
 * Teacher entity that inherits from User.
 */
public class Teacher extends User {
    private String officeHours;
    private boolean currentlyTeaching;
    private String subjectExam;

    public Teacher() {
        super();
    }

    public Teacher(String uid, String name, String email, String role, String password, List<String> notificationList, int level,
                   String officeHours, boolean currentlyTeaching, String subjectExam) {
        super(uid, name, email, role, password, notificationList, level);
        this.officeHours = officeHours;
        this.currentlyTeaching = currentlyTeaching;
        this.subjectExam = subjectExam;
    }

    public void updateSchedule() {
        System.out.println("Updating schedule for Teacher: " + this.name);
        this.currentlyTeaching = true; 
    }

    public void notifyTeacherOfRoomChange() {
        System.out.println("Notification sent to Teacher " + this.name + " regarding room change.");
    }

    public String getOfficeHours() { return officeHours; }
    public void setOfficeHours(String officeHours) { this.officeHours = officeHours; }
    
    public boolean isCurrentlyTeaching() { return currentlyTeaching; }
    public void setCurrentlyTeaching(boolean currentlyTeaching) { this.currentlyTeaching = currentlyTeaching; }
    
    public String getSubjectExam() { return subjectExam; }
    public void setSubjectExam(String subjectExam) { this.subjectExam = subjectExam; }
}
