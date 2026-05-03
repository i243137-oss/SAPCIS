package ui;

import controller.SubstituteController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

public class AdminSubstituteUIController {

    @FXML
    private TextField sessionIdField;
    @FXML
    private TextField originalTeacherIdField;
    @FXML
    private TextField substituteTeacherIdField;
    @FXML
    private TextField reasonField;
    @FXML
    private Label declineAlert;

    @FXML
    private void onAssignClicked(ActionEvent event) {
        SubstituteController sc = new SubstituteController();
        try {
            sc.assignTeacher(originalTeacherIdField.getText(), substituteTeacherIdField.getText());
            declineAlert.setText("Teacher assigned successfully.");
        } catch (Exception e) {
            declineAlert.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void onDeclineReceived(ActionEvent event) {
        SubstituteController sc = new SubstituteController();
        String teacherId = substituteTeacherIdField.getText();
        String sessionId = sessionIdField.getText();
        String reason = reasonField.getText();
        
        sc.handleSubstituteResponse(teacherId, sessionId, false, reason);
        declineAlert.setText("Session returned to Unassigned.");
        
        sc.reopenSubstituteSearch();
    }
}
