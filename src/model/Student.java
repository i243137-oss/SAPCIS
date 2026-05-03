package model;

import java.util.ArrayList;
import java.util.List;

/**
 * // GRASP Pattern: Information Expert (UC-04)
 * Student entity that inherits from User.
 */
public class Student extends User {
    private String studentId;
    private List<String> enrolledCourses;
    private String major;

    public Student() {
        super();
        this.enrolledCourses = new ArrayList<>();
    }

    public Student(String uid, String name, String email, String role, String password, List<String> notificationList, int level,
                   String studentId, List<String> enrolledCourses, String major) {
        super(uid, name, email, role, password, notificationList, level);
        this.studentId = studentId;
        this.enrolledCourses = enrolledCourses != null ? enrolledCourses : new ArrayList<>();
        this.major = major;
    }

    public List<String> filterCourses() {
        System.out.println("Filtering courses for student: " + this.name);
        return this.enrolledCourses;
    }

    public List<ClassSession> filterSessions(String status) {
        System.out.println("Filtering sessions with status " + status + " for Student: " + this.name);
        return new ArrayList<>();
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public List<String> getEnrolledCourses() { return enrolledCourses; }
    public void setEnrolledCourses(List<String> enrolledCourses) { this.enrolledCourses = enrolledCourses; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
}
