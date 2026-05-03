package ui;

import controller.RoomSwapController;
import controller.SubstituteController;
import controller.TeacherController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import model.ScheduleEntry;
import model.User;
import utils.UserSession;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * =============================================================================
 *  TeacherDashboardController  (UC-01)
 * -----------------------------------------------------------------------------
 *  Backs {@code ui/TeacherDashboard.fxml}.
 *
 *  Responsibilities (UI layer ONLY — no SQL here):
 *      • Pull the logged-in Teacher from {@link UserSession}.
 *      • Ask the backend {@link TeacherController} for that teacher's
 *        weekly schedule and render it across 5 day-of-week TableViews.
 *      • Listen for row selections in any of those TableViews; when a row
 *        is clicked, enable the top "Action Console" and expose the row to
 *        the Submit flow.
 *      • When Submit is pressed, delegate the DB write + Observer cascade
 *        to {@link TeacherController#updateSessionStatus} and then refresh
 *        the tables so the new status is visible immediately.
 *
 *  ARCHITECTURE:
 *      Strict MVC — this class does not call JDBC directly. Every DB read
 *      or write is proxied through the domain controller in the
 *      {@code controller/} package.
 * =============================================================================
 */
public class TeacherDashboardController {

    // ------------------------------------------------------------------
    //  Sidebar (profile)
    // ------------------------------------------------------------------
    @FXML private Label profileNameLabel;
    @FXML private Label profileUidLabel;
    @FXML private Label profileRoleLabel;

    @FXML private Button navMyScheduleButton;
    @FXML private Button navSubstituteButton;
    @FXML private Button navRoomSwapButton;
    @FXML private Button refreshButton;
    @FXML private Button logoutButton;

    // ------------------------------------------------------------------
    //  Action console
    // ------------------------------------------------------------------
    @FXML private Label             todayLabel;
    @FXML private GridPane          actionConsole;
    @FXML private Label             selectedClassLabel;
    @FXML private ComboBox<String>  statusCombo;
    @FXML private TextField         reasonField;
    @FXML private Label             consoleMessageLabel;
    @FXML private Button            clearSelectionButton;
    @FXML private Button            submitUpdateButton;

    // ------------------------------------------------------------------
    //  Main tab pane (3 tabs: My Schedule, Request Substitute, Inbox)
    // ------------------------------------------------------------------
    @FXML private TabPane mainTabPane;

    // ------------------------------------------------------------------
    //  Weekly schedule tabs / tables
    // ------------------------------------------------------------------
    @FXML private TabPane weekTabPane;
    @FXML private TableView<ScheduleEntry> mondayTable;
    @FXML private TableView<ScheduleEntry> tuesdayTable;
    @FXML private TableView<ScheduleEntry> wednesdayTable;
    @FXML private TableView<ScheduleEntry> thursdayTable;
    @FXML private TableView<ScheduleEntry> fridayTable;

    // ------------------------------------------------------------------
    //  Request Substitute panel (Teacher → Admin) (UC-08)
    // ------------------------------------------------------------------
    @FXML private TableView<java.util.Map<String, String>>           mySessionsTable;
    @FXML private TableColumn<java.util.Map<String, String>, String> myColCourse;
    @FXML private TableColumn<java.util.Map<String, String>, String> myColSection;
    @FXML private TableColumn<java.util.Map<String, String>, String> myColDay;
    @FXML private TableColumn<java.util.Map<String, String>, String> myColTime;
    @FXML private TableColumn<java.util.Map<String, String>, String> myColRoom;
    @FXML private TextField                                          subRequestReasonField;
    @FXML private Label                                              subRequestStatusLabel;

    // ------------------------------------------------------------------
    //  Substitute Inbox (Admin → Teacher) (UC-08)
    // ------------------------------------------------------------------
    @FXML private TableView<java.util.Map<String, String>>         subInboxTable;
    @FXML private TableColumn<java.util.Map<String, String>, String> subInColId;
    @FXML private TableColumn<java.util.Map<String, String>, String> subInColCourse;
    @FXML private TableColumn<java.util.Map<String, String>, String> subInColDay;
    @FXML private TableColumn<java.util.Map<String, String>, String> subInColTime;
    @FXML private TableColumn<java.util.Map<String, String>, String> subInColFrom;
    @FXML private TableColumn<java.util.Map<String, String>, String> subInColReason;
    @FXML private Label                                             subInboxStatusLabel;

    // ------------------------------------------------------------------
    //  UC-02: Room Swap console (Tab 1, below Action Console)
    // ------------------------------------------------------------------
    @FXML private javafx.scene.layout.VBox roomSwapConsole;
    @FXML private TextField                swapCapacityField;
    @FXML private TextField                swapReasonField;
    @FXML private Label                    swapStatusLabel;

    // ------------------------------------------------------------------
    //  Runtime state
    // ------------------------------------------------------------------
    private TeacherController teacherController;
    private SubstituteController substituteCtrl;
    private RoomSwapController roomSwapCtrl;
    private User              currentTeacher;

    /** The session that is currently populated in the Action Console. */
    private ScheduleEntry selectedEntry;

    // ==================================================================
    //  Lifecycle
    // ==================================================================

    @FXML
    public void initialize() {
        // 1) Resolve the logged-in teacher from the session singleton.
        UserSession session = UserSession.getInstance();
        currentTeacher = session.getCurrentUser();

        // 2) Instantiate the backend domain controllers (NO JDBC in this class).
        teacherController = new TeacherController();
        substituteCtrl    = new SubstituteController();
        roomSwapCtrl      = new RoomSwapController();

        // 3) Populate the sidebar profile.
        if (currentTeacher != null) {
            profileNameLabel.setText(safe(currentTeacher.getName()));
            profileUidLabel.setText("ID: " + safe(currentTeacher.getUid()));
            profileRoleLabel.setText("Role: "
                    + (currentTeacher.getRole() == null ? "Teacher" : currentTeacher.getRole()));
        } else {
            profileNameLabel.setText("(not signed in)");
        }

        // 4) Today's date in the header.
        todayLabel.setText(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH)));

        // 5) Status dropdown (DELAYED / CANCELLED).
        statusCombo.setItems(FXCollections.observableArrayList("DELAYED", "CANCELLED"));

        // 6) Build the five TableViews and wire up the row-selection listeners.
        configureTable(mondayTable);
        configureTable(tuesdayTable);
        configureTable(wednesdayTable);
        configureTable(thursdayTable);
        configureTable(fridayTable);

        // 7) Focus today's tab if it's a weekday.
        selectTodayTab();

        // 8) Setup substitute panels (UC-08)
        initMySessionsTable();
        initSubstituteInbox();

        // 9) First data load.
        reloadSchedule();
        loadMySessionsTable();
        loadSubstituteInbox();
    }

    // ==================================================================
    //  Request Substitute — Teacher → Admin (UC-08)
    // ==================================================================

    private void initMySessionsTable() {
        myColCourse.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getOrDefault("courseCode", "") + " " + cd.getValue().getOrDefault("courseName", "")));
        myColSection.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("sectionName", "")));
        myColDay.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("dayOfWeek", "")));
        myColTime.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getOrDefault("startTime", "") + "–" + cd.getValue().getOrDefault("endTime", "")));
        myColRoom.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("roomId", "")));
    }

    private void loadMySessionsTable() {
        if (currentTeacher == null) return;
        try {
            List<java.util.Map<String, String>> sessions =
                    substituteCtrl.getMyAssignments(currentTeacher.getUid());
            mySessionsTable.setItems(FXCollections.observableArrayList(sessions));
        } catch (Exception e) {
            // silently ignore — table stays empty
        }
    }

    @FXML
    public void onRequestSubstituteClicked(ActionEvent event) {
        java.util.Map<String, String> sel = mySessionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showError("No session selected", "Select one of your sessions from the table above.");
            return;
        }
        String reason = subRequestReasonField.getText();
        if (reason == null || reason.trim().isEmpty()) {
            showError("No reason", "Please enter a reason for requesting a substitute.");
            return;
        }

        int assignmentId;
        try { assignmentId = Integer.parseInt(sel.get("assignmentId")); }
        catch (NumberFormatException e) { showError("Bad ID", "Invalid assignment."); return; }

        String course = sel.getOrDefault("courseCode", "") + " " + sel.getOrDefault("courseName", "");
        String day = sel.getOrDefault("dayOfWeek", "");
        String time = sel.getOrDefault("startTime", "") + "–" + sel.getOrDefault("endTime", "");

        Alert confirm = new Alert(AlertType.CONFIRMATION,
                "Send substitute request to Admin?\n\n"
              + "Course: " + course + "\n"
              + "Day: " + day + " " + time + "\n"
              + "Reason: " + reason.trim(),
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Substitute Request");
        confirm.showAndWait().ifPresent(choice -> {
            if (choice == ButtonType.YES) {
                try {
                    String subId = substituteCtrl.requestSubstituteForSelf(
                            assignmentId, currentTeacher.getUid(), reason.trim());
                    subRequestStatusLabel.setText("✓ Request " + subId + " sent to Admin");
                    subRequestStatusLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                    subRequestReasonField.clear();
                    showInfo("Request sent ✓",
                            "Your substitute request has been sent to the Admin.\n\n"
                          + "Request ID: " + subId + "\n"
                          + "Course: " + course + "\n"
                          + "Day: " + day + " " + time + "\n\n"
                          + "The Admin will find a qualified substitute and notify you.");
                } catch (Exception e) {
                    showError("Request failed", e.getMessage());
                    subRequestStatusLabel.setText("✗ Failed: " + e.getMessage());
                    subRequestStatusLabel.setStyle("-fx-text-fill: #DC2626;");
                }
            }
        });
    }

    // ==================================================================
    //  Substitute Inbox (Admin → Teacher) (UC-08)
    // ==================================================================

    private void initSubstituteInbox() {
        // Wire table columns
        subInColId.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("substituteId", "")));
        subInColCourse.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getOrDefault("courseCode", "") + " " + cd.getValue().getOrDefault("courseName", "")));
        subInColDay.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("dayOfWeek", "")));
        subInColTime.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getOrDefault("startTime", "") + "–" + cd.getValue().getOrDefault("endTime", "")));
        subInColFrom.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("originalName", "")));
        subInColReason.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("reason", "")));
    }

    private void loadSubstituteInbox() {
        if (currentTeacher == null) return;
        try {
            List<java.util.Map<String, String>> requests =
                    substituteCtrl.getPendingRequestsForTeacher(currentTeacher.getUid());
            subInboxTable.setItems(FXCollections.observableArrayList(requests));
            subInboxStatusLabel.setText(requests.isEmpty() ? "No pending requests"
                    : requests.size() + " pending request(s)");
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage() == null ? "Unknown SQL error" : e.getMessage();
            // If the table simply doesn't exist yet, show a friendly hint
            if (msg.contains("substitute_assignments") || msg.contains("Invalid object name")) {
                subInboxStatusLabel.setText("⚠ Run migrate_substitute_assignments.sql first");
            } else {
                subInboxStatusLabel.setText("DB error: " + msg);
            }
            subInboxTable.setItems(FXCollections.observableArrayList());
        } catch (Exception e) {
            subInboxStatusLabel.setText("Error: " + e.getMessage());
            subInboxTable.setItems(FXCollections.observableArrayList());
        }
    }

    @FXML
    public void onAcceptSubstitute(ActionEvent event) {
        java.util.Map<String, String> sel = subInboxTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showError("No selection", "Select a substitute request from the table.");
            return;
        }
        String subId = sel.get("substituteId");
        String course = sel.getOrDefault("courseCode", "") + " " + sel.getOrDefault("courseName", "");
        String from = sel.getOrDefault("originalName", "");

        Alert confirm = new Alert(AlertType.CONFIRMATION,
                "Accept substitute request?\n\n"
              + "Course: " + course + "\n"
              + "From: " + from + "\n"
              + "Day: " + sel.getOrDefault("dayOfWeek", "") + " "
              + sel.getOrDefault("startTime", "") + "–" + sel.getOrDefault("endTime", ""),
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Acceptance");
        confirm.showAndWait().ifPresent(choice -> {
            if (choice == ButtonType.YES) {
                try {
                    substituteCtrl.acceptSubstitute(subId);
                    showInfo("Request accepted ✓",
                            "You have accepted the substitute request for " + course + ".\n"
                          + "The admin and original teacher will be notified.");
                    loadSubstituteInbox();
                    reloadSchedule(); // refresh schedule to show new assignment
                } catch (Exception e) {
                    showError("Accept failed", e.getMessage());
                }
            }
        });
    }

    @FXML
    public void onRejectSubstitute(ActionEvent event) {
        java.util.Map<String, String> sel = subInboxTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showError("No selection", "Select a substitute request from the table.");
            return;
        }
        String subId = sel.get("substituteId");
        String course = sel.getOrDefault("courseCode", "") + " " + sel.getOrDefault("courseName", "");

        Alert confirm = new Alert(AlertType.CONFIRMATION,
                "Reject substitute request for " + course + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Rejection");
        confirm.showAndWait().ifPresent(choice -> {
            if (choice == ButtonType.YES) {
                try {
                    substituteCtrl.rejectSubstitute(subId);
                    showInfo("Request rejected",
                            "You have declined the substitute request.\n"
                          + "The admin will be notified and can search for another substitute.");
                    loadSubstituteInbox();
                } catch (Exception e) {
                    showError("Reject failed", e.getMessage());
                }
            }
        });
    }

    /**
     * Reads the full weekly schedule for the current teacher from the
     * backend and distributes it into the 5 day-of-week tables.
     */
    private void reloadSchedule() {
        if (currentTeacher == null) return;

        final String uid = currentTeacher.getUid();
        List<ScheduleEntry> all = teacherController.getScheduleForTeacher(uid);

        mondayTable.setItems(filterByDay(all, "Monday"));
        tuesdayTable.setItems(filterByDay(all, "Tuesday"));
        wednesdayTable.setItems(filterByDay(all, "Wednesday"));
        thursdayTable.setItems(filterByDay(all, "Thursday"));
        fridayTable.setItems(filterByDay(all, "Friday"));

        // Re-enable tables and reset any prior selection.
        clearSelection();
    }

    // ==================================================================
    //  TableView setup
    // ==================================================================

    /**
     * Creates the 5 columns expected by the FXML contract and attaches a
     * {@link ChangeListener} to the selection model so the Action Console
     * lights up when the teacher clicks any row.
     *
     * Columns (left-to-right):
     *     Section | Course Name | Room | Time | Current Status
     *
     * Note on mapping: {@link ScheduleEntry#teacherName} is repurposed
     * as the "Section" label in {@link TeacherController#getScheduleForTeacher},
     * which is why the first column binds to {@code teacherName}.
     */
    private void configureTable(TableView<ScheduleEntry> table) {
        if (table == null) return;

        table.getColumns().clear();

        TableColumn<ScheduleEntry, String> colSection = new TableColumn<>("Section");
        colSection.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        colSection.setPrefWidth(120);

        TableColumn<ScheduleEntry, String> colCourse = new TableColumn<>("Course Name");
        colCourse.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colCourse.setPrefWidth(260);

        TableColumn<ScheduleEntry, String> colRoom = new TableColumn<>("Room");
        colRoom.setCellValueFactory(new PropertyValueFactory<>("room"));
        colRoom.setPrefWidth(130);

        TableColumn<ScheduleEntry, String> colTime = new TableColumn<>("Time");
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colTime.setPrefWidth(150);

        TableColumn<ScheduleEntry, String> colStatus = new TableColumn<>("Current Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(150);

        table.getColumns().addAll(
                Arrays.asList(colSection, colCourse, colRoom, colTime, colStatus));
        table.setPlaceholder(new Label("No classes scheduled."));

        // Single-selection is the default; we add the row-selection listener
        // that enables and populates the Action Console.
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> onRowSelected(table, newSel));
    }

    /**
     * Invoked whenever the user selects a different row in any day-table.
     * Also clears the other 4 tables' selection so only ONE row is ever
     * selected across the whole TabPane.
     */
    private void onRowSelected(TableView<ScheduleEntry> source, ScheduleEntry entry) {
        if (entry == null) return;

        // Deselect peers so the UI isn't ambiguous.
        for (TableView<ScheduleEntry> other : Arrays.asList(
                mondayTable, tuesdayTable, wednesdayTable, thursdayTable, fridayTable)) {
            if (other != source) other.getSelectionModel().clearSelection();
        }

        selectedEntry = entry;

        // Light up the Action Console.
        actionConsole.setDisable(false);
        selectedClassLabel.setText(
                "Updating: " + safe(entry.getTeacherName())      // section
                        + " — " + safe(entry.getSubject())       // course
                        + "  •  " + safe(entry.getTime())
                        + "  •  Room " + safe(entry.getRoom())
                        + "  [" + safe(entry.getStatus()) + "]");
        consoleMessageLabel.setText("");
        reasonField.clear();
        statusCombo.getSelectionModel().clearSelection();

        // UC-02: also enable the Room Swap console and reset its feedback label
        if (roomSwapConsole != null) {
            roomSwapConsole.setDisable(false);
            swapStatusLabel.setText("");
        }
    }

    // ==================================================================
    //  FXML action handlers
    // ==================================================================

    /**
     * Fires when the Teacher clicks "Submit Update" in the Action Console.
     * Validates input, delegates to the backend on a background thread
     * (so the UI doesn't freeze), and refreshes the tables on completion.
     */
    @FXML
    public void onUpdateStatusClicked(ActionEvent event) {
        if (selectedEntry == null) {
            showError("No class selected.",
                    "Click a row in the schedule below before submitting.");
            return;
        }

        // ---- Guard: block invalid status transitions ----
        // CANCELLED is terminal — no changes allowed.
        // DELAYED can only escalate to CANCELLED (not back to DELAYED again).
        String currentStatus = selectedEntry.getStatus() == null
                ? "" : selectedEntry.getStatus().trim().toUpperCase();

        String newStatus = statusCombo.getValue();
        String reasonOrEta = reasonField.getText() == null ? "" : reasonField.getText().trim();

        if (newStatus == null || newStatus.isEmpty()) {
            showError("Pick a status.", "Choose DELAYED or CANCELLED from the dropdown.");
            return;
        }

        String newUpper = newStatus.trim().toUpperCase();

        if ("CANCELLED".equals(currentStatus)) {
            showError("Status is final",
                    "This class is already CANCELLED.\nNo further changes are allowed.");
            return;
        }
        if ("DELAYED".equals(currentStatus) && !"CANCELLED".equals(newUpper)) {
            showError("Only cancellation allowed",
                    "This class is already DELAYED.\nYou can only escalate it to CANCELLED.");
            return;
        }

        // Disable the console so the teacher can't double-click while the
        // background work is in progress.
        actionConsole.setDisable(true);
        consoleMessageLabel.setText("Submitting…");

        // Capture values for the background thread.
        final String sessionId = selectedEntry.getSessionId();
        final String subjectName = safe(selectedEntry.getSubject());
        final String finalStatus = newStatus;

        // ==============================================================
        //   GoF Observer Pattern Triggered:
        //   The backend NotificationService (invoked inside
        //   TeacherController.updateSessionStatus → reportDelay /
        //   reportCancellation) will now query the DB and ONLY notify
        //   enrolled students who have 'Smart Alerts' subscribed.
        //   This UI controller itself does ZERO observer work — it is
        //   only the event-routing layer.
        // ==============================================================

        // Run the heavy DB + notification work OFF the JavaFX thread so
        // the UI remains responsive (no freeze / spinning cursor).
        new Thread(() -> {
            try {
                teacherController.updateSessionStatus(sessionId, finalStatus, reasonOrEta);

                // Jump back to the FX thread for UI updates.
                Platform.runLater(() -> {
                    showInfo("Status update submitted",
                            "Class \"" + subjectName + "\" is now "
                                    + finalStatus + ".\nSubscribed students will receive an alert.");
                    reloadSchedule();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Update failed", ex.getMessage());
                    actionConsole.setDisable(false);
                });
            }
        }, "StatusUpdate-Worker").start();
    }

    // ==================================================================
    //  UC-02: Request Room Change
    // ==================================================================

    /**
     * Fires when the Teacher clicks "Submit Swap Request".
     *
     * Sequence (mirrors the UC-02 Sequence Diagram):
     *   1. Validate inputs (session selected, capacity numeric, reason non-empty).
     *   2. Delegate to RoomSwapController.requestRoomSwap(sessionId, reason, capacity).
     *      - Controller fetches session details (day/time).
     *      - Controller calls RoomRepository.checkRoomAvailability (Information Expert).
     *      - If no room → throws RoomUnavailableException → show Alert.
     *      - If room found → UPDATE teacher_assignments.sessionStatus = 'SWAP_PENDING'.
     *      - INSERT into schedule_adjustment_requests (Creator).
     *   3. Show success feedback inline + refresh schedule.
     */
    @FXML
    public void onRequestSwapClicked(ActionEvent event) {
        // ── 1. Validate: a session must be selected ──────────────────
        if (selectedEntry == null) {
            showError("No session selected",
                    "Click a row in the weekly schedule above to select a session first.");
            return;
        }

        // ── 2. Validate: capacity must be a positive integer ─────────
        String capText = swapCapacityField.getText() == null ? "" : swapCapacityField.getText().trim();
        if (capText.isEmpty()) {
            showError("Capacity required", "Enter the minimum required room capacity (e.g. 60).");
            return;
        }
        int capacity;
        try {
            capacity = Integer.parseInt(capText);
            if (capacity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Invalid capacity", "Capacity must be a positive whole number (e.g. 60).");
            return;
        }

        // ── 3. Validate: reason must not be blank ────────────────────
        String reason = swapReasonField.getText() == null ? "" : swapReasonField.getText().trim();
        if (reason.isEmpty()) {
            showError("Reason required", "Please enter a reason for the room change request.");
            return;
        }

        // ── 4. Delegate to RoomSwapController ────────────────────────
        String sessionId = selectedEntry.getSessionId();
        swapStatusLabel.setText("Checking availability…");
        swapStatusLabel.setStyle("-fx-text-fill: #475569;");
        roomSwapConsole.setDisable(true);

        new Thread(() -> {
            try {
                String requestId = roomSwapCtrl.requestRoomSwap(sessionId, reason, capacity);

                Platform.runLater(() -> {
                    swapStatusLabel.setText("✓ Swap request " + requestId + " submitted — session marked SWAP_PENDING.");
                    swapStatusLabel.setStyle("-fx-text-fill: #047857; -fx-font-weight: bold;");
                    swapCapacityField.clear();
                    swapReasonField.clear();
                    roomSwapConsole.setDisable(false);
                    reloadSchedule(); // refresh tables to show SWAP_PENDING status
                    showInfo("Room Swap Request Submitted ✓",
                            "Your room change request has been recorded.\n\n"
                          + "Request ID : " + requestId + "\n"
                          + "Session    : " + safe(selectedEntry.getSubject())
                                           + " — " + safe(selectedEntry.getTime()) + "\n"
                          + "Capacity   : ≥ " + capacity + " seats\n"
                          + "Status     : SWAP_PENDING\n\n"
                          + "The Admin will review and assign a suitable room.");
                });

            } catch (RoomSwapController.RoomUnavailableException ex) {
                Platform.runLater(() -> {
                    swapStatusLabel.setText("✗ " + ex.getMessage());
                    swapStatusLabel.setStyle("-fx-text-fill: #DC2626;");
                    roomSwapConsole.setDisable(false);
                    showError("No Rooms Available", ex.getMessage());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    swapStatusLabel.setText("✗ Error: " + ex.getMessage());
                    swapStatusLabel.setStyle("-fx-text-fill: #DC2626;");
                    roomSwapConsole.setDisable(false);
                    showError("Swap Request Failed", ex.getMessage());
                });
            }
        }, "RoomSwap-Worker").start();
    }

    /**
     * Resets the Action Console and clears any row selection.
     */
    @FXML
    public void onClearSelectionClicked(ActionEvent event) {
        clearSelection();
    }

    @FXML
    public void onRefreshClicked(ActionEvent event) {
        reloadSchedule();
    }

    @FXML
    public void onNavMyScheduleClicked(ActionEvent event) {
        // Tab 0 = My Schedule
        if (mainTabPane != null) mainTabPane.getSelectionModel().select(0);
        selectTodayTab();
        updateNavHighlight(0);
    }

    @FXML
    public void onNavSubstituteClicked(ActionEvent event) {
        // Tab 1 = Request Substitute
        if (mainTabPane != null) mainTabPane.getSelectionModel().select(1);
        updateNavHighlight(1);
    }

    @FXML
    public void onNavRoomSwapClicked(ActionEvent event) {
        // Tab 2 = Substitute Inbox
        if (mainTabPane != null) mainTabPane.getSelectionModel().select(2);
        loadSubstituteInbox(); // refresh inbox when navigating to it
        updateNavHighlight(2);
    }

    @FXML
    public void onLogoutClicked(ActionEvent event) {
        Alert confirm = new Alert(AlertType.CONFIRMATION,
                "Log out of SAPCIS?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(choice -> {
            if (choice == ButtonType.OK) {
                UserSession.getInstance().clear();
                navigateToLogin();
            }
        });
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    /** Highlights the active nav button and resets the others. */
    private void updateNavHighlight(int activeTab) {
        String active   = "-fx-background-color: #FFFFFF; -fx-text-fill: #1A365D; "
                        + "-fx-font-weight: bold; -fx-font-size: 13px; "
                        + "-fx-background-radius: 8; -fx-padding: 10 14 10 14; -fx-cursor: hand;";
        String inactive = "-fx-background-color: transparent; -fx-text-fill: #FFFFFF; "
                        + "-fx-font-size: 13px; -fx-background-radius: 8; "
                        + "-fx-border-color: rgba(255,255,255,0.35); -fx-border-radius: 8; "
                        + "-fx-padding: 10 14 10 14; -fx-cursor: hand;";
        if (navMyScheduleButton  != null) navMyScheduleButton.setStyle(activeTab == 0 ? active : inactive);
        if (navSubstituteButton  != null) navSubstituteButton.setStyle(activeTab == 1 ? active : inactive);
        if (navRoomSwapButton    != null) navRoomSwapButton.setStyle(activeTab == 2 ? active : inactive);
    }

    private void clearSelection() {
        selectedEntry = null;
        actionConsole.setDisable(true);
        selectedClassLabel.setText("— Click a row in the schedule below to begin —");
        consoleMessageLabel.setText("");
        reasonField.clear();
        statusCombo.getSelectionModel().clearSelection();
        for (TableView<ScheduleEntry> t : Arrays.asList(
                mondayTable, tuesdayTable, wednesdayTable, thursdayTable, fridayTable)) {
            t.getSelectionModel().clearSelection();
        }
    }

    private ObservableList<ScheduleEntry> filterByDay(List<ScheduleEntry> src, String day) {
        ObservableList<ScheduleEntry> out = FXCollections.observableArrayList();
        if (src == null || day == null) return out;
        String target = day.trim().toLowerCase().substring(0, 3);
        for (ScheduleEntry e : src) {
            if (e.getDay() != null && e.getDay().trim().toLowerCase().startsWith(target)) {
                out.add(e);
            }
        }
        return out;
    }

    private void selectTodayTab() {
        if (weekTabPane == null) return;
        int idx;
        switch (LocalDate.now().getDayOfWeek()) {
            case MONDAY:    idx = 0; break;
            case TUESDAY:   idx = 1; break;
            case WEDNESDAY: idx = 2; break;
            case THURSDAY:  idx = 3; break;
            case FRIDAY:    idx = 4; break;
            default:        idx = 0; break;
        }
        weekTabPane.getSelectionModel().select(idx);
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SAPCIS – Login");
            stage.centerOnScreen();
        } catch (IOException ex) {
            showError("Navigation failed", "Could not load Login.fxml: " + ex.getMessage());
        }
    }

    private static String safe(String s) {
        return s == null ? "—" : s;
    }

    private void showInfo(String header, String body) {
        Alert a = new Alert(AlertType.INFORMATION, body, ButtonType.OK);
        a.setHeaderText(header);
        a.setTitle("SAPCIS");
        a.getDialogPane().setStyle("-fx-font-size: 13px;");
        a.showAndWait();
    }

    private void showError(String header, String body) {
        Alert a = new Alert(AlertType.ERROR, body, ButtonType.OK);
        a.setHeaderText(header);
        a.setTitle("SAPCIS");
        a.getDialogPane().setStyle("-fx-font-size: 13px;");
        a.showAndWait();
    }
}
