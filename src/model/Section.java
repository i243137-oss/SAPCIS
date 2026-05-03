package model;

import java.time.LocalTime;

public class Section {
    private String sectionId;
    private String sectionName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timetableSlot;
    private String roomNumber;

    public Section() {}

    public Section(String sectionId, String sectionName, LocalTime startTime, LocalTime endTime, String timetableSlot, String roomNumber) {
        this.sectionId = sectionId;
        this.sectionName = sectionName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timetableSlot = timetableSlot;
        this.roomNumber = roomNumber;
    }

    public String getAssignedRoom() {
        return roomNumber;
    }

    public String getAssignedTeacher() {
        return "Teacher_ID_Placeholder"; // Needs linkage to actual teacher assignment
    }

    // Getters and setters
    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getTimetableSlot() { return timetableSlot; }
    public void setTimetableSlot(String timetableSlot) { this.timetableSlot = timetableSlot; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
}
