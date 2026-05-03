package ui;

import controller.AuthController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.StudentData;
import model.User;
import utils.UserSession;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * =============================================================================
 *  AuthUIController
 * -----------------------------------------------------------------------------
 *  JavaFX UI Controller backing BOTH:
 *      - Login.fxml
 *      - StudentSignup.fxml
 *
 *  MVC Responsibility:
 *  This class is a THIN UI controller. It is ONLY responsible for:
 *      1. Binding @FXML nodes defined in the FXML files.
 *      2. Performing lightweight client-side field validation.
 *      3. Routing ActionEvents to the backend domain controller
 *         ({@code controller.AuthController}) which owns all JDBC logic.
 *      4. Navigating between scenes (Login <-> Signup <-> Dashboard).
 *
 *  IMPORTANT: No SQL, JDBC, or persistence logic lives in this class.
 *  All database reads/writes are delegated to controller.AuthController.
 * =============================================================================
 */
public class AuthUIController {

    // ------------------------------------------------------------------
    //  @FXML bindings — Login.fxml
    // ------------------------------------------------------------------
    @FXML private TextField      emailField;
    @FXML private PasswordField  passwordField;
    @FXML private Button         loginButton;
    @FXML private Hyperlink      signupLink;
    @FXML private Label          loginMessageLabel;

    // ------------------------------------------------------------------
    //  @FXML bindings — StudentSignup.fxml
    // ------------------------------------------------------------------
    @FXML private TextField          rollNoField;
    @FXML private TextField          nameField;
    @FXML private TextField          signupEmailField;
    @FXML private PasswordField      signupPasswordField;
    @FXML private ComboBox<String>   batchComboBox;
    @FXML private ComboBox<String>   departmentComboBox;
    @FXML private ComboBox<String>   sectionComboBox;
    @FXML private Button             signupButton;
    @FXML private Label              signupMessageLabel;

    // ------------------------------------------------------------------
    //  Backend reference — ALL JDBC logic lives here, never in this class.
    // ------------------------------------------------------------------
    private final AuthController authController = new AuthController();

    // ==================================================================
    //  Initialization
    // ==================================================================
    /**
     * Called automatically by the FXMLLoader after @FXML fields are injected.
     * Populates the signup ComboBoxes from the database (only if the signup
     * view was loaded — on the Login screen those references will be null).
     */
    @FXML
    public void initialize() {
        if (isSignupView()) {
            // Section is cascaded from (batch, department), so start it
            // disabled and empty until both parents are chosen.
            sectionComboBox.setDisable(true);
            sectionComboBox.setPromptText("Select Batch & Dept first");

            loadParentDropdownsFromDatabase();
            wireCascadeListeners();
        }
    }

    private boolean isSignupView() {
        return batchComboBox != null && departmentComboBox != null && sectionComboBox != null;
    }

    /**
     * Loads only the Batch dropdown on startup.
     * Department cascades from batch; Section cascades from batch+dept.
     */
    private void loadParentDropdownsFromDatabase() {
        // Department starts disabled until batch is chosen
        departmentComboBox.setDisable(true);
        departmentComboBox.setPromptText("Select Batch first");

        new Thread(() -> {
            final List<String> batches = authController.getBatches();
            Platform.runLater(() -> batchComboBox.getItems().setAll(batches));
        }, "sapcis-batch-loader").start();
    }

    /**
     * Batch → Department → Section cascade listeners.
     */
    private void wireCascadeListeners() {
        // When batch changes → reload departments for that batch
        batchComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            departmentComboBox.getSelectionModel().clearSelection();
            departmentComboBox.getItems().clear();
            sectionComboBox.getSelectionModel().clearSelection();
            sectionComboBox.getItems().clear();
            sectionComboBox.setDisable(true);
            sectionComboBox.setPromptText("Select Batch & Dept first");

            if (newVal == null || newVal.trim().isEmpty()) {
                departmentComboBox.setDisable(true);
                departmentComboBox.setPromptText("Select Batch first");
                return;
            }
            departmentComboBox.setDisable(true);
            departmentComboBox.setPromptText("Loading departments...");

            new Thread(() -> {
                final List<String> depts = authController.getDepartmentsForBatch(newVal);
                Platform.runLater(() -> {
                    departmentComboBox.getItems().setAll(depts);
                    departmentComboBox.setDisable(depts.isEmpty());
                    departmentComboBox.setPromptText(
                            depts.isEmpty() ? "No departments for this batch" : "Select Department");
                });
            }, "sapcis-dept-loader").start();
        });

        // When department changes → reload sections for batch+dept
        departmentComboBox.valueProperty().addListener(
                (obs, oldVal, newVal) -> reloadSectionsForSelection());
    }

    /**
     * Fetches sections for the currently-selected batch + department.
     */
    private void reloadSectionsForSelection() {
        final String batch = batchComboBox.getValue();
        final String dept  = departmentComboBox.getValue();

        sectionComboBox.getSelectionModel().clearSelection();
        sectionComboBox.getItems().clear();

        if (batch == null || dept == null) {
            sectionComboBox.setDisable(true);
            sectionComboBox.setPromptText("Select Batch & Dept first");
            return;
        }

        sectionComboBox.setDisable(true);
        sectionComboBox.setPromptText("Loading sections...");

        new Thread(() -> {
            final List<String> sections = authController.getSectionsFor(batch, dept);
            Platform.runLater(() -> {
                sectionComboBox.getItems().setAll(sections);
                sectionComboBox.setDisable(sections.isEmpty());
                sectionComboBox.setPromptText(
                        sections.isEmpty() ? "No sections available" : "Select Section");
            });
        }, "sapcis-section-loader").start();
    }

    // ==================================================================
    //  LOGIN HANDLERS
    // ==================================================================

    /**
     * Handles the "Login" button click on Login.fxml.
     * Validates input and delegates authentication to the backend controller.
     */
    @FXML
    private void onLoginClicked(ActionEvent event) {
        String email    = safeTrim(emailField.getText());
        String password = safeTrim(passwordField.getText());

        if (email.isEmpty() || password.isEmpty()) {
            setError(loginMessageLabel, "Please enter both email and password.");
            return;
        }

        setInfo(loginMessageLabel, "Authenticating...");
        loginButton.setDisable(true);

        // Run the JDBC call off the UI thread.
        new Thread(() -> {
            // >>> BACKEND CALL: controller.AuthController handles all JDBC/SQL.
            final String role = authController.login(email, password);
            // On success, also fetch the full User row so we can seed the session.
            final User user = (role != null) ? authController.loadUserByEmail(email) : null;

            Platform.runLater(() -> {
                loginButton.setDisable(false);
                if (role != null && user != null) {
                    // Populate the global session BEFORE navigating so every
                    // downstream screen can call UserSession.getInstance().
                    UserSession.getInstance().setCurrentUser(user);
                    setSuccess(loginMessageLabel, "Welcome, " + user.getName() + "!");
                    navigateToDashboard(role);
                } else {
                    setError(loginMessageLabel, "Invalid email or password. Please try again.");
                    passwordField.clear();
                }
            });
        }, "sapcis-login").start();
    }

    /**
     * Handles the "Create Account" hyperlink click on Login.fxml.
     * Swaps the current scene to StudentSignup.fxml.
     */
    @FXML
    private void onGoToSignupClicked(ActionEvent event) {
        switchScene(event, "StudentSignup.fxml", "SAPCIS - Student Registration", 900, 700);
    }

    /**
     * Loads the correct dashboard FXML based on the role returned by backend.
     */
    private void navigateToDashboard(String role) {
        String fxmlFile;
        String title;
        String normalized = role == null ? "" : role.toLowerCase().trim();

        switch (normalized) {
            case "student":
                fxmlFile = "StudentDashboard.fxml";
                title    = "Student Dashboard";
                break;
            case "teacher":
                fxmlFile = "TeacherDashboard.fxml";
                title    = "Teacher Dashboard";
                break;
            case "admin":
            case "academiccoordinator":
                fxmlFile = "AdminDashboard.fxml";
                title    = "Admin Dashboard";
                break;
            default:
                setError(loginMessageLabel, "Unknown role returned from server: " + role);
                return;
        }

        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("SAPCIS - " + title);
        } catch (Throwable e) {
            // Surface the real root cause on the console — FXML LoadExceptions
            // wrap the actual NPE/ClassNotFoundException deep in the chain.
            System.err.println("========== FXML LOAD FAILED: " + fxmlFile + " ==========");
            e.printStackTrace();
            Throwable c = e.getCause();
            while (c != null) {
                System.err.println(">> caused by: " + c);
                c = c.getCause();
            }
            String rootMsg = (e.getCause() != null ? e.getCause().toString() : e.toString());
            setError(loginMessageLabel, "Could not open " + fxmlFile + ": " + rootMsg);
        }
    }

    // ==================================================================
    //  SIGNUP HANDLERS
    // ==================================================================

    /**
     * Handles the "Register & Auto-Enroll" button click on StudentSignup.fxml.
     * Validates input and delegates the registration + auto-enrollment
     * transaction to the backend AuthController.
     */
    @FXML
    private void onSignupClicked(ActionEvent event) {
        final String rollNo     = safeTrim(rollNoField.getText());
        final String fullName   = safeTrim(nameField.getText());
        final String email      = safeTrim(signupEmailField.getText());
        final String password   = safeTrim(signupPasswordField.getText());
        final String batch      = batchComboBox.getValue();
        final String department = departmentComboBox.getValue();
        final String section    = sectionComboBox.getValue();

        // Client-side validation only — no business rules here.
        if (rollNo.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()
                || batch == null || department == null || section == null) {
            setError(signupMessageLabel, "Please fill in every field and select all dropdowns.");
            return;
        }
        if (password.length() < 6) {
            setError(signupMessageLabel, "Password must be at least 6 characters long.");
            return;
        }
        if (!email.contains("@")) {
            setError(signupMessageLabel, "Please enter a valid email address.");
            return;
        }

        setInfo(signupMessageLabel, "Creating account and auto-enrolling...");
        signupButton.setDisable(true);

        // Build DTO and hand off to backend on a worker thread.
        final String uid = UUID.randomUUID().toString();
        final StudentData data = new StudentData(
                uid, fullName, email, password, rollNo, batch, department, section);

        new Thread(() -> {
            // >>> BACKEND CALL: controller.AuthController opens a JDBC
            // transaction and performs INSERT into users, students,
            // and auto-enrollment into timetable_db.
            final boolean ok = authController.registerStudent(data);

            Platform.runLater(() -> {
                signupButton.setDisable(false);
                if (ok) {
                    setSuccess(signupMessageLabel,
                            "Account created and auto-enrolled! Redirecting to login...");
                    // Small delay so the user can read the success banner.
                    new Thread(() -> {
                        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                        Platform.runLater(() ->
                                switchScene(event, "Login.fxml", "SAPCIS - Login", 900, 600));
                    }, "sapcis-redirect").start();
                } else {
                    setError(signupMessageLabel,
                            "Registration failed. Email or Roll No may already exist.");
                }
            });
        }, "sapcis-signup").start();
    }

    /**
     * Handles the "Back to Login" hyperlink click on StudentSignup.fxml.
     */
    @FXML
    private void onBackToLoginClicked(ActionEvent event) {
        switchScene(event, "Login.fxml", "SAPCIS - Login", 900, 600);
    }

    // ==================================================================
    //  PRIVATE UI HELPERS (no business logic)
    // ==================================================================

    /**
     * Loads a new FXML file into the current Stage.
     */
    private void switchScene(ActionEvent event, String fxmlFile, String title,
                             double width, double height) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, width, height));
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private void setError(Label label, String text) {
        if (label == null) return;
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #B91C1C; -fx-font-weight: bold;");
        label.setText(text);
    }

    private void setInfo(Label label, String text) {
        if (label == null) return;
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #1D4ED8; -fx-font-weight: bold;");
        label.setText(text);
    }

    private void setSuccess(Label label, String text) {
        if (label == null) return;
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #047857; -fx-font-weight: bold;");
        label.setText(text);
    }
}
