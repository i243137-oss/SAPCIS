package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Base abstract entity
 */
public abstract class User {
    protected String uid;
    protected String name;
    protected String email;
    protected String role;
    protected String password;
    protected List<String> notificationList;
    protected int level;

    public User() {
        this.notificationList = new ArrayList<>();
    }

    public User(String uid, String name, String email, String role, String password, List<String> notificationList, int level) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.password = password;
        this.notificationList = notificationList != null ? notificationList : new ArrayList<>();
        this.level = level;
    }

    public boolean login(String email, String password) {
        // Basic login logic validating against instance fields
        if (this.email != null && this.password != null) {
            return this.email.equals(email) && this.password.equals(password);
        }
        return false;
    }

    public void logout() {
        System.out.println(this.name + " has logged out.");
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getNotificationList() {
        return notificationList;
    }

    public void setNotificationList(List<String> notificationList) {
        this.notificationList = notificationList;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
