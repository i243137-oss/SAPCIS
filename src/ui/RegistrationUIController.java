package ui;

import controller.AuthController;
import model.StudentData;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.UUID;

public class RegistrationUIController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField rollNoField;
    @FXML private ComboBox<String> batchComboBox;
    @FXML private ComboBox<String> deptComboBox;
    @FXML private ComboBox<String> sectionComboBox;
    @FXML private Label messageLabel;

    private AuthController authController = new AuthController();

    @FXML
    public void initialize() {
        // Populate dropdowns with common values
        batchComboBox.getItems().addAll("2021", "2022", "2023", "2024", "2025");
        deptComboBox.getItems().addAll("Computer Science", "Software Engineering", "Information Technology", "Business Admin");
        sectionComboBox.getItems().addAll("A", "B", "C", "D", "E");
    }

    @FXML
    private void handleRegistration(ActionEvent event) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String rollNo = rollNoField.getText().trim();
        String batch = batchComboBox.getValue();
        String dept = deptComboBox.getValue();
        String section = sectionComboBox.getValue();

        // Validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || rollNo.isEmpty() || section == null) {
            showMessage("Please fill in all mandatory (*) fields.", "message-error");
            return;
        }
        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters.", "message-error");
            return;
        }
        if (!email.contains("@")) {
            showMessage("Please enter a valid email address.", "message-error");
            return;
        }

        String uid = UUID.randomUUID().toString();
        StudentData data = new StudentData(uid, name, email, password, rollNo, batch, dept, section);

        boolean success = authController.registerStudent(data);
        if (success) {
            showMessage("✅ Registration & Auto-Enrollment Successful! Redirecting to login...", "message-success");
            // Auto-redirect to login after a short delay
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(this::navigateToLogin);
            }).start();
        } else {
            showMessage("Registration failed. Email or Roll No may already exist.", "message-error");
        }
    }

    @FXML
    private void backToLogin(ActionEvent event) {
        navigateToLogin();
    }

    private void navigateToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root, 520, 550));
            stage.setTitle("SAPCIS - Login");
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
