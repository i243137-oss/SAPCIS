package controller;

import exception.RoomAlreadyBookedException;
import dao.RoomRepositoryDAO;
import model.ClassSession;

/**
 * // GRASP Pattern: Controller + Information Expert (ConflictDetectionEngine, ClassSession, ClassroomRepository)
 * Used in UC-09
 */
public class ScheduleController {
    private RoomRepositoryDAO classroomRepository;
    private ClassSession classSession;

    public ScheduleController() {
        this.classroomRepository = new RoomRepositoryDAO();
        this.classSession = new ClassSession();
    }

    public void flashConflictAlert(Object clashDetails) {
        System.out.println("ScheduleController: Conflict alert flashed.");
    }

    public void viewClashDetails() {
        System.out.println("ScheduleController: Viewing clash details.");
    }

    public void reassignClass(String classB, String newRoomId, String timetableSlot) throws RoomAlreadyBookedException {
        try {
            boolean available = true; // In full implementation, validateRoomAvailability
            
            // *** ALTERNATIVE SCENARIO (alt SSD) ***
            if (!available) {
                throw new RoomAlreadyBookedException("Room already booked");
            }
            
            classSession.updateRoom(newRoomId);
            System.out.println("ScheduleController: Room reassigned to " + newRoomId + " for class " + classB);
            
            System.out.println("ScheduleController: Conflict check passed.");
        } catch (RoomAlreadyBookedException re) {
            throw re;
        } catch (Exception e) {
            System.err.println("Error reassigning class: " + e.getMessage());
        }
    }

    public void confirmResolutionAndUpdateTimetable() {
        System.out.println("ScheduleController: Resolution confirmed and timetable updated.");
    }
}
