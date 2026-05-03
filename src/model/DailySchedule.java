package model;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * // GRASP Pattern: Information Expert (UC-04)
 */
public class DailySchedule {
    private Date date;
    private List<ClassSession> sessionList;

    public DailySchedule() {
        this.sessionList = new ArrayList<>();
    }

    public DailySchedule(Date date, List<ClassSession> sessionList) {
        this.date = date;
        this.sessionList = sessionList != null ? sessionList : new ArrayList<>();
    }

    public DailySchedule getDailySchedule(Date date) {
        System.out.println("Fetching daily schedule for date: " + date);
        return this;
    }

    // Getters and setters
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public List<ClassSession> getSessionList() { return sessionList; }
    public void setSessionList(List<ClassSession> sessionList) { this.sessionList = sessionList; }
}
