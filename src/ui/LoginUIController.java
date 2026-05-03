package ui;

import controller.AuthController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginUIController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private AuthController authController = new AuthController();

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Please enter both email and password.", "message-error");
            return;
        }

        String role = authController.login(email, password);
        if (role != null) {
            showMessage("Welcome! Logged in as: " + role, "message-success");
            // Navigate to appropriate dashboard based on role
            navigateToDashboard(role);
        } else {
            showMessage("Invalid email or password. Try again.", "message-error");
            passwordField.clear();
        }
    }

    private void navigateToDashboard(String role) {
        try {
            String fxmlFile;
            String title;
            
            // Normalize case for comparison
            String normalizedRole = role.toLowerCase().trim();

            switch (normalizedRole) {
                case "student":
                    fxmlFile = "StudentDashboard.fxml";
                    title = "Student Dashboard";
                    break;
                case "teacher":
                    fxmlFile = "TeacherDashboard.fxml";
                    title = "Teacher Dashboard";
                    break;
                case "admin":
                case "academiccoordinator":
                    fxmlFile = "AdminNavHub.fxml";
                    title = "Admin Dashboard";
                    break;
                default:
                    showMessage("Unknown role: " + role, "message-error");
                    return;
            }
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, 600, 700);
            stage.setScene(scene);
            stage.setTitle("SAPCIS - " + title);
        } catch (IOException e) {
            showMessage("Dashboard not found: " + e.getMessage(), "message-error");
        }
    }

    @FXML
    private void goToRegistration(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("Registration.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 520, 700));
            stage.setTitle("SAPCIS - Student Registration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String text, String styleClass) {
        messageLabel.getStyleClass().removeAll("message-success", "message-error", "message-info");
        messageLabel.getStyleClass().add(styleClass);
        messageLabel.setText(text);
    }
}
