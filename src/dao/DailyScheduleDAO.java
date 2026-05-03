package dao;

import db.DBConnection;
import model.DailySchedule;
import model.ClassSession;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class DailyScheduleDAO {

    public DailySchedule getDailySchedule(Date date) throws SQLException {
        List<ClassSession> sessions = new ArrayList<>();
        String query = "SELECT * FROM class_sessions";
        try (Connection conn = DBConnection.getInstance();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                sessions.add(new ClassSession(
                    rs.getString("sessionId"),
                    rs.getTime("startTime").toLocalTime(),
                    rs.getTime("endTime").toLocalTime(),
                    rs.getString("status"),
                    rs.getString("timetableSlot"),
                    rs.getString("roomNumber")
                ));
            }
        }
        return new DailySchedule(date, sessions);
    }
}
