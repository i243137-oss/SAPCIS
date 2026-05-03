package ui;

import controller.DashboardController;
import db.DBConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.ScheduleEntry;
import model.User;
import utils.UserSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * =============================================================================
 *  StudentDashboardController  (UC-04)
 * -----------------------------------------------------------------------------
 *  Backs {@code ui/StudentDashboard.fxml}. Responsible for:
 *      • Pulling the currently-logged-in user from {@link UserSession}.
 *      • Calling the backend {@link DashboardController} to fetch the full
 *        week schedule and today's live-status rows.
 *      • Rendering the Critical Alerts banner, 4 "Today" sub-tabs, and 5
 *        day-of-week TableViews.
 *
 *  ARCHITECTURE:
 *  All SQL lives in the controller/ and dao/ packages. This class only
 *  orchestrates the UI, so it stays squarely in the "V/Controller" layer.
 * =============================================================================
 */
public class StudentDashboardController {

    // ------------------------------------------------------------------
    //  Header / alerts
    // ------------------------------------------------------------------
    @FXML private VBox   alertsBanner;
    @FXML private VBox   alertsContainer;
    @FXML private Label  todayLabel;
    @FXML private Button logoutButton;
    @FXML private Button refreshButton;

    // ------------------------------------------------------------------
    //  Sidebar
    // ------------------------------------------------------------------
    @FXML private Label    profileNameLabel;
    @FXML private Label    profileRollNoLabel;
    @FXML private Label    profileDeptLabel;
    @FXML private Label    profileSectionLabel;
    @FXML private CheckBox smartAlertsCheckBox;

    @FXML private Label statTotalLabel;
    @FXML private Label statOngoingLabel;
    @FXML private Label statDelayedLabel;
    @FXML private Label statCancelledLabel;

    // ------------------------------------------------------------------
    //  Main TabPane + inner tables
    // ------------------------------------------------------------------
    @FXML private TabPane mainTabPane;
    @FXML private TabPane todayTabPane;

    // "Today's Live Status" sub-tab tables
    @FXML private TableView<ScheduleEntry> ongoingTable;
    @FXML private TableView<ScheduleEntry> upcomingTable;
    @FXML private TableView<ScheduleEntry> delayedTable;
    @FXML private TableView<ScheduleEntry> cancelledTable;

    // Day-of-week tables
    @FXML private TableView<ScheduleEntry> mondayTable;
    @FXML private TableView<ScheduleEntry> tuesdayTable;
    @FXML private TableView<ScheduleEntry> wednesdayTable;
    @FXML private TableView<ScheduleEntry> thursdayTable;
    @FXML private TableView<ScheduleEntry> fridayTable;

    // ------------------------------------------------------------------
    //  Backend controller — ALL JDBC lives there.
    // ------------------------------------------------------------------
    private final DashboardController dashboardController = new DashboardController();

    // Cached logged-in student identity.
    private String currentUid;
    private String currentName;

    // ==================================================================
    //  Initialization
    // ==================================================================
    @FXML
    public void initialize() {
        // 1. Header date label.
        todayLabel.setText(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH)));

        // 2. Programmatically configure every TableView's columns + placeholder.
        //    Done in Java (not FXML) so we never trip over PropertyValueFactory
        //    generics issues in the FXML parser.
        configureScheduleTable(ongoingTable,   "No classes are ongoing right now.");
        configureScheduleTable(upcomingTable,  "No more classes scheduled for today.");
        configureScheduleTable(delayedTable,   "No delayed classes today.");
        configureScheduleTable(cancelledTable, "No cancellations today — enjoy!");
        configureScheduleTable(mondayTable,    "No classes scheduled on Monday.");
        configureScheduleTable(tuesdayTable,   "No classes scheduled on Tuesday.");
        configureScheduleTable(wednesdayTable, "No classes scheduled on Wednesday.");
        configureScheduleTable(thursdayTable,  "No classes scheduled on Thursday.");
        configureScheduleTable(fridayTable,    "No classes scheduled on Friday.");

        // 3. Pull the logged-in user from the global session.
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) {
            // Defensive: if someone opened this FXML without a session,
            // bail out to the login screen on the next tick.
            Platform.runLater(this::backToLogin);
            return;
        }
        this.currentUid  = user.getUid();
        this.currentName = user.getName();
        profileNameLabel.setText(currentName == null ? "—" : currentName);

        // 4. Kick off the data-load pipeline on a worker thread so the UI
        //    thread stays responsive.
        loadDashboardData();
    }

    /**
     * Builds the four standard columns (Subject, Teacher, Room, Time) on a
     * TableView and installs a styled empty-state placeholder.
     */
    private void configureScheduleTable(TableView<ScheduleEntry> table, String emptyMessage) {
        if (table == null) return;

        // Avoid re-adding columns if the same FXML loader re-invokes initialize.
        if (!table.getColumns().isEmpty()) return;

        TableColumn<ScheduleEntry, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setPrefWidth(240);
        subjectCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getSubject()));

        TableColumn<ScheduleEntry, String> teacherCol = new TableColumn<>("Teacher");
        teacherCol.setPrefWidth(200);
        teacherCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getTeacherName()));

        TableColumn<ScheduleEntry, String> roomCol = new TableColumn<>("Room");
        roomCol.setPrefWidth(150);
        roomCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getRoom()));

        TableColumn<ScheduleEntry, String> timeCol = new TableColumn<>("Time");
        timeCol.setPrefWidth(160);
        timeCol.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getTime()));

        table.getColumns().addAll(subjectCol, teacherCol, roomCol, timeCol);

        Label placeholder = new Label(emptyMessage);
        placeholder.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
        table.setPlaceholder(placeholder);
    }

    /**
     * Fetches student profile (roll no, dept, section) + full week schedule
     * in the background, then hands everything back to the UI thread.
     */
    private void loadDashboardData() {
        final String uid = this.currentUid;
        if (uid == null) return;

        new Thread(() -> {
            // ---- profile extras (roll no / dept / section) ----
            String[] profile = loadStudentProfile(uid);

            // ---- schedule data via backend ----
            List<ScheduleEntry> fullWeek     = dashboardController.getFullSchedule(uid);
            List<ScheduleEntry> ongoing      = dashboardController.getTodaysSessionsByStatus(uid, DashboardController.ST_ONGOING);
            List<ScheduleEntry> upcoming     = dashboardController.getTodaysSessionsByStatus(uid, DashboardController.ST_UPCOMING);
            List<ScheduleEntry> delayed      = dashboardController.getTodaysSessionsByStatus(uid, DashboardController.ST_DELAYED);
            List<ScheduleEntry> cancelled    = dashboardController.getTodaysSessionsByStatus(uid, DashboardController.ST_CANCELLED);
            List<ScheduleEntry> critical     = dashboardController.getCriticalAlertsForToday(uid);

            Platform.runLater(() -> {
                // --- sidebar profile ---
                profileRollNoLabel.setText("Roll No: " + safe(profile[0]));
                profileDeptLabel.setText("Department: " + safe(profile[1]));
                profileSectionLabel.setText("Section: " + safe(profile[2]));

                // --- Today's Live Status sub-tabs ---
                setTableData(ongoingTable,   ongoing);
                setTableData(upcomingTable,  upcoming);
                setTableData(delayedTable,   delayed);
                setTableData(cancelledTable, cancelled);

                // --- 5 weekday tables ---
                setTableData(mondayTable,    filterByDay(fullWeek, "Monday"));
                setTableData(tuesdayTable,   filterByDay(fullWeek, "Tuesday"));
                setTableData(wednesdayTable, filterByDay(fullWeek, "Wednesday"));
                setTableData(thursdayTable,  filterByDay(fullWeek, "Thursday"));
                setTableData(fridayTable,    filterByDay(fullWeek, "Friday"));

                // --- Critical Alerts banner ---
                renderAlertsBanner(critical);

                // --- Stats card ---
                int totalToday = ongoing.size() + upcoming.size() + delayed.size() + cancelled.size();
                statTotalLabel.setText("Total classes: "   + totalToday);
                statOngoingLabel.setText("Ongoing: "       + ongoing.size());
                statDelayedLabel.setText("Delayed: "       + delayed.size());
                statCancelledLabel.setText("Cancelled: "   + cancelled.size());
            });
        }, "sapcis-dashboard-loader").start();
    }

    /**
     * Reads the {@code students} table for roll no / dept / section.
     * NOTE: this is a tiny read-only JDBC call for rendering the sidebar;
     * because there is no pre-existing DAO method for this single-row fetch,
     * it is inlined here for brevity rather than polluting DashboardController.
     * All write/transaction logic continues to live in the controller/dao layers.
     */
    private String[] loadStudentProfile(String uid) {
        String[] profile = { "", "", "" }; // rollNo, dept, section
        String sql = "SELECT rollNo, dept, section FROM students WHERE uid = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    profile[0] = rs.getString("rollNo");
                    profile[1] = rs.getString("dept");
                    profile[2] = rs.getString("section");
                }
            }
        } catch (SQLException e) {
            System.err.println("loadStudentProfile failed: " + e.getMessage());
        }
        return profile;
    }

    // ==================================================================
    //  Event handlers
    // ==================================================================

    @FXML
    private void onRefreshClicked(ActionEvent event) {
        loadDashboardData();
    }

    @FXML
    private void onSmartAlertsToggled(ActionEvent event) {
        // UC-06 integration point. Delegate to NotificationController when ready.
        boolean subscribed = smartAlertsCheckBox.isSelected();
        System.out.println("[Smart Alerts] "
                + (subscribed ? "Subscribed" : "Unsubscribed")
                + " for uid=" + currentUid);
    }

    @FXML
    private void onLogoutClicked(ActionEvent event) {
        UserSession.getInstance().clear();
        backToLogin();
    }

    // ==================================================================
    //  UI helpers
    // ==================================================================

    private void setTableData(TableView<ScheduleEntry> table, List<ScheduleEntry> rows) {
        if (table == null) return;
        ObservableList<ScheduleEntry> data = FXCollections.observableArrayList(rows);
        table.setItems(data);
    }

    private List<ScheduleEntry> filterByDay(List<ScheduleEntry> all, String day) {
        ObservableList<ScheduleEntry> out = FXCollections.observableArrayList();
        String target = day.substring(0, 3).toLowerCase();
        for (ScheduleEntry e : all) {
            String d = e.getDay() == null ? "" : e.getDay().toLowerCase();
            if (d.startsWith(target)) out.add(e);
        }
        return out;
    }

    /**
     * Renders each critical alert (CANCELLED = red, DELAYED = orange) as a
     * coloured banner row inside {@code alertsContainer}. The whole banner
     * is hidden when there are no alerts.
     */
    private void renderAlertsBanner(List<ScheduleEntry> alerts) {
        alertsContainer.getChildren().clear();

        if (alerts == null || alerts.isEmpty()) {
            alertsContainer.setVisible(false);
            alertsContainer.setManaged(false);
            return;
        }
        alertsContainer.setVisible(true);
        alertsContainer.setManaged(true);

        for (ScheduleEntry e : alerts) {
            String st = e.getStatus() == null ? "" : e.getStatus().toUpperCase();

            String bgColor, accentColor, textColor, icon;
            if ("CANCELLED".equals(st)) {
                bgColor     = "#FEE2E2";
                accentColor = "#B91C1C";
                textColor   = "#7F1D1D";
                icon        = "✕";
            } else if ("DELAYED".equals(st)) {
                bgColor     = "#FFEDD5";
                accentColor = "#C2410C";
                textColor   = "#7C2D12";
                icon        = "⏱";
            } else {
                continue; // ignore anything that isn't a "critical" status
            }

            HBox row = new HBox(12);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle(
                    "-fx-background-color: " + bgColor + ";"
                  + "-fx-background-radius: 8;"
                  + "-fx-border-color: " + accentColor + ";"
                  + "-fx-border-radius: 8;"
                  + "-fx-border-width: 0 0 0 4;"
            );

            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");

            Label subjectLabel = new Label(safe(e.getSubject()));
            subjectLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

            Label statusLabel = new Label(st);
            statusLabel.setStyle(
                    "-fx-font-size: 11px; -fx-font-weight: bold;"
                  + "-fx-text-fill: #FFFFFF;"
                  + "-fx-background-color: " + accentColor + ";"
                  + "-fx-background-radius: 10;"
                  + "-fx-padding: 2 10 2 10;"
            );

            Label detailLabel = new Label(
                    "Room " + safe(e.getRoom()) + " · " + safe(e.getTime()));
            detailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + textColor + ";");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(iconLabel, subjectLabel, statusLabel, spacer, detailLabel);
            alertsContainer.getChildren().add(row);
        }
    }

    private void backToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
            Stage stage = (Stage) profileNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("SAPCIS - Login");
        } catch (IOException e) {
            System.err.println("Could not load Login.fxml: " + e.getMessage());
        }
    }

    private String safe(String s) {
        return s == null || s.trim().isEmpty() ? "—" : s.trim();
    }

    /** Convenience no-op for the linter — advertises the weekday tabs we serve. */
    @SuppressWarnings("unused")
    private List<String> supportedDays() {
        return Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");
    }
}
