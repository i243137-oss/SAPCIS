package ui;

import controller.OverrideController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;

public class AdminOverrideUIController {

    @FXML
    private TextField sessionIdField;
    @FXML
    private TextField emergencyReasonField;
    @FXML
    private TextField newStatusField;
    @FXML
    private ComboBox<String> adminActionBox;
    @FXML
    private Label statusLabel;

    @FXML
    private void onExecuteActionClicked(ActionEvent event) {
        OverrideController overrideController = new OverrideController();
        String adminAction = adminActionBox.getValue();

        try {
            if ("OVERRIDE_STATUS".equals(adminAction)) {
                String sessionId = sessionIdField.getText();
                String newStatus = newStatusField.getText();
                String emergencyReason = emergencyReasonField.getText();
                
                overrideController.overrideStatus(sessionId, newStatus, emergencyReason);
                overrideController.confirmOverrideAndNotifyParties();
                statusLabel.setText("Status overridden successfully.");
            } else if ("INITIATE_ROOM_MOVE".equals(adminAction)) {
                overrideController.initiateRoomMove();
                statusLabel.setText("Navigating to RoomSwap...");
            }
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}
