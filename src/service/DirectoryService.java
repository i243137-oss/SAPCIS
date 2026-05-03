package service;

import dao.DirectoryServiceDAO;
import model.Teacher;
import java.util.List;
import java.util.ArrayList;

/**
 * // GRASP Pattern: High Cohesion Fabrication
 * Used in UC-08, UC-11
 */
public class DirectoryService {
    private DirectoryServiceDAO directoryServiceDAO;

    public DirectoryService() {
        this.directoryServiceDAO = new DirectoryServiceDAO();
    }

    public List<Teacher> getFreeTeachers(String timetableSlot) {
        try {
            return directoryServiceDAO.getFreeTeachers(timetableSlot);
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Teacher searchTeacherByName(String name) {
        try {
            return directoryServiceDAO.searchTeacherByName(name);
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            return null;
        }
    }
}
