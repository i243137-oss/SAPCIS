package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * =============================================================================
 *  ScheduleEntry  (DTO / View-Model)
 * -----------------------------------------------------------------------------
 *  Flattened row used by the Student Dashboard TableViews. It denormalises the
 *  joins across class_sessions / courses / sections / users / classrooms so
 *  the UI can bind directly without any further processing.
 *
 *  Uses JavaFX {@link StringProperty} so it can be bound via
 *  {@code PropertyValueFactory}.
 * =============================================================================
 */
public class ScheduleEntry {

    private final StringProperty sessionId   = new SimpleStringProperty();
    private final StringProperty subject     = new SimpleStringProperty();
    private final StringProperty teacherName = new SimpleStringProperty();
    private final StringProperty room        = new SimpleStringProperty();
    private final StringProperty time        = new SimpleStringProperty();
    private final StringProperty day         = new SimpleStringProperty();
    private final StringProperty status      = new SimpleStringProperty();

    public ScheduleEntry() { }

    public ScheduleEntry(String sessionId, String subject, String teacherName,
                         String room, String time, String day, String status) {
        this.sessionId.set(sessionId);
        this.subject.set(subject);
        this.teacherName.set(teacherName);
        this.room.set(room);
        this.time.set(time);
        this.day.set(day);
        this.status.set(status);
    }

    // ----- properties (for PropertyValueFactory) -----
    public StringProperty sessionIdProperty()   { return sessionId; }
    public StringProperty subjectProperty()     { return subject; }
    public StringProperty teacherNameProperty() { return teacherName; }
    public StringProperty roomProperty()        { return room; }
    public StringProperty timeProperty()        { return time; }
    public StringProperty dayProperty()         { return day; }
    public StringProperty statusProperty()      { return status; }

    // ----- plain getters -----
    public String getSessionId()   { return sessionId.get(); }
    public String getSubject()     { return subject.get(); }
    public String getTeacherName() { return teacherName.get(); }
    public String getRoom()        { return room.get(); }
    public String getTime()        { return time.get(); }
    public String getDay()         { return day.get(); }
    public String getStatus()      { return status.get(); }

    // ----- setters -----
    public void setSessionId(String v)   { sessionId.set(v); }
    public void setSubject(String v)     { subject.set(v); }
    public void setTeacherName(String v) { teacherName.set(v); }
    public void setRoom(String v)        { room.set(v); }
    public void setTime(String v)        { time.set(v); }
    public void setDay(String v)         { day.set(v); }
    public void setStatus(String v)      { status.set(v); }
}
