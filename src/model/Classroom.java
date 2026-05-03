package model;

import java.util.ArrayList;
import java.util.List;

/**
 * // GRASP Pattern: Information Expert (UC-10)
 */
public class Classroom {
    private String roomId;
    private String roomName;
    private int capacity;
    private boolean hasProjector;
    private String location;

    public Classroom() {}

    public Classroom(String roomId, String roomName, int capacity, boolean hasProjector, String location) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.capacity = capacity;
        this.hasProjector = hasProjector;
        this.location = location;
    }

    public List<String> getRuleCategories() {
        System.out.println("Fetching rule categories for classroom: " + roomId);
        List<String> categories = new ArrayList<>();
        categories.add("MAX_CAPACITY");
        categories.add("EQUIPMENT");
        return categories;
    }

    public String getCurrentConstraints() {
        System.out.println("Fetching current constraints for classroom: " + roomId);
        return "Capacity: " + capacity + ", Projector: " + hasProjector;
    }

    public boolean checkExistingRules() {
        System.out.println("Checking existing rules for classroom: " + roomId);
        return false;
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public boolean isHasProjector() { return hasProjector; }
    public void setHasProjector(boolean hasProjector) { this.hasProjector = hasProjector; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
