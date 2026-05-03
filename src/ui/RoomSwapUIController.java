package ui;

import controller.RoomSwapController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;

public class RoomSwapUIController {

    @FXML
    private TextField classIdField;
    @FXML
    private TextField reasonField;
    @FXML
    private TextField capacityField;
    @FXML
    private TextField specificRoomIdField;
    @FXML
    private ComboBox<String> conditionComboBox;
    @FXML
    private Label statusLabel;

    @FXML
    private void onRequestSwapClicked(ActionEvent event) {
        RoomSwapController roomSwapController = new RoomSwapController();
        String classId = classIdField.getText();
        String reason = reasonField.getText();
        String condition = conditionComboBox.getValue();

        try {
            if ("LET_ADMIN_CHOOSE".equals(condition)) {
                int capacity = Integer.parseInt(capacityField.getText());
                roomSwapController.requestRoomSwap(classId, reason, capacity);
            } else if ("REQUEST_SPECIFIC_ROOM".equals(condition)) {
                String roomId = specificRoomIdField.getText();
                roomSwapController.requestSpecificRoom(classId, roomId, reason);
            }
            roomSwapController.showSwapPendingConfirmation();
            statusLabel.setText("Swap request submitted.");
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid capacity format.");
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }
}
