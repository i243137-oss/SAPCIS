package model;

import java.util.UUID;

/**
 * // GRASP Pattern: Creator called from RuleController (UC-10)
 */
public class Rule {
    private String ruleId;
    private String ruleName;
    private String description;
    private String type;
    private String value;
    private boolean isActive;

    public Rule() {}

    public Rule(String ruleId, String ruleName, String description, String type, String value, boolean isActive) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.description = description;
        this.type = type;
        this.value = value;
        this.isActive = isActive;
    }

    public static Rule create(String roomId, String type, int capacityValue) {
        System.out.println("GRASP Creator: Creating Rule for Room: " + roomId);
        return new Rule(UUID.randomUUID().toString(), "Room Rule", "Auto-generated rule", type, String.valueOf(capacityValue), true);
    }

    public void applyNewRule() {
        System.out.println("Applying new rule: " + ruleName);
    }

    public boolean checkRoomAvailability() {
        System.out.println("Checking room availability under rule: " + ruleName);
        return true;
    }

    // Getters and setters
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }
}
