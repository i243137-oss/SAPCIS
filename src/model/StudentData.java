package model;

/**
 * Data Transfer Object for Student Registration
 */
public class StudentData {
    private String uid;
    private String name;
    private String email;
    private String password;
    private String rollNo;
    private String batch;
    private String dept;
    private String section;

    public StudentData(String uid, String name, String email, String password, String rollNo, String batch, String dept, String section) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.password = password;
        this.rollNo = rollNo;
        this.batch = batch;
        this.dept = dept;
        this.section = section;
    }

    // Getters
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRollNo() { return rollNo; }
    public String getBatch() { return batch; }
    public String getDept() { return dept; }
    public String getSection() { return section; }
}
