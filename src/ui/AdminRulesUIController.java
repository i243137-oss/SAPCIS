package ui;

import controller.RuleController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;

public class AdminRulesUIController {

    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private TextField roomIdField;
    @FXML
    private TextField capacityValueField;
    @FXML
    private TextField maxClassesField;
    @FXML
    private Label statusLabel;

    @FXML
    private void onApplyRuleClicked(ActionEvent event) {
        RuleController ruleController = new RuleController();
        String selectedCategory = categoryComboBox.getValue();

        try {
            if ("GLOBAL_FACULTY".equals(selectedCategory)) {
                ruleController.selectGlobalRules();
                int maxClasses = Integer.parseInt(maxClassesField.getText());
                ruleController.setMaxClassesPerTeacher(maxClasses);
                ruleController.confirmGlobalRuleEnforced();
            } else {
                String roomId = roomIdField.getText();
                ruleController.selectClassroom(roomId);
                int capacityValue = Integer.parseInt(capacityValueField.getText());
                ruleController.setMaxCapacity(roomId, capacityValue);
            }
            statusLabel.setText("Rule applied successfully.");
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid number format.");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}
