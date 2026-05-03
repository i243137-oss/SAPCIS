package controller;

import model.Classroom;
import model.Rule;
import java.util.List;

/**
 * // GRASP Pattern: Controller + Information Expert (Classroom, ConflictDetectionEngine) + GRASP Creator (Rule)
 * Used in UC-10
 */
public class RuleController {
    private Classroom classroom;

    public RuleController() {
        this.classroom = new Classroom();
    }

    public void openRuleEnforcementSettings() {
        System.out.println("RuleController: Rule enforcement settings opened.");
    }

    public void selectClassroom(String roomId) {
        classroom.setRoomId(roomId);
        List<String> categories = classroom.getRuleCategories();
        System.out.println("Categories found: " + categories);
        
        String constraints = classroom.getCurrentConstraints();
        System.out.println("Current constraints: " + constraints);
    }

    public void setMaxCapacity(String roomId, int capacityValue) {
        // GRASP Creator: RuleController supplies all init data for Rule.
        Rule newRule = Rule.create(roomId, "MAX_CAPACITY", capacityValue);
        
        System.out.println("RuleController: Rule applied and enforced.");
        confirmRuleSavedAndApplied();
    }

    public void selectGlobalRules() {
        System.out.println("RuleController: Global rules selected.");
    }

    public void setMaxClassesPerTeacher(int maxClasses) {
        System.out.println("RuleController: Max classes per teacher set to " + maxClasses);
    }

    public void confirmGlobalRuleEnforced() {
        System.out.println("RuleController: Global rule enforced successfully.");
    }
    
    private void confirmRuleSavedAndApplied() {
        System.out.println("RuleController: Rule saved and applied confirmed.");
    }
}
