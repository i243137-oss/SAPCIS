package model;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class Schedule {
    private String scheduleId;
    private Date startDate;
    private Date endDate;
    private List<String> classList;

    public Schedule() {
        this.classList = new ArrayList<>();
    }

    public Schedule(String scheduleId, Date startDate, Date endDate, List<String> classList) {
        this.scheduleId = scheduleId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.classList = classList != null ? classList : new ArrayList<>();
    }

    public Schedule getCurrentSchedule() {
        System.out.println("Fetching current schedule details...");
        return this;
    }

    public void updateScheduleDetails() {
        System.out.println("Updating schedule details for schedule ID: " + scheduleId);
    }

    // Getters and Setters
    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public List<String> getClassList() { return classList; }
    public void setClassList(List<String> classList) { this.classList = classList; }
}
