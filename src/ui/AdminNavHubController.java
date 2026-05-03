package ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

public class AdminNavHubController {

    @FXML
    private void openTeacherDashboard(ActionEvent event) { openWindow("TeacherDashboard.fxml", "Teacher Dashboard"); }

    @FXML
    private void openRoomSwap(ActionEvent event) { openWindow("RoomSwap.fxml", "Room Swap Request"); }

    @FXML
    private void openSubstitute(ActionEvent event) { openWindow("AdminSubstitute.fxml", "Substitute Assignment"); }

    @FXML
    private void openSchedule(ActionEvent event) { openWindow("AdminSchedule.fxml", "Resolve Clash"); }

    @FXML
    private void openRules(ActionEvent event) { openWindow("AdminRules.fxml", "Rule Configuration"); }

    @FXML
    private void openOverride(ActionEvent event) { openWindow("AdminOverride.fxml", "Emergency Override"); }

    @FXML
    private void openReport(ActionEvent event) { openWindow("AdminReport.fxml", "Analytics Reports"); }

    @FXML
    private void logout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 520, 550));
            stage.setTitle("SAPCIS - Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openWindow(String fxmlFile, String title) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = new Stage();
            stage.setTitle("SAPCIS - " + title);
            stage.setScene(new Scene(view, 520, 480));
            stage.show();
        } catch (Exception e) {
            System.err.println("Failed to load " + fxmlFile + ": " + e.getMessage());
        }
    }
}
