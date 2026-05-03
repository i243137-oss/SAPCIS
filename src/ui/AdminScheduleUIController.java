package ui;

import controller.ScheduleController;
import exception.RoomAlreadyBookedException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

public class AdminScheduleUIController {

    @FXML
    private TextField selectedClassField;
    @FXML
    private TextField newRoomIdField;
    @FXML
    private TextField timetableSlotField;
    @FXML
    private Label errorLabel;

    @FXML
    private void onReassignClicked(ActionEvent event) {
        ScheduleController sc = new ScheduleController();
        try {
            String selectedClass = selectedClassField.getText();
            String newRoomId = newRoomIdField.getText();
            String slot = timetableSlotField.getText();
            
            sc.reassignClass(selectedClass, newRoomId, slot);
            sc.confirmResolutionAndUpdateTimetable();
            errorLabel.setText("Class reassigned successfully.");
        } catch (RoomAlreadyBookedException e) {
            // Alternative Scenario: show error, keep room selection form open
            errorLabel.setText("Room already booked. Please select another.");
            newRoomIdField.clear();
        } catch (Exception e) {
            errorLabel.setText("Error: " + e.getMessage());
        }
    }
}
