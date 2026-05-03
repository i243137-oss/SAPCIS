package dao;

import db.DBConnection;
import model.Teacher;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DirectoryServiceDAO {

    public List<Teacher> getFreeTeachers(String timetableSlot) throws SQLException {
        List<Teacher> freeTeachers = new ArrayList<>();
        String query = "SELECT * FROM users WHERE role = 'Teacher'"; 
        try (Connection conn = DBConnection.getInstance();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Teacher t = new Teacher();
                t.setUid(rs.getString("uid"));
                t.setName(rs.getString("name"));
                t.setRole(rs.getString("role"));
                freeTeachers.add(t);
            }
        }
        return freeTeachers;
    }

    public Teacher searchTeacherByName(String name) throws SQLException {
        String query = "SELECT * FROM users WHERE role = 'Teacher' AND name = ?";
        try (Connection conn = DBConnection.getInstance();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Teacher t = new Teacher();
                    t.setUid(rs.getString("uid"));
                    t.setName(rs.getString("name"));
                    t.setRole(rs.getString("role"));
                    return t;
                }
            }
        }
        return null;
    }
}
