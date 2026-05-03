package ui;

import db.DBConnection;
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
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.User;
import controller.OverrideController;
import controller.SubstituteController;
import dao.SessionRepository;
import service.ConstraintResolverService;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import utils.UserSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * =============================================================================
 *  AdminDashboardController
 * -----------------------------------------------------------------------------
 *  Backs {@code ui/AdminDashboard.fxml}.
 *
 *  Tab 1: Campus Core Setup (Dept, Batch, Course, Room, Section, Teacher)
 *  Tab 2: Campus Policies & Rules (UC-10)
 *  Tab 3: Teacher Timetable Assignment + Validation Engine
 * =============================================================================
 */
public class AdminDashboardController {

    // ------------------------------------------------------------------
    //  Sidebar
    // ------------------------------------------------------------------
    @FXML private Label  profileNameLabel;
    @FXML private Label  profileUidLabel;
    @FXML private Label  profileRoleLabel;
    @FXML private Label  todayLabel;
    @FXML private Button logoutButton;
    @FXML private TabPane adminTabPane;

    // ------------------------------------------------------------------
    //  Tab 1 — Campus Core Setup
    // ------------------------------------------------------------------
    @FXML private TextField deptIdField;
    @FXML private TextField deptNameField;
    @FXML private TextField batchIdField;
    @FXML private TextField batchYearField;
    @FXML private TextField courseCodeField;
    @FXML private TextField courseNameField;
    @FXML private TextField courseCreditsField;
    @FXML private TextField roomIdField;
    @FXML private TextField roomNameField;
    @FXML private TextField roomCapacityField;

    // Add Section card (Tab 1)
    @FXML private ComboBox<String> secBatchCombo;
    @FXML private ComboBox<String> secDeptCombo;
    @FXML private TextField        secNameField;

    // Tab 1 entity selector + form VBoxes (only one visible at a time)
    @FXML private ComboBox<String> setupEntityCombo;
    @FXML private VBox deptForm;
    @FXML private VBox batchForm;
    @FXML private VBox courseForm;
    @FXML private VBox roomForm;
    @FXML private VBox sectionForm;
    @FXML private VBox teacherForm;
    @FXML private TextField teacherNameField;
    @FXML private TextField teacherEmailField;
    @FXML private TextField teacherPasswordField;
    @FXML private Label teacherFormMessageLabel;

    // ------------------------------------------------------------------
    //  Tab 2 — Rules (UC-10)
    // ------------------------------------------------------------------
    @FXML private ComboBox<String> ruleTypeCombo;
    @FXML private VBox maxCoursesForm;
    @FXML private VBox maxClassesForm;
    @FXML private VBox maxDurationForm;
    @FXML private VBox restGapForm;
    @FXML private VBox roomRuleForm;
    @FXML private VBox openingTimeForm;
    @FXML private VBox closingTimeForm;
    @FXML private VBox removedClassesForm;
    @FXML private TextField openingTimeField;
    @FXML private TextField closingTimeField;
    @FXML private TableView<Map<String, String>> removedClassesTable;
    @FXML private TableColumn<Map<String, String>, String> remColTeacher;
    @FXML private TableColumn<Map<String, String>, String> remColCourse;
    @FXML private TableColumn<Map<String, String>, String> remColDay;
    @FXML private TableColumn<Map<String, String>, String> remColTime;
    @FXML private TableColumn<Map<String, String>, String> remColRoom;
    @FXML private TableColumn<Map<String, String>, String> remColReason;
    
    // Emergency Override - Day/Time change fields
    @FXML private ComboBox<String> overrideNewDayCombo;
    @FXML private TextField overrideNewStartField;
    @FXML private TextField overrideNewEndField;
    
    // List to track removed classes for reassignment
    private ObservableList<Map<String, String>> removedClassesList = FXCollections.observableArrayList();

    @FXML private TextField         maxCoursesField;
    @FXML private TextField         maxClassesPerDayField;
    @FXML private TextField         maxDurationField;
    @FXML private TextField         restGapField;
    @FXML private ComboBox<String>  ruleRoomCombo;
    @FXML private TextField         roomCapOverrideField;

    // ------------------------------------------------------------------
    //  Tab 3 — Teacher Timetable Assignment
    // ------------------------------------------------------------------
    @FXML private ComboBox<String> assignTeacherCombo;
    @FXML private ComboBox<String> assignDeptCombo;
    @FXML private ComboBox<String> assignBatchCombo;
    @FXML private ComboBox<String> assignSectionCombo;
    @FXML private ComboBox<String> assignCourseCombo;
    @FXML private ComboBox<String> assignDayCombo;
    @FXML private TextField        assignStartTimeField;
    @FXML private TextField        assignEndTimeField;
    @FXML private ComboBox<String> assignRoomCombo;

    // ------------------------------------------------------------------
    //  Tab 4 — Analytics & Reports (UC-12)
    // ------------------------------------------------------------------
    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private HBox             dynamicInputArea;
    @FXML private VBox             dateRangeInputs;
    @FXML private VBox             teacherSelectInput;
    @FXML private VBox             roomSelectInput;
    @FXML private DatePicker       reportStartDate;
    @FXML private DatePicker       reportEndDate;
    @FXML private ComboBox<String> reportTeacherCombo;
    @FXML private ComboBox<String> reportRoomCombo;
    @FXML private VBox             reportDisplayArea;

    // ------------------------------------------------------------------
    //  Tab 5 — Emergency Override (UC-11)
    // ------------------------------------------------------------------
    @FXML private ComboBox<String>                         overrideTeacherCombo;
    @FXML private ComboBox<String>                         overrideDayCombo;
    @FXML private TableView<Map<String, String>>           overrideSessionTable;
    @FXML private TableColumn<Map<String, String>, String> colAssignId;
    @FXML private TableColumn<Map<String, String>, String> colCourse;
    @FXML private TableColumn<Map<String, String>, String> colRoom;
    @FXML private TableColumn<Map<String, String>, String> colStart;
    @FXML private TableColumn<Map<String, String>, String> colEnd;
    @FXML private TableColumn<Map<String, String>, String> colSection;
    @FXML private TableColumn<Map<String, String>, String> colStatus;
    @FXML private GridPane                                 overrideActionPane;
    @FXML private ComboBox<String>                         overrideStatusCombo;
    @FXML private TextField                                overrideReasonField;
    @FXML private ComboBox<String>                         swapRoomCombo;

    // ------------------------------------------------------------------
    //  Tab 6 — Substitute Management (UC-08)
    // ------------------------------------------------------------------
    @FXML private TableView<Map<String, String>>           subNeededTable;
    @FXML private TableColumn<Map<String, String>, String> subColId;
    @FXML private TableColumn<Map<String, String>, String> subColTeacher;
    @FXML private TableColumn<Map<String, String>, String> subColCourse;
    @FXML private TableColumn<Map<String, String>, String> subColDay;
    @FXML private TableColumn<Map<String, String>, String> subColTime;
    @FXML private TableColumn<Map<String, String>, String> subColStatus;
    @FXML private Label                                    subSelectedLabel;
    @FXML private ComboBox<String>                         subAvailableTeachersCombo;
    @FXML private TextField                                subReasonField;
    @FXML private TableView<Map<String, String>>           subHistoryTable;
    @FXML private TableColumn<Map<String, String>, String> subHColId;
    @FXML private TableColumn<Map<String, String>, String> subHColOrig;
    @FXML private TableColumn<Map<String, String>, String> subHColSub;
    @FXML private TableColumn<Map<String, String>, String> subHColStatus;

    // ------------------------------------------------------------------
    //  Tab 7 — Room Swap Requests (UC-02 Admin side)
    // ------------------------------------------------------------------
    @FXML private TableView<Map<String, String>>           swapRequestsTable;
    @FXML private TableColumn<Map<String, String>, String> swapColId;
    @FXML private TableColumn<Map<String, String>, String> swapColSession;
    @FXML private TableColumn<Map<String, String>, String> swapColCourse;
    @FXML private TableColumn<Map<String, String>, String> swapColDay;
    @FXML private TableColumn<Map<String, String>, String> swapColCapacity;
    @FXML private TableColumn<Map<String, String>, String> swapColReason;
    @FXML private TableColumn<Map<String, String>, String> swapColStatus;
    @FXML private ComboBox<String>                         swapRoomPickerCombo;
    @FXML private Label                                    swapRequestStatusLabel;

    private final SessionRepository    sessionRepo    = new SessionRepository();
    private final OverrideController   overrideCtrl   = new OverrideController();
    private final SubstituteController substituteCtrl = new SubstituteController();

    // ==================================================================
    //  Lifecycle
    // ==================================================================

    @FXML
    public void initialize() {
        User admin = UserSession.getInstance().getCurrentUser();
        if (admin != null) {
            profileNameLabel.setText(admin.getName() == null ? "Admin" : admin.getName());
            profileUidLabel.setText("ID: " + (admin.getUid() == null ? "—" : admin.getUid()));
            profileRoleLabel.setText("Role: " + (admin.getRole() == null ? "Admin" : admin.getRole()));
        }
        todayLabel.setText(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH)));

        assignDayCombo.setItems(FXCollections.observableArrayList(
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"));

        loadTeachers();
        loadCourses();
        loadAllRoomsForRuleTab();

        ObservableList<String> allBatches = loadBatchItems();
        assignBatchCombo.setItems(allBatches);
        secBatchCombo.setItems(FXCollections.observableArrayList(allBatches));

        ObservableList<String> allDepts = loadDeptItems();
        secDeptCombo.setItems(allDepts);

        assignDeptCombo.setItems(FXCollections.observableArrayList());
        assignDeptCombo.setPromptText("Select Batch first");
        assignDeptCombo.setDisable(true);

        assignSectionCombo.setItems(FXCollections.observableArrayList());
        assignSectionCombo.setPromptText("Select Dept first");
        assignSectionCombo.setDisable(true);

        assignRoomCombo.setItems(FXCollections.observableArrayList());
        assignRoomCombo.setPromptText("Select Day + Times first");
        assignRoomCombo.setDisable(true);

        wireBatchDeptSectionCascade();
        wireRoomAvailabilityListeners();

        prefillRuleField(maxCoursesField,      "MAX_COURSES");
        prefillRuleField(maxClassesPerDayField, "MAX_CLASSES_PER_DAY");
        prefillRuleField(maxDurationField,      "MAX_DURATION");
        prefillRuleField(restGapField,          "REST_GAP");

        // Tab 1: entity selector
        setupEntityCombo.setItems(FXCollections.observableArrayList(
                "Department", "Batch", "Course", "Room", "Section", "Teacher"));
        VBox[] setupForms = { deptForm, batchForm, courseForm, roomForm, sectionForm, teacherForm };
        setupEntityCombo.valueProperty().addListener((obs, o, n) -> {
            for (VBox f : setupForms) { f.setVisible(false); f.setManaged(false); }
            if (n == null) return;
            switch (n) {
                case "Department": deptForm.setVisible(true);    deptForm.setManaged(true);    break;
                case "Batch":      batchForm.setVisible(true);   batchForm.setManaged(true);   break;
                case "Course":     courseForm.setVisible(true);  courseForm.setManaged(true);  break;
                case "Room":       roomForm.setVisible(true);    roomForm.setManaged(true);    break;
                case "Section":    sectionForm.setVisible(true); sectionForm.setManaged(true); break;
                case "Teacher":    teacherForm.setVisible(true); teacherForm.setManaged(true); break;
            }
        });

        // Tab 2: rule type selector
        ruleTypeCombo.setItems(FXCollections.observableArrayList(
                "Max Courses per Teacher",
                "Max Classes per Day per Teacher",
                "Max Class Duration (minutes)",
                "Min Rest Gap (minutes)",
                "Room Capacity Override",
                "University Opening Time (HH:mm)",
                "University Closing Time (HH:mm)"));
        VBox[] ruleForms = { maxCoursesForm, maxClassesForm, maxDurationForm, restGapForm,
                             roomRuleForm, openingTimeForm, closingTimeForm };
        ruleTypeCombo.valueProperty().addListener((obs, o, n) -> {
            for (VBox f : ruleForms) { f.setVisible(false); f.setManaged(false); }
            if (n == null) return;
            switch (n) {
                case "Max Courses per Teacher":           maxCoursesForm.setVisible(true);   maxCoursesForm.setManaged(true);   break;
                case "Max Classes per Day per Teacher":   maxClassesForm.setVisible(true);   maxClassesForm.setManaged(true);   break;
                case "Max Class Duration (minutes)":      maxDurationForm.setVisible(true);  maxDurationForm.setManaged(true);  break;
                case "Min Rest Gap (minutes)":            restGapForm.setVisible(true);      restGapForm.setManaged(true);      break;
                case "Room Capacity Override":            roomRuleForm.setVisible(true);     roomRuleForm.setManaged(true);     break;
                case "University Opening Time (HH:mm)":  openingTimeForm.setVisible(true);  openingTimeForm.setManaged(true);  break;
                case "University Closing Time (HH:mm)":  closingTimeForm.setVisible(true);  closingTimeForm.setManaged(true);  break;
            }
        });
        // Pre-fill opening/closing time fields from DB
        prefillRuleField(openingTimeField, "UNI_OPENING_TIME");
        prefillRuleField(closingTimeField,  "UNI_CLOSING_TIME");

        initReportsTab();
        initOverrideTab();
        initSubstituteTab();
        initSwapRequestsTab();
        initRemovedClassesTable();
    }

    /** Initialize the removed classes table columns for rule conflict display */
    private void initRemovedClassesTable() {
        if (remColTeacher != null) {
            remColTeacher.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("teacherName", "")));
            remColCourse.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("courseCode", "") + " " + cd.getValue().getOrDefault("courseName", "")));
            remColDay.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("dayOfWeek", "")));
            remColTime.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("startTime", "") + " – " + cd.getValue().getOrDefault("endTime", "")));
            remColRoom.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("roomId", "")));
            remColReason.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("reason", "")));
            removedClassesTable.setItems(removedClassesList);
        }
    }

    // ==================================================================
    //  Cascading: Batch → Dept → Section (Tab 3)
    // ==================================================================

    private void wireBatchDeptSectionCascade() {
        assignBatchCombo.valueProperty().addListener((obs, o, n) -> {
            assignDeptCombo.getSelectionModel().clearSelection();
            assignSectionCombo.getSelectionModel().clearSelection();
            assignSectionCombo.getItems().clear();
            assignSectionCombo.setDisable(true);
            if (n == null || n.isEmpty()) {
                assignDeptCombo.getItems().clear();
                assignDeptCombo.setDisable(true);
                assignDeptCombo.setPromptText("Select Batch first");
                return;
            }
            loadDeptsForBatch(extractId(n));
        });

        assignDeptCombo.valueProperty().addListener((obs, o, n) -> {
            assignSectionCombo.getSelectionModel().clearSelection();
            assignSectionCombo.getItems().clear();
            String batchSel = assignBatchCombo.getValue();
            if (n == null || n.isEmpty() || batchSel == null) {
                assignSectionCombo.setDisable(true);
                assignSectionCombo.setPromptText("Select Dept first");
                return;
            }
            loadSectionsForBatchDept(extractId(batchSel), extractId(n));
        });
    }

    private void loadDeptsForBatch(String batchId) {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT d.deptId, d.deptName FROM departments d ORDER BY d.deptName");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) items.add(rs.getString("deptId") + " — " + rs.getString("deptName"));
        } catch (SQLException e) { e.printStackTrace(); }
        assignDeptCombo.setItems(items);
        assignDeptCombo.setDisable(items.isEmpty());
        assignDeptCombo.setPromptText(items.isEmpty() ? "No departments" : "Select Dept");
    }

    private void loadSectionsForBatchDept(String batchId, String deptId) {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT sectionName FROM batch_dept_sections WHERE batchId=? AND deptId=? ORDER BY sectionName")) {
            s.setString(1, batchId); s.setString(2, deptId);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) items.add(rs.getString("sectionName"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        assignSectionCombo.setItems(items);
        assignSectionCombo.setDisable(items.isEmpty());
        assignSectionCombo.setPromptText(items.isEmpty() ? "No sections (add in Tab 1)" : "Select Section");
    }

    // ==================================================================
    //  Dynamic Room Filtering
    // ==================================================================

    private void wireRoomAvailabilityListeners() {
        assignDayCombo.valueProperty().addListener((obs, o, n) -> tryLoadAvailableRooms());
        ChangeListener<String> tl = (obs, o, n) -> tryLoadAvailableRooms();
        assignStartTimeField.textProperty().addListener(tl);
        assignEndTimeField.textProperty().addListener(tl);
    }

    private void tryLoadAvailableRooms() {
        String day = assignDayCombo.getValue();
        String startStr = trim(assignStartTimeField), endStr = trim(assignEndTimeField);
        if (day == null || day.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
            assignRoomCombo.getItems().clear(); assignRoomCombo.setPromptText("Select Day + Times first"); assignRoomCombo.setDisable(true); return;
        }
        LocalTime start, end;
        try {
            start = LocalTime.parse(startStr, DateTimeFormatter.ofPattern("H:mm"));
            end   = LocalTime.parse(endStr,   DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception ex) {
            assignRoomCombo.getItems().clear(); assignRoomCombo.setPromptText("Enter valid HH:mm"); assignRoomCombo.setDisable(true); return;
        }
        if (!end.isAfter(start)) {
            assignRoomCombo.getItems().clear(); assignRoomCombo.setPromptText("End > Start"); assignRoomCombo.setDisable(true); return;
        }
        loadAvailableRooms(day, start, end);
    }

    private void loadAvailableRooms(String day, LocalTime start, LocalTime end) {
        ObservableList<String> items = FXCollections.observableArrayList();
        String sql = "SELECT cr.roomId, cr.roomName FROM classrooms cr "
                   + "WHERE cr.roomId NOT IN (SELECT ta.roomId FROM teacher_assignments ta "
                   + "WHERE ta.dayOfWeek=? AND ta.startTime<? AND ta.endTime>?) ORDER BY cr.roomId";
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, day); s.setString(2, end.toString()); s.setString(3, start.toString());
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) items.add(rs.getString("roomId") + " — " + rs.getString("roomName"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        assignRoomCombo.setItems(items);
        assignRoomCombo.setDisable(items.isEmpty());
        assignRoomCombo.setPromptText(items.isEmpty() ? "No rooms available" : "Select Room (" + items.size() + " free)");
    }

    // ==================================================================
    //  Non-cascading combo loaders
    // ==================================================================

    private void loadTeachers() {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT uid, name FROM users WHERE role='Teacher' ORDER BY name");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) items.add(rs.getString("uid") + " — " + rs.getString("name"));
        } catch (SQLException e) { e.printStackTrace(); }
        assignTeacherCombo.setItems(items);
    }

    private ObservableList<String> loadDeptItems() {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT deptId, deptName FROM departments ORDER BY deptName");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) items.add(rs.getString("deptId") + " — " + rs.getString("deptName"));
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    private ObservableList<String> loadBatchItems() {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT batchId, batchYear FROM batches ORDER BY batchYear DESC");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) items.add(rs.getString("batchId") + " — " + rs.getString("batchYear"));
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    private void loadCourses() {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT courseCode, courseName FROM courses ORDER BY courseCode");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) items.add(rs.getString("courseCode") + " — " + rs.getString("courseName"));
        } catch (SQLException e) { e.printStackTrace(); }
        assignCourseCombo.setItems(items);
    }

    private void loadAllRoomsForRuleTab() {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT roomId, roomName FROM classrooms ORDER BY roomId");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) items.add(rs.getString("roomId") + " — " + rs.getString("roomName"));
        } catch (SQLException e) { e.printStackTrace(); }
        ruleRoomCombo.setItems(items);
    }

    private void prefillRuleField(TextField field, String ruleType) {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "SELECT TOP 1 value FROM rules WHERE type=? AND isActive=1 AND roomId IS NULL ORDER BY ruleId DESC")) {
            s.setString(1, ruleType);
            try (ResultSet rs = s.executeQuery()) { if (rs.next()) field.setText(rs.getString("value")); }
        } catch (SQLException e) { /* skip */ }
    }

    private void refreshBatchCombos() {
        ObservableList<String> fresh = loadBatchItems();
        assignBatchCombo.setItems(fresh);
        secBatchCombo.setItems(FXCollections.observableArrayList(fresh));
    }

    private void refreshDeptCombos() {
        secDeptCombo.setItems(loadDeptItems());
    }

    // ==================================================================
    //  Tab 1 — Campus Core Setup handlers
    // ==================================================================

    @FXML
    public void onAddDeptClicked(ActionEvent event) {
        String id = trim(deptIdField), name = trim(deptNameField);
        if (id.isEmpty() || name.isEmpty()) { showError("Missing fields", "Dept ID and Name are required."); return; }
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("INSERT INTO departments (deptId, deptName) VALUES (?, ?)")) {
            s.setString(1, id); s.setString(2, name); s.executeUpdate();
            showInfo("Department added", id + " — " + name);
            deptIdField.clear(); deptNameField.clear();
            refreshDeptCombos();
        } catch (SQLException e) { showError("Insert failed", e.getMessage()); }
    }

    @FXML
    public void onAddBatchClicked(ActionEvent event) {
        String id = trim(batchIdField), year = trim(batchYearField);
        if (id.isEmpty() || year.isEmpty()) { showError("Missing fields", "Batch ID and Year are required."); return; }
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("INSERT INTO batches (batchId, batchYear) VALUES (?, ?)")) {
            s.setString(1, id); s.setString(2, year); s.executeUpdate();
            showInfo("Batch added", id + " — " + year);
            batchIdField.clear(); batchYearField.clear();
            refreshBatchCombos();
        } catch (SQLException e) { showError("Insert failed", e.getMessage()); }
    }

    @FXML
    public void onAddCourseClicked(ActionEvent event) {
        String code = trim(courseCodeField), name = trim(courseNameField), credits = trim(courseCreditsField);
        if (code.isEmpty() || name.isEmpty()) { showError("Missing fields", "Code and Name are required."); return; }
        int cr = 3; try { cr = Integer.parseInt(credits); } catch (NumberFormatException ignored) {}
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("INSERT INTO courses (courseCode, courseName, credits) VALUES (?, ?, ?)")) {
            s.setString(1, code); s.setString(2, name); s.setInt(3, cr); s.executeUpdate();
            showInfo("Course added", code + " — " + name + " (" + cr + " cr)");
            courseCodeField.clear(); courseNameField.clear(); courseCreditsField.clear();
            loadCourses();
        } catch (SQLException e) { showError("Insert failed", e.getMessage()); }
    }

    @FXML
    public void onAddRoomClicked(ActionEvent event) {
        String id = trim(roomIdField), name = trim(roomNameField), capStr = trim(roomCapacityField);
        if (id.isEmpty() || name.isEmpty()) { showError("Missing fields", "Room ID and Name are required."); return; }
        int cap = 30; try { cap = Integer.parseInt(capStr); } catch (NumberFormatException ignored) {}
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO classrooms (roomId, roomName, capacity, hasProjector, location) VALUES (?, ?, ?, 0, '')")) {
            s.setString(1, id); s.setString(2, name); s.setInt(3, cap); s.executeUpdate();
            showInfo("Room added", id + " — " + name + " (cap " + cap + ")");
            roomIdField.clear(); roomNameField.clear(); roomCapacityField.clear();
            loadAllRoomsForRuleTab(); tryLoadAvailableRooms();
        } catch (SQLException e) { showError("Insert failed", e.getMessage()); }
    }

    @FXML
    public void onAddSectionClicked(ActionEvent event) {
        String batchSel = secBatchCombo.getValue(), deptSel = secDeptCombo.getValue(), secName = trim(secNameField);
        if (batchSel == null || deptSel == null || secName.isEmpty()) {
            showError("Missing fields", "Select Batch, Department, and enter a Section Name."); return;
        }
        String batchId = extractId(batchSel), deptId = extractId(deptSel);
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO batch_dept_sections (deptId, batchId, sectionName) VALUES (?, ?, ?)")) {
            s.setString(1, deptId); s.setString(2, batchId); s.setString(3, secName); s.executeUpdate();
            showInfo("Section added", "Section '" + secName + "' → " + deptSel + " / " + batchSel);
            secNameField.clear();
            String ab = assignBatchCombo.getValue(), ad = assignDeptCombo.getValue();
            if (ab != null && ad != null && extractId(ab).equals(batchId) && extractId(ad).equals(deptId)) {
                loadSectionsForBatchDept(batchId, deptId);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("uq_bds")) {
                showError("Duplicate", "Section '" + secName + "' already exists for this Batch + Dept.");
            } else { showError("Insert failed", e.getMessage()); }
        }
    }

    @FXML
    public void onAddTeacherClicked(ActionEvent event) {
        String name     = trim(teacherNameField);
        String email    = trim(teacherEmailField);
        String password = trim(teacherPasswordField);

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            teacherFormMessageLabel.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold;");
            teacherFormMessageLabel.setText("All fields are required."); return;
        }
        if (!email.contains("@")) {
            teacherFormMessageLabel.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold;");
            teacherFormMessageLabel.setText("Enter a valid email address."); return;
        }
        if (password.length() < 6) {
            teacherFormMessageLabel.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold;");
            teacherFormMessageLabel.setText("Password must be at least 6 characters."); return;
        }

        String uid = "T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO users (uid, name, email, role, password, level) VALUES (?, ?, ?, 'Teacher', ?, 2)")) {
            s.setString(1, uid); s.setString(2, name); s.setString(3, email); s.setString(4, password);
            s.executeUpdate();
            teacherFormMessageLabel.setStyle("-fx-text-fill: #047857; -fx-font-weight: bold;");
            teacherFormMessageLabel.setText("✓ Teacher created: " + name + " (ID: " + uid + ")");
            teacherNameField.clear(); teacherEmailField.clear(); teacherPasswordField.clear();
            // Refresh all teacher combos so the new teacher appears immediately
            loadTeachers();
            overrideTeacherCombo.setItems(FXCollections.observableArrayList(assignTeacherCombo.getItems()));
            reportTeacherCombo.setItems(FXCollections.observableArrayList(assignTeacherCombo.getItems()));
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "Unknown error" : e.getMessage();
            teacherFormMessageLabel.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold;");
            if (msg.contains("email") || msg.contains("UNIQUE") || msg.contains("duplicate") || msg.contains("Violation")) {
                teacherFormMessageLabel.setText("Email already exists. Use a different email.");
            } else {
                teacherFormMessageLabel.setText("Error: " + msg);
            }
        }
    }

    // ==================================================================
    //  Tab 2 — Rules (UC-10)
    // ==================================================================

    @FXML public void onSaveOpeningTimeRule(ActionEvent e) { upsertTimeRule("UNI_OPENING_TIME", openingTimeField, "University Opening Time"); }
    @FXML public void onSaveClosingTimeRule(ActionEvent e) { upsertTimeRule("UNI_CLOSING_TIME", closingTimeField, "University Closing Time"); }

    private void upsertTimeRule(String ruleType, TextField field, String label) {
        String val = trim(field);
        if (val.isEmpty()) { showError("Missing value", "Enter a time in HH:mm format."); return; }
        // Validate HH:mm format
        try { LocalTime.parse(val, DateTimeFormatter.ofPattern("H:mm")); }
        catch (Exception ex) { showError("Invalid time", "Enter a valid time in HH:mm format (e.g. 08:00)."); return; }
        // Normalise to HH:mm
        LocalTime t = LocalTime.parse(val, DateTimeFormatter.ofPattern("H:mm"));
        String normalised = String.format("%02d:%02d", t.getHour(), t.getMinute());
        try (Connection c = DBConnection.getConnection()) {
            try (PreparedStatement d = c.prepareStatement("UPDATE rules SET isActive=0 WHERE type=? AND roomId IS NULL")) {
                d.setString(1, ruleType); d.executeUpdate();
            }
            String ruleId = "RULE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            try (PreparedStatement i = c.prepareStatement(
                    "INSERT INTO rules (ruleId,ruleName,description,type,value,isActive,roomId) VALUES(?,?,?,?,?,1,NULL)")) {
                i.setString(1, ruleId); i.setString(2, label); i.setString(3, "Global: " + label);
                i.setString(4, ruleType); i.setString(5, normalised); i.executeUpdate();
            }
            field.setText(normalised);

            // ── Auto-rebalance existing assignments that violate the new window ──
            rebalanceExistingAssignments(c, ruleType, normalised);

            showInfo("Rule saved ✓", label + " = " + normalised);
        } catch (SQLException ex) { showError("Save failed", ex.getMessage()); }
    }

    /**
     * Reversible proportional rebalancer.
     *
     * Key insight: we store a BASELINE snapshot (original timetable) in
     * teacher_assignments_baseline. Every time the window changes we
     * recalculate ALL assignments from the baseline, so:
     *   baseline → window A → window B → window A  ⇒  same timetable.
     *
     * Formula (no rounding — exact integer arithmetic via millisecond precision):
     *   For each assignment:
     *     fraction = (baseline.start − baselineOpen) / baselineWindow
     *     new.start = newOpen + fraction * newWindow          (round to minute)
     *     new.end   = new.start + (baseline.dur / baselineWindow) * newWindow  (round to minute)
     */
    private void rebalanceExistingAssignments(Connection c, String ruleType, String newValue) {
        try {
            String openVal  = ruleType.equals("UNI_OPENING_TIME") ? newValue : ruleStr(c, "UNI_OPENING_TIME");
            String closeVal = ruleType.equals("UNI_CLOSING_TIME") ? newValue : ruleStr(c, "UNI_CLOSING_TIME");

            if (openVal.isEmpty() || closeVal.isEmpty()) {
                showInfo("Rebalance Skipped",
                        "Both Opening and Closing times must be set before rebalancing.\n"
                      + "Currently: Open=" + (openVal.isEmpty() ? "not set" : openVal)
                      + ", Close=" + (closeVal.isEmpty() ? "not set" : closeVal));
                return;
            }

            int newOpenMins  = parseHHmm(openVal);
            int newCloseMins = parseHHmm(closeVal);
            int newWindow    = newCloseMins - newOpenMins;
            if (newWindow <= 0) { showError("Invalid window", "Closing must be after Opening."); return; }

            // ── Ensure baseline table exists & is populated ──────────────
            ensureBaseline(c);

            // ── Read baseline bounds ─────────────────────────────────────
            int blOpen = newOpenMins, blClose = newCloseMins;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT MIN(startTime) AS mn, MAX(endTime) AS mx FROM teacher_assignments_baseline")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Time mn = rs.getTime("mn"), mx = rs.getTime("mx");
                        if (mn != null) blOpen  = toMinsSinceMidnight(mn);
                        if (mx != null) blClose = toMinsSinceMidnight(mx);
                    }
                }
            }
            int blWindow = blClose - blOpen;
            if (blWindow <= 0) blWindow = newWindow;

            double scale = (double) newWindow / blWindow;

            // ── Scale every assignment from baseline ─────────────────────
            int updated = 0;
            String fetchSql =
                "SELECT b.assignmentId, b.startTime AS blStart, b.endTime AS blEnd "
              + "FROM teacher_assignments_baseline b "
              + "ORDER BY b.assignmentId";

            List<int[]> updates = new ArrayList<>(); // [id, newStartMins, newEndMins]
            try (PreparedStatement ps = c.prepareStatement(fetchSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("assignmentId");
                    int bStart = toMinsSinceMidnight(rs.getTime("blStart"));
                    int bEnd   = toMinsSinceMidnight(rs.getTime("blEnd"));
                    int bDur   = bEnd - bStart;
                    if (bDur <= 0) bDur = 60;

                    // Exact proportional mapping
                    int offsetFromBlOpen = bStart - blOpen;
                    int nStart = newOpenMins + (int) Math.round((double) offsetFromBlOpen / blWindow * newWindow);
                    int nDur   = (int) Math.round((double) bDur / blWindow * newWindow);
                    if (nDur < 15) nDur = 15;  // absolute minimum
                    int nEnd = nStart + nDur;

                    // Clamp
                    if (nStart < newOpenMins) nStart = newOpenMins;
                    if (nEnd > newCloseMins) { nEnd = newCloseMins; nStart = nEnd - nDur; if (nStart < newOpenMins) nStart = newOpenMins; }

                    updates.add(new int[]{id, nStart, nEnd});
                }
            }

            try (PreparedStatement upd = c.prepareStatement(
                    "UPDATE teacher_assignments SET startTime = CAST(? AS TIME), endTime = CAST(? AS TIME) "
                  + "WHERE assignmentId = ?")) {
                for (int[] row : updates) {
                    String ns = String.format("%02d:%02d:00", row[1]/60, row[1]%60);
                    String ne = String.format("%02d:%02d:00", row[2]/60, row[2]%60);
                    upd.setString(1, ns); upd.setString(2, ne); upd.setInt(3, row[0]);
                    upd.executeUpdate();
                    updated++;

                    System.out.println("  [Rebalance] #" + row[0] + " → " + ns.substring(0,5) + "–" + ne.substring(0,5));
                }
            }

            showInfo("Rebalance Complete ✓",
                    "Scale: " + String.format("%.4f", scale)
                  + "\nAssignments updated: " + updated
                  + "\nOperating hours: " + openVal + " – " + closeVal
                  + "\n\nThis is fully reversible — change back to original hours to restore the original timetable.");

        } catch (Exception ex) {
            showError("Rebalance Error", "Failed to rebalance:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Ensures the baseline table exists. If it doesn't, creates it and
     * copies the current teacher_assignments into it (the "original" timetable).
     * The baseline is NEVER modified by rebalancing — it is the reference truth.
     */
    private void ensureBaseline(Connection c) throws SQLException {
        // Check if table exists
        boolean exists = false;
        try (ResultSet rs = c.getMetaData().getTables(null, null, "teacher_assignments_baseline", null)) {
            exists = rs.next();
        }
        if (!exists) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT * INTO teacher_assignments_baseline FROM teacher_assignments")) {
                ps.executeUpdate();
            }
            System.out.println("[Rebalance] Created baseline snapshot with current timetable.");
        }
    }

    /** Parses "HH:mm" into minutes since midnight. */
    private static int parseHHmm(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    @FXML public void onSaveMaxCoursesRule(ActionEvent e)       { upsertGlobalRule("MAX_COURSES",        maxCoursesField,       "Max Courses per Teacher"); }
    @FXML public void onSaveMaxClassesPerDayRule(ActionEvent e) { upsertGlobalRule("MAX_CLASSES_PER_DAY", maxClassesPerDayField, "Max Classes/Day/Teacher"); }
    @FXML public void onSaveMaxDurationRule(ActionEvent e)      { upsertGlobalRule("MAX_DURATION",       maxDurationField,      "Max Class Duration (min)"); }
    @FXML public void onSaveRestGapRule(ActionEvent e)          { upsertGlobalRule("REST_GAP",           restGapField,          "Min Rest Gap (min)"); }

    private void upsertGlobalRule(String ruleType, TextField field, String label) {
        String val = trim(field);
        if (val.isEmpty()) { showError("Missing value", "Enter a number for " + label + "."); return; }
        try (Connection c = DBConnection.getConnection()) {
            try (PreparedStatement d = c.prepareStatement("UPDATE rules SET isActive=0 WHERE type=? AND roomId IS NULL")) {
                d.setString(1, ruleType); d.executeUpdate();
            }
            String ruleId = "RULE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            try (PreparedStatement i = c.prepareStatement(
                    "INSERT INTO rules (ruleId,ruleName,description,type,value,isActive,roomId) VALUES(?,?,?,?,?,1,NULL)")) {
                i.setString(1, ruleId); i.setString(2, label); i.setString(3, "Global: " + label);
                i.setString(4, ruleType); i.setString(5, val); i.executeUpdate();
            }
            showInfo("Rule saved", label + " = " + val);
        } catch (SQLException ex) { showError("Save failed", ex.getMessage()); }
    }

    @FXML
    public void onSaveRoomRuleClicked(ActionEvent event) {
        String roomSel = ruleRoomCombo.getValue(), capVal = trim(roomCapOverrideField);
        if (roomSel == null) { showError("No room", "Pick a room."); return; }
        if (capVal.isEmpty()) { showError("Missing value", "Enter capacity."); return; }
        String roomId = extractId(roomSel);
        String ruleId = "RULE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(
                     "INSERT INTO rules (ruleId,ruleName,description,type,value,isActive,roomId) "
                   + "VALUES(?,'Room Cap Override','Override capacity','ROOM_CAPACITY_OVERRIDE',?,1,?)")) {
            s.setString(1, ruleId); s.setString(2, capVal); s.setString(3, roomId); s.executeUpdate();
            showInfo("Room rule saved", roomId + " → cap " + capVal);
            roomCapOverrideField.clear();
        } catch (SQLException e) { showError("Failed", e.getMessage()); }
    }

    // ==================================================================
    //  Tab 3 — Assignment + Validation Engine
    // ==================================================================

    @FXML
    public void onAssignTimetableClicked(ActionEvent event) {
        String teacherSel = assignTeacherCombo.getValue();
        String deptSel    = assignDeptCombo.getValue();
        String batchSel   = assignBatchCombo.getValue();
        String section    = assignSectionCombo.getValue();
        String courseSel  = assignCourseCombo.getValue();
        String day        = assignDayCombo.getValue();
        String startStr   = trim(assignStartTimeField);
        String endStr     = trim(assignEndTimeField);
        String roomSel    = assignRoomCombo.getValue();

        if (teacherSel == null || deptSel == null || batchSel == null
                || section == null || courseSel == null || day == null || roomSel == null
                || startStr.isEmpty() || endStr.isEmpty()) {
            showError("Incomplete form", "Please fill in every field."); return;
        }

        String teacherUid = extractId(teacherSel);
        String deptId = extractId(deptSel), batchId = extractId(batchSel);
        String courseCode = extractId(courseSel), roomId = extractId(roomSel);

        LocalTime startTime, endTime;
        try {
            startTime = LocalTime.parse(startStr, DateTimeFormatter.ofPattern("H:mm"));
            endTime   = LocalTime.parse(endStr,   DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception ex) { showError("Bad time", "Use HH:mm format."); return; }
        if (!endTime.isAfter(startTime)) { showError("Bad times", "End must be after Start."); return; }

        try (Connection conn = DBConnection.getConnection()) {
            // ── University Opening / Closing Time rules ──────────────────────────
            String openingVal = ruleStr(conn, "UNI_OPENING_TIME");
            String closingVal = ruleStr(conn, "UNI_CLOSING_TIME");
            if (!openingVal.isEmpty()) {
                LocalTime opening = LocalTime.parse(openingVal, DateTimeFormatter.ofPattern("H:mm"));
                if (startTime.isBefore(opening)) {
                    showError("Rule: OPENING TIME",
                            "Class starts at " + startStr + " but university opens at " + openingVal + ".\n"
                          + "No classes may be scheduled before opening time."); return;
                }
            }
            if (!closingVal.isEmpty()) {
                LocalTime closing = LocalTime.parse(closingVal, DateTimeFormatter.ofPattern("H:mm"));
                if (endTime.isAfter(closing)) {
                    showError("Rule: CLOSING TIME",
                            "Class ends at " + endStr + " but university closes at " + closingVal + ".\n"
                          + "No classes may be scheduled after closing time."); return;
                }
            }

            // ── Teacher Overlap Check (HARD RULE — cannot be in two places at once) ──
            String overlapConflict = checkTeacherOverlap(conn, teacherUid, day, startTime, endTime);
            if (overlapConflict != null) {
                showError("Scheduling Conflict",
                        "Teacher " + teacherSel + " is already teaching on " + day + ":\n\n"
                      + overlapConflict + "\n\n"
                      + "A teacher cannot be assigned to two classes at the same time.");
                return;
            }

            long dur = ChronoUnit.MINUTES.between(startTime, endTime);
            int maxDur = ruleInt(conn, "MAX_DURATION"); if (maxDur <= 0) maxDur = 90;
            if (dur > maxDur) { showError("Rule: MAX_DURATION", "Class is " + dur + " min. Max = " + maxDur + "."); return; }

            int maxPD = ruleInt(conn, "MAX_CLASSES_PER_DAY"); if (maxPD <= 0) maxPD = 3;
            int onDay = countOnDay(conn, teacherUid, day);
            if (onDay + 1 > maxPD) { showError("Rule: MAX_CLASSES_PER_DAY", teacherSel + " has " + onDay + " class(es) on " + day + ". Max = " + maxPD + "."); return; }

            int maxC = ruleInt(conn, "MAX_COURSES");
            if (maxC > 0) {
                int cur = countCourses(conn, teacherUid);
                boolean already = teachesCourse(conn, teacherUid, courseCode);
                if ((!already ? cur + 1 : cur) > maxC) { showError("Rule: MAX_COURSES", cur + " courses. Max = " + maxC + "."); return; }
            }

            int gap = ruleInt(conn, "REST_GAP");
            if (gap > 0) {
                String v = checkGap(conn, teacherUid, day, startTime, endTime, gap);
                if (v != null) { showError("Rule: REST_GAP", v); return; }
            }

            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO teacher_assignments "
                  + "(teacherUid,courseCode,sectionName,deptId,batchId,dayOfWeek,startTime,endTime,roomId) "
                  + "VALUES(?,?,?,?,?,?,?,?,?)")) {
                ins.setString(1, teacherUid); ins.setString(2, courseCode);
                ins.setString(3, section);    ins.setString(4, deptId);
                ins.setString(5, batchId);    ins.setString(6, day);
                ins.setString(7, startStr);   ins.setString(8, endStr);
                ins.setString(9, roomId);     ins.executeUpdate();
            }
            showInfo("Assignment created ✓",
                    teacherSel + "\n→ " + courseSel + "\n  " + day + " " + startStr + "–" + endStr
                  + "\n  Room: " + roomSel + "  Section: " + section);

            assignTeacherCombo.getSelectionModel().clearSelection();
            assignBatchCombo.getSelectionModel().clearSelection();
            assignCourseCombo.getSelectionModel().clearSelection();
            assignDayCombo.getSelectionModel().clearSelection();
            assignRoomCombo.getItems().clear(); assignRoomCombo.setDisable(true);
            assignStartTimeField.clear(); assignEndTimeField.clear();
        } catch (SQLException e) { showError("DB error", e.getMessage()); }
    }

    // ==================================================================
    //  Validation helpers
    // ==================================================================

    /** Returns the active string value of a rule, or "" if not set. */
    private String ruleStr(Connection c, String type) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT TOP 1 value FROM rules WHERE type=? AND isActive=1 AND roomId IS NULL ORDER BY ruleId DESC")) {
            s.setString(1, type);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) { String v = rs.getString("value"); return v == null ? "" : v.trim(); }
            }
        }
        return "";
    }

    private int ruleInt(Connection c, String type) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT TOP 1 value FROM rules WHERE type=? AND isActive=1 AND roomId IS NULL ORDER BY ruleId DESC")) {
            s.setString(1, type);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) try { return Integer.parseInt(rs.getString("value").trim()); } catch (NumberFormatException e) {}
            }
        }
        return -1;
    }

    private int countOnDay(Connection c, String uid, String day) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT COUNT(*) AS cnt FROM teacher_assignments WHERE teacherUid=? AND dayOfWeek=?")) {
            s.setString(1, uid); s.setString(2, day);
            try (ResultSet rs = s.executeQuery()) { return rs.next() ? rs.getInt("cnt") : 0; }
        }
    }

    private int countCourses(Connection c, String uid) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT COUNT(DISTINCT courseCode) AS cnt FROM teacher_assignments WHERE teacherUid=?")) {
            s.setString(1, uid);
            try (ResultSet rs = s.executeQuery()) { return rs.next() ? rs.getInt("cnt") : 0; }
        }
    }

    private boolean teachesCourse(Connection c, String uid, String code) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "SELECT 1 FROM teacher_assignments WHERE teacherUid=? AND courseCode=?")) {
            s.setString(1, uid); s.setString(2, code);
            try (ResultSet rs = s.executeQuery()) { return rs.next(); }
        }
    }

    /**
     * Checks if the teacher already has ANY assignment on the given day
     * that overlaps with [newStart, newEnd).
     *
     * Overlap condition (standard interval overlap):
     *   existing.start < newEnd  AND  existing.end > newStart
     *
     * @return a human-readable conflict description, or null if no overlap.
     */
    private String checkTeacherOverlap(Connection c, String uid, String day,
                                        LocalTime newStart, LocalTime newEnd) throws SQLException {
        String sql =
            "SELECT ta.courseCode, ta.sectionName, ta.startTime, ta.endTime, cr.courseName "
          + "FROM teacher_assignments ta "
          + "LEFT JOIN courses cr ON cr.courseCode = ta.courseCode "
          + "WHERE ta.teacherUid = ? "
          + "  AND ta.dayOfWeek  = ? "
          + "  AND ta.startTime  < CAST(? AS TIME) "
          + "  AND ta.endTime    > CAST(? AS TIME)";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uid);
            ps.setString(2, day);
            ps.setString(3, String.format("%02d:%02d:00", newEnd.getHour(),   newEnd.getMinute()));
            ps.setString(4, String.format("%02d:%02d:00", newStart.getHour(), newStart.getMinute()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String existStart = rs.getString("startTime");
                    String existEnd   = rs.getString("endTime");
                    String course     = rs.getString("courseCode");
                    String courseName = rs.getString("courseName");
                    String section    = rs.getString("sectionName");
                    // Trim to HH:mm
                    if (existStart != null && existStart.length() > 5) existStart = existStart.substring(0, 5);
                    if (existEnd   != null && existEnd.length()   > 5) existEnd   = existEnd.substring(0, 5);
                    return course + " – " + (courseName == null ? "" : courseName)
                         + "  [Section " + section + "]"
                         + "  " + existStart + " – " + existEnd;
                }
            }
        }
        return null;  // no overlap
    }

    private String checkGap(Connection c, String uid, String day,
                             LocalTime ns, LocalTime ne, int gap) throws SQLException {
        List<LocalTime[]> ex = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(
                "SELECT startTime, endTime FROM teacher_assignments WHERE teacherUid=? AND dayOfWeek=? ORDER BY startTime")) {
            s.setString(1, uid); s.setString(2, day);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    Time st = rs.getTime("startTime"), et = rs.getTime("endTime");
                    if (st != null && et != null) ex.add(new LocalTime[]{st.toLocalTime(), et.toLocalTime()});
                }
            }
        }
        for (LocalTime[] sl : ex) {
            if (ns.isBefore(sl[1]) && ne.isAfter(sl[0])) return "Overlap with " + sl[0] + "–" + sl[1] + " on " + day + ".";
            long g1 = ChronoUnit.MINUTES.between(sl[1], ns);
            long g2 = ChronoUnit.MINUTES.between(ne, sl[0]);
            if (g1 >= 0 && g1 < gap) return "Only " + g1 + " min gap after " + sl[1] + ". Need " + gap + ".";
            if (g2 >= 0 && g2 < gap) return "Only " + g2 + " min gap before " + sl[0] + ". Need " + gap + ".";
        }
        return null;
    }

    // ==================================================================
    //  Tab 4 — Analytics & Reports (UC-12)
    // ==================================================================

    private static final String RPT_CLASSROOM_UTIL    = "Classroom Utilization";
    private static final String RPT_FACULTY_LOAD      = "Faculty Load Summary";
    private static final String RPT_TEACHER_TIMETABLE = "Teacher Weekly Timetable";
    private static final String RPT_ROOM_TIMETABLE    = "Room Weekly Timetable";

    private void initReportsTab() {
        reportTypeCombo.setItems(FXCollections.observableArrayList(
                RPT_CLASSROOM_UTIL, RPT_FACULTY_LOAD, RPT_TEACHER_TIMETABLE, RPT_ROOM_TIMETABLE));
        reportTeacherCombo.setItems(FXCollections.observableArrayList(assignTeacherCombo.getItems()));
        ObservableList<String> roomItems = FXCollections.observableArrayList();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT roomId, roomName FROM classrooms ORDER BY roomId");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) roomItems.add(rs.getString("roomId") + " — " + rs.getString("roomName"));
        } catch (SQLException e) { e.printStackTrace(); }
        reportRoomCombo.setItems(roomItems);

        reportTypeCombo.valueProperty().addListener((obs, o, n) -> {
            dateRangeInputs.setVisible(false);   dateRangeInputs.setManaged(false);
            teacherSelectInput.setVisible(false); teacherSelectInput.setManaged(false);
            roomSelectInput.setVisible(false);    roomSelectInput.setManaged(false);
            if (n == null) return;
            switch (n) {
                case RPT_CLASSROOM_UTIL: case RPT_FACULTY_LOAD:
                    dateRangeInputs.setVisible(true); dateRangeInputs.setManaged(true); break;
                case RPT_TEACHER_TIMETABLE:
                    teacherSelectInput.setVisible(true); teacherSelectInput.setManaged(true); break;
                case RPT_ROOM_TIMETABLE:
                    roomSelectInput.setVisible(true); roomSelectInput.setManaged(true); break;
            }
        });
    }

    @FXML
    public void onGenerateReportClicked(ActionEvent event) {
        String reportType = reportTypeCombo.getValue();
        if (reportType == null) { showError("No report selected", "Pick a report type."); return; }
        reportDisplayArea.getChildren().clear();
        try {
            switch (reportType) {
                case RPT_TEACHER_TIMETABLE: generateTeacherTimetable(); break;
                case RPT_ROOM_TIMETABLE:    generateRoomTimetable();    break;
                case RPT_CLASSROOM_UTIL:    generateClassroomUtil();    break;
                case RPT_FACULTY_LOAD:      generateFacultyLoad();      break;
            }
        } catch (SQLException e) { showError("Report error", e.getMessage()); }
    }

    private void generateTeacherTimetable() throws SQLException {
        String sel = reportTeacherCombo.getValue();
        if (sel == null) { showError("No teacher", "Select a teacher."); return; }
        List<Map<String, String>> rows = sessionRepo.getTeacherWeeklyTimetable(extractId(sel));
        Label title = new Label("Weekly Timetable: " + sel);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1A365D;");
        reportDisplayArea.getChildren().add(title);
        if (rows.isEmpty()) { reportDisplayArea.getChildren().add(styledLabel("No assignments found.", "#94A3B8", true)); return; }
        reportDisplayArea.getChildren().add(buildHeaderRow(new String[]{"Day", "Time", "Course", "Section", "Room", "Dept/Batch"}));
        String lastDay = "";
        for (Map<String, String> r : rows) {
            String day = r.get("dayOfWeek");
            if (!day.equals(lastDay)) { reportDisplayArea.getChildren().add(styledLabel("— " + day + " —", "#1A365D", false)); lastDay = day; }
            reportDisplayArea.getChildren().add(buildDataRow(new String[]{
                    day, r.get("startTime") + " – " + r.get("endTime"),
                    r.get("courseCode") + " " + r.get("courseName"), r.get("sectionName"),
                    r.get("roomId") + " " + r.get("roomName"), r.get("deptId") + "/" + r.get("batchId")}));
        }
        reportDisplayArea.getChildren().add(styledLabel("Total sessions: " + rows.size(), "#475569", false));
    }

    private void generateRoomTimetable() throws SQLException {
        String sel = reportRoomCombo.getValue();
        if (sel == null) { showError("No room", "Select a room."); return; }
        List<Map<String, String>> rows = sessionRepo.getRoomWeeklyTimetable(extractId(sel));
        Label title = new Label("Room Schedule: " + sel);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1A365D;");
        reportDisplayArea.getChildren().add(title);
        if (rows.isEmpty()) { reportDisplayArea.getChildren().add(styledLabel("Room is completely free.", "#94A3B8", true)); return; }
        reportDisplayArea.getChildren().add(buildHeaderRow(new String[]{"Day", "Time", "Course", "Teacher", "Section"}));
        String lastDay = "";
        for (Map<String, String> r : rows) {
            String day = r.get("dayOfWeek");
            if (!day.equals(lastDay)) { reportDisplayArea.getChildren().add(styledLabel("— " + day + " —", "#1A365D", false)); lastDay = day; }
            reportDisplayArea.getChildren().add(buildDataRow(new String[]{
                    day, r.get("startTime") + " – " + r.get("endTime"),
                    r.get("courseCode") + " " + r.get("courseName"), r.get("teacherName"), r.get("sectionName")}));
        }
        reportDisplayArea.getChildren().add(styledLabel("Total sessions: " + rows.size(), "#475569", false));
    }

    private void generateClassroomUtil() throws SQLException {
        List<Map<String, String>> rows = sessionRepo.getClassroomUtilization();
        reportDisplayArea.getChildren().add(new Label("Classroom Utilization Report"));
        reportDisplayArea.getChildren().add(buildHeaderRow(new String[]{"Room ID", "Room Name", "Capacity", "Weekly Sessions", "Usage"}));
        for (Map<String, String> r : rows) {
            int slots = 0; try { slots = Integer.parseInt(r.get("totalSlots")); } catch (Exception ignored) {}
            reportDisplayArea.getChildren().add(buildDataRow(new String[]{
                    r.get("roomId"), r.get("roomName"), r.get("capacity"), String.valueOf(slots), slots == 0 ? "Idle" : slots + " session(s)"}));
        }
    }

    private void generateFacultyLoad() throws SQLException {
        List<Map<String, String>> rows = sessionRepo.getFacultyLoadSummary();
        reportDisplayArea.getChildren().add(new Label("Faculty Load Summary"));
        reportDisplayArea.getChildren().add(buildHeaderRow(new String[]{"Teacher ID", "Name", "Sessions", "Courses"}));
        for (Map<String, String> r : rows) {
            reportDisplayArea.getChildren().add(buildDataRow(new String[]{r.get("uid"), r.get("name"), r.get("totalSessions"), r.get("distinctCourses")}));
        }
    }

    private HBox buildHeaderRow(String[] cols) {
        HBox row = new HBox(8);
        row.setStyle("-fx-background-color: #1A365D; -fx-background-radius: 6; -fx-padding: 8 12 8 12;");
        for (String col : cols) { Label l = new Label(col); l.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px;"); l.setPrefWidth(160); row.getChildren().add(l); }
        return row;
    }

    private HBox buildDataRow(String[] vals) {
        HBox row = new HBox(8);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 4; -fx-padding: 6 12 6 12; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
        for (String val : vals) { Label l = new Label(val == null ? "" : val); l.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;"); l.setPrefWidth(160); l.setWrapText(true); row.getChildren().add(l); }
        return row;
    }

    private Label styledLabel(String text, String color, boolean italic) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + ";" + (italic ? " -fx-font-style: italic;" : " -fx-font-weight: bold;"));
        return l;
    }

    // ==================================================================
    //  Tab 5 — Emergency Override (UC-11)
    // ==================================================================

    private void initOverrideTab() {
        overrideTeacherCombo.setItems(FXCollections.observableArrayList(assignTeacherCombo.getItems()));
        overrideDayCombo.setItems(FXCollections.observableArrayList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"));
        overrideStatusCombo.setItems(FXCollections.observableArrayList("CANCELLED", "DELAYED", "ON-TIME"));
        
        // Initialize the new day combo for schedule changes
        if (overrideNewDayCombo != null) {
            overrideNewDayCombo.setItems(FXCollections.observableArrayList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"));
        }

        colAssignId.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("assignmentId", "")));
        colCourse.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("courseCode", "") + " " + cd.getValue().getOrDefault("courseName", "")));
        colRoom.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("roomId", "") + " " + cd.getValue().getOrDefault("roomName", "")));
        colStart.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("startTime", "")));
        colEnd.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("endTime", "")));
        colSection.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("sectionName", "")));
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("status", "")));

        overrideSessionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            if (newRow == null) { overrideActionPane.setVisible(false); overrideActionPane.setManaged(false); return; }
            overrideActionPane.setVisible(true); overrideActionPane.setManaged(true);
            String day = overrideDayCombo.getValue();
            String startStr = newRow.getOrDefault("startTime", ""), endStr = newRow.getOrDefault("endTime", "");
            if (day != null && !startStr.isEmpty() && !endStr.isEmpty()) {
                try {
                    LocalTime start = LocalTime.parse(startStr.length() > 5 ? startStr.substring(0, 5) : startStr);
                    LocalTime end   = LocalTime.parse(endStr.length()   > 5 ? endStr.substring(0, 5)   : endStr);
                    loadSwapRooms(day, start, end);
                } catch (Exception ex) { swapRoomCombo.getItems().clear(); }
            }
        });
    }

    private void loadSwapRooms(String day, LocalTime start, LocalTime end) {
        ObservableList<String> items = FXCollections.observableArrayList();
        String sql = "SELECT cr.roomId, cr.roomName FROM classrooms cr "
                   + "WHERE cr.roomId NOT IN (SELECT ta.roomId FROM teacher_assignments ta "
                   + "WHERE ta.dayOfWeek=? AND ta.startTime<? AND ta.endTime>?) ORDER BY cr.roomId";
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, day); s.setString(2, end.toString()); s.setString(3, start.toString());
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) items.add(rs.getString("roomId") + " — " + rs.getString("roomName"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        swapRoomCombo.setItems(items);
        swapRoomCombo.setPromptText(items.isEmpty() ? "No rooms free" : items.size() + " room(s) available");
    }

    @FXML
    public void onSearchTeacherClicked(ActionEvent event) {
        String teacherSel = overrideTeacherCombo.getValue(), day = overrideDayCombo.getValue();
        if (teacherSel == null || day == null) { showError("Missing input", "Select a teacher and day."); return; }
        try {
            List<Map<String, String>> sessions = overrideCtrl.searchActiveSession(extractId(teacherSel), day);
            overrideSessionTable.setItems(FXCollections.observableArrayList(sessions));
            overrideActionPane.setVisible(false); overrideActionPane.setManaged(false);
            if (sessions.isEmpty()) showInfo("No sessions", teacherSel + " has no sessions on " + day + ".");
        } catch (SQLException e) { showError("Search failed", e.getMessage()); }
    }

    @FXML
    public void onOverrideStatusClicked(ActionEvent event) {
        Map<String, String> selected = overrideSessionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("No selection", "Select a session row first."); return; }
        String newStatus = overrideStatusCombo.getValue(), reason = overrideReasonField.getText();
        if (newStatus == null || newStatus.isEmpty()) { showError("No status", "Pick a new status."); return; }
        if (reason == null || reason.trim().isEmpty()) { showError("No reason", "Enter an emergency reason."); return; }
        int assignmentId;
        try { assignmentId = Integer.parseInt(selected.get("assignmentId")); }
        catch (NumberFormatException e) { showError("Bad ID", "Invalid assignment ID."); return; }
        try {
            overrideCtrl.overrideStatus(assignmentId, newStatus, reason.trim());
            showInfo("Status overridden ✓", "Session #" + assignmentId + " → " + newStatus + "\nReason: " + reason.trim());
            onSearchTeacherClicked(event); overrideReasonField.clear();
        } catch (SQLException e) { showError("Override failed", e.getMessage()); }
    }

    @FXML
    public void onSwapRoomClicked(ActionEvent event) {
        Map<String, String> selected = overrideSessionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("No selection", "Select a session row first."); return; }
        String roomSel = swapRoomCombo.getValue();
        if (roomSel == null) { showError("No room", "Pick an available room."); return; }
        int assignmentId;
        try { assignmentId = Integer.parseInt(selected.get("assignmentId")); }
        catch (NumberFormatException e) { showError("Bad ID", "Invalid assignment ID."); return; }
        try {
            overrideCtrl.swapRoom(assignmentId, extractId(roomSel));
            showInfo("Room swapped ✓", "Session #" + assignmentId + "\nNew Room: " + roomSel);
            onSearchTeacherClicked(event);
        } catch (SQLException e) { showError("Swap failed", e.getMessage()); }
    }

    /**
     * Handler for changing the day/time of a session (UC-11 Emergency Override).
     * Allows admin to reschedule a class to a different day and/or time.
     * Applies ALL validation rules: opening/closing time, teacher overlap,
     * max duration, max classes per day, rest gap, room availability.
     */
    @FXML
    public void onChangeScheduleClicked(ActionEvent event) {
        Map<String, String> selected = overrideSessionTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("No selection", "Select a session row first."); return; }
        
        String newDay = overrideNewDayCombo != null ? overrideNewDayCombo.getValue() : null;
        String newStart = overrideNewStartField != null ? trim(overrideNewStartField) : "";
        String newEnd = overrideNewEndField != null ? trim(overrideNewEndField) : "";
        
        // At least one field must be filled
        if ((newDay == null || newDay.isEmpty()) && newStart.isEmpty() && newEnd.isEmpty()) {
            showError("No changes", "Enter at least a new day, start time, or end time."); return;
        }
        
        int assignmentId;
        try { assignmentId = Integer.parseInt(selected.get("assignmentId")); }
        catch (NumberFormatException e) { showError("Bad ID", "Invalid assignment ID."); return; }
        
        // Resolve final day/start/end (merge new values with existing)
        String currentDay = overrideDayCombo.getValue();
        String currentStart = selected.getOrDefault("startTime", "");
        String currentEnd   = selected.getOrDefault("endTime", "");
        if (currentStart.length() > 5) currentStart = currentStart.substring(0, 5);
        if (currentEnd.length()   > 5) currentEnd   = currentEnd.substring(0, 5);
        
        String finalDay   = (newDay != null && !newDay.isEmpty()) ? newDay : currentDay;
        String finalStart = !newStart.isEmpty() ? newStart : currentStart;
        String finalEnd   = !newEnd.isEmpty()   ? newEnd   : currentEnd;
        
        // Parse and validate times
        LocalTime startTime, endTime;
        try {
            startTime = LocalTime.parse(finalStart, DateTimeFormatter.ofPattern("H:mm"));
            endTime   = LocalTime.parse(finalEnd,   DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception ex) { showError("Bad time", "Use HH:mm format for start/end times."); return; }
        if (!endTime.isAfter(startTime)) { showError("Bad times", "End time must be after Start time."); return; }
        
        // Get teacher UID from the search combo
        String teacherSel = overrideTeacherCombo.getValue();
        if (teacherSel == null) { showError("No teacher", "Teacher info not available."); return; }
        String teacherUid = extractId(teacherSel);
        
        try (Connection conn = DBConnection.getConnection()) {
            // ── Rule: University Opening Time ──
            String openingVal = ruleStr(conn, "UNI_OPENING_TIME");
            if (!openingVal.isEmpty()) {
                LocalTime opening = LocalTime.parse(openingVal, DateTimeFormatter.ofPattern("H:mm"));
                if (startTime.isBefore(opening)) {
                    showError("Rule: OPENING TIME",
                            "Class starts at " + finalStart + " but university opens at " + openingVal + ".\n"
                          + "No classes may be scheduled before opening time."); return;
                }
            }
            
            // ── Rule: University Closing Time ──
            String closingVal = ruleStr(conn, "UNI_CLOSING_TIME");
            if (!closingVal.isEmpty()) {
                LocalTime closing = LocalTime.parse(closingVal, DateTimeFormatter.ofPattern("H:mm"));
                if (endTime.isAfter(closing)) {
                    showError("Rule: CLOSING TIME",
                            "Class ends at " + finalEnd + " but university closes at " + closingVal + ".\n"
                          + "No classes may be scheduled after closing time."); return;
                }
            }
            
            // ── Rule: Max Duration ──
            long dur = ChronoUnit.MINUTES.between(startTime, endTime);
            int maxDur = ruleInt(conn, "MAX_DURATION"); if (maxDur <= 0) maxDur = 90;
            if (dur > maxDur) {
                showError("Rule: MAX_DURATION", "Class is " + dur + " min. Max allowed = " + maxDur + " min."); return;
            }
            
            // ── Rule: Teacher Overlap (exclude current assignment) ──
            String overlapSql =
                "SELECT ta.courseCode, ta.sectionName, ta.startTime, ta.endTime, c.courseName "
              + "FROM teacher_assignments ta "
              + "LEFT JOIN courses c ON c.courseCode = ta.courseCode "
              + "WHERE ta.teacherUid = ? AND ta.dayOfWeek = ? "
              + "  AND ta.assignmentId != ? "
              + "  AND ta.startTime < CAST(? AS TIME) "
              + "  AND ta.endTime   > CAST(? AS TIME)";
            try (PreparedStatement ps = conn.prepareStatement(overlapSql)) {
                ps.setString(1, teacherUid);
                ps.setString(2, finalDay);
                ps.setInt(3, assignmentId);
                ps.setString(4, String.format("%02d:%02d:00", endTime.getHour(), endTime.getMinute()));
                ps.setString(5, String.format("%02d:%02d:00", startTime.getHour(), startTime.getMinute()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String es = rs.getString("startTime"), ee = rs.getString("endTime");
                        if (es != null && es.length() > 5) es = es.substring(0, 5);
                        if (ee != null && ee.length() > 5) ee = ee.substring(0, 5);
                        showError("Scheduling Conflict",
                                teacherSel + " already has a class on " + finalDay + ":\n"
                              + rs.getString("courseCode") + " " + rs.getString("courseName")
                              + " [" + rs.getString("sectionName") + "] " + es + "–" + ee
                              + "\n\nCannot reschedule to an overlapping time slot.");
                        return;
                    }
                }
            }
            
            // ── Rule: Max Classes Per Day (exclude current if day changed) ──
            int maxPD = ruleInt(conn, "MAX_CLASSES_PER_DAY"); if (maxPD <= 0) maxPD = 3;
            String countSql = "SELECT COUNT(*) AS cnt FROM teacher_assignments WHERE teacherUid=? AND dayOfWeek=? AND assignmentId!=?";
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                ps.setString(1, teacherUid); ps.setString(2, finalDay); ps.setInt(3, assignmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt("cnt") + 1 > maxPD) {
                        showError("Rule: MAX_CLASSES_PER_DAY",
                                teacherSel + " already has " + rs.getInt("cnt") + " class(es) on " + finalDay
                              + ". Max allowed = " + maxPD + ".\nCannot move this class to " + finalDay + ".");
                        return;
                    }
                }
            }
            
            // ── Rule: Rest Gap (exclude current assignment) ──
            int gap = ruleInt(conn, "REST_GAP");
            if (gap > 0) {
                String gapSql = "SELECT startTime, endTime FROM teacher_assignments WHERE teacherUid=? AND dayOfWeek=? AND assignmentId!=? ORDER BY startTime";
                List<LocalTime[]> existing = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(gapSql)) {
                    ps.setString(1, teacherUid); ps.setString(2, finalDay); ps.setInt(3, assignmentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Time st = rs.getTime("startTime"), et = rs.getTime("endTime");
                            if (st != null && et != null) existing.add(new LocalTime[]{st.toLocalTime(), et.toLocalTime()});
                        }
                    }
                }
                for (LocalTime[] sl : existing) {
                    long g1 = ChronoUnit.MINUTES.between(sl[1], startTime);
                    long g2 = ChronoUnit.MINUTES.between(endTime, sl[0]);
                    if (startTime.isBefore(sl[1]) && endTime.isAfter(sl[0])) {
                        showError("Rule: REST_GAP", "Overlap with existing " + sl[0] + "–" + sl[1] + " on " + finalDay + "."); return;
                    }
                    if (g1 >= 0 && g1 < gap) {
                        showError("Rule: REST_GAP", "Only " + g1 + " min gap after " + sl[1] + ". Need " + gap + " min."); return;
                    }
                    if (g2 >= 0 && g2 < gap) {
                        showError("Rule: REST_GAP", "Only " + g2 + " min gap before " + sl[0] + ". Need " + gap + " min."); return;
                    }
                }
            }
            
            // ── Room availability check for new day/time ──
            String roomId = selected.getOrDefault("roomId", "").split(" ")[0].trim();
            if (!roomId.isEmpty()) {
                String roomSql = "SELECT 1 FROM teacher_assignments WHERE roomId=? AND dayOfWeek=? AND assignmentId!=? AND startTime<? AND endTime>?";
                try (PreparedStatement ps = conn.prepareStatement(roomSql)) {
                    ps.setString(1, roomId); ps.setString(2, finalDay); ps.setInt(3, assignmentId);
                    ps.setString(4, String.format("%02d:%02d:00", endTime.getHour(), endTime.getMinute()));
                    ps.setString(5, String.format("%02d:%02d:00", startTime.getHour(), startTime.getMinute()));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            showError("Room Conflict",
                                    "Room " + roomId + " is already booked on " + finalDay + " at " + finalStart + "–" + finalEnd + ".\n"
                                  + "Use the 'Swap Room' option to pick a different room, or choose a different time.");
                            return;
                        }
                    }
                }
            }
            
        } catch (SQLException e) { showError("Validation error", e.getMessage()); return; }
        
        // ── All rules passed — apply the change ──
        try {
            overrideCtrl.changeSchedule(assignmentId,
                    (newDay != null && !newDay.isEmpty()) ? newDay : null,
                    !newStart.isEmpty() ? newStart : null,
                    !newEnd.isEmpty() ? newEnd : null);
            
            StringBuilder msg = new StringBuilder("Session #" + assignmentId + " rescheduled:");
            if (newDay != null && !newDay.isEmpty()) msg.append("\n  Day: ").append(newDay);
            if (!newStart.isEmpty()) msg.append("\n  Start: ").append(newStart);
            if (!newEnd.isEmpty()) msg.append("\n  End: ").append(newEnd);
            msg.append("\n\nAll rules validated ✓");
            
            showInfo("Schedule changed ✓", msg.toString());
            
            // Clear the fields
            if (overrideNewDayCombo != null) overrideNewDayCombo.getSelectionModel().clearSelection();
            if (overrideNewStartField != null) overrideNewStartField.clear();
            if (overrideNewEndField != null) overrideNewEndField.clear();
            
            // Refresh the table
            onSearchTeacherClicked(event);
        } catch (SQLException e) { showError("Schedule change failed", e.getMessage()); }
    }

    /**
     * Handler for clearing the removed classes list (Tab 2 - Rules).
     * Called when admin acknowledges the removed classes and wants to clear the display.
     */
    @FXML
    public void onClearRemovedClasses(ActionEvent event) {
        removedClassesList.clear();
        if (removedClassesForm != null) {
            removedClassesForm.setVisible(false);
            removedClassesForm.setManaged(false);
        }
        showInfo("Cleared", "Removed classes list has been cleared.\nUse the Timetable Assignment tab to reassign these classes.");
    }

    // ==================================================================
    //  Tab 6 — Substitute Management (UC-08)
    // ==================================================================

    private void initSubstituteTab() {
        subColId.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("assignmentId", "")));
        subColTeacher.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("teacherName", "")));
        subColCourse.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("courseCode", "") + " " + cd.getValue().getOrDefault("courseName", "")));
        subColDay.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("dayOfWeek", "")));
        subColTime.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("startTime", "") + "–" + cd.getValue().getOrDefault("endTime", "")));
        subColStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("sessionStatus", "")));

        subHColId.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("substituteId", "")));
        subHColOrig.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("originalName", "")));
        subHColSub.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("substituteName", "")));
        subHColStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getOrDefault("status", "")));

        subNeededTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == null) { subSelectedLabel.setText("(Select a session from the left table)"); return; }
            subSelectedLabel.setText("Session #" + n.getOrDefault("assignmentId", "?")
                    + " | " + n.getOrDefault("courseCode", "") + " " + n.getOrDefault("courseName", "")
                    + " | " + n.getOrDefault("dayOfWeek", "") + " " + n.getOrDefault("startTime", "") + "–" + n.getOrDefault("endTime", ""));
            subAvailableTeachersCombo.getItems().clear();
            subAvailableTeachersCombo.setPromptText("Click 'Find Available Substitutes'");
        });

        refreshSubNeeded();
        refreshSubHistory();
    }

    @FXML
    public void onRefreshSubNeeded(ActionEvent event) {
        refreshSubNeeded();
        refreshSubHistory();
    }

    private void refreshSubNeeded() {
        try {
            subNeededTable.setItems(FXCollections.observableArrayList(substituteCtrl.getSessionsNeedingSub()));
        } catch (SQLException e) { showError("Load failed", e.getMessage()); }
    }

    private void refreshSubHistory() {
        try {
            subHistoryTable.setItems(FXCollections.observableArrayList(substituteCtrl.getAllSubstituteRequests()));
        } catch (SQLException e) { /* skip */ }
    }

    @FXML
    public void onFindSubstitutesClicked(ActionEvent event) {
        Map<String, String> sel = subNeededTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("No selection", "Select a session from the left table."); return; }
        try {
            List<Map<String, String>> free = substituteCtrl.getFreeTeachers(
                    sel.getOrDefault("courseCode", ""), sel.getOrDefault("dayOfWeek", ""),
                    sel.getOrDefault("startTime", ""), sel.getOrDefault("endTime", ""),
                    sel.getOrDefault("teacherUid", ""));
            ObservableList<String> items = FXCollections.observableArrayList();
            for (Map<String, String> t : free) items.add(t.get("uid") + " — " + t.get("name"));
            subAvailableTeachersCombo.setItems(items);
            subAvailableTeachersCombo.setPromptText(items.isEmpty() ? "No qualified & free teachers found" : items.size() + " teacher(s) available");
            if (items.isEmpty()) showInfo("No substitutes", "No teachers are both qualified AND free for this slot.");
        } catch (SQLException e) { showError("Search failed", e.getMessage()); }
    }

    @FXML
    public void onSendSubstituteRequest(ActionEvent event) {
        Map<String, String> sel = subNeededTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("No selection", "Select a session first."); return; }
        String teacherSel = subAvailableTeachersCombo.getValue();
        if (teacherSel == null) { showError("No substitute", "Select a substitute teacher."); return; }
        String reason = subReasonField.getText();
        if (reason == null || reason.trim().isEmpty()) { showError("No reason", "Enter a reason."); return; }
        int assignmentId;
        try { assignmentId = Integer.parseInt(sel.get("assignmentId")); }
        catch (NumberFormatException e) { showError("Bad ID", "Invalid assignment."); return; }
        try {
            String subId = substituteCtrl.proposeSubstitute(assignmentId, sel.getOrDefault("teacherUid", ""), extractId(teacherSel), reason.trim());
            showInfo("Substitute request sent ✓", "Request " + subId + "\nOriginal: " + sel.getOrDefault("teacherName", "") + "\nSubstitute: " + teacherSel + "\nStatus: PENDING");
            subReasonField.clear();
            refreshSubHistory();
        } catch (SQLException e) { showError("Send failed", e.getMessage()); }
    }

    // ==================================================================
    //  Tab 7 — Room Swap Requests (UC-02 Admin side)
    // ==================================================================

    /**
     * Wires the swapRequestsTable columns, selection listener for room picker,
     * and loads all PENDING swap requests.
     */
    private void initSwapRequestsTab() {
        swapColId.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getOrDefault("requestId", "")));
        swapColSession.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getOrDefault("classId", "")));
        swapColCourse.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getOrDefault("courseId", "")));
        swapColDay.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getOrDefault("dayTime", "")));
        swapColCapacity.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getOrDefault("capacity", "")));
        swapColReason.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getOrDefault("reason", "")));
        swapColStatus.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getOrDefault("status", "")));

        // When admin selects a request row → populate swapRoomPickerCombo with available rooms
        swapRequestsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            swapRoomPickerCombo.getItems().clear();
            if (newRow == null) { swapRoomPickerCombo.setPromptText("Select a request first"); return; }
            String sessionId = newRow.getOrDefault("classId", "");
            int cap = 0;
            try { cap = Integer.parseInt(newRow.getOrDefault("capacity", "0")); } catch (NumberFormatException ignored) {}
            populateSwapRoomPicker(sessionId, cap);
        });

        loadSwapRequests();
    }

    /** Populates swapRoomPickerCombo with rooms available for the given session's time slot. */
    private void populateSwapRoomPicker(String sessionId, int minCapacity) {
        try (Connection c = DBConnection.getConnection()) {
            String dayTime = "", startTime = "", endTime = "";
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT timetableSlot, startTime, endTime FROM class_sessions WHERE sessionId = ?")) {
                ps.setString(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dayTime = rs.getString("timetableSlot");
                        startTime = rs.getString("startTime");
                        endTime = rs.getString("endTime");
                        if (startTime != null && startTime.length() > 5) startTime = startTime.substring(0, 5);
                        if (endTime   != null && endTime.length()   > 5) endTime   = endTime.substring(0, 5);
                    }
                }
            }
            String dayOfWeek = deriveDayFromSlot(dayTime);
            dao.RoomRepository roomRepo = new dao.RoomRepository();
            List<Map<String, String>> rooms = roomRepo.checkRoomAvailability(minCapacity, dayOfWeek, startTime, endTime);
            ObservableList<String> items = FXCollections.observableArrayList();
            for (Map<String, String> r : rooms) {
                items.add(r.get("roomId") + " — " + r.get("roomName") + " (cap " + r.get("capacity") + ")");
            }
            swapRoomPickerCombo.setItems(items);
            swapRoomPickerCombo.setPromptText(items.isEmpty() ? "No rooms available" : items.size() + " room(s) — pick one");
        } catch (SQLException e) {
            swapRoomPickerCombo.setPromptText("Error loading rooms");
        }
    }

    /** Loads all swap requests (PENDING first, then others) into the table. */
    private void loadSwapRequests() {
        ObservableList<Map<String, String>> rows = FXCollections.observableArrayList();
        String sql =
            "SELECT sar.requestId, sar.classId, sar.reason, sar.capacity, sar.status, " +
            "       cs.courseId, cs.timetableSlot AS dayTime, cs.startTime, cs.endTime " +
            "FROM schedule_adjustment_requests sar " +
            "LEFT JOIN class_sessions cs ON cs.sessionId = sar.classId " +
            "ORDER BY CASE sar.status WHEN 'PENDING' THEN 0 ELSE 1 END, sar.createdAt DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement s = c.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                java.util.LinkedHashMap<String, String> row = new java.util.LinkedHashMap<>();
                row.put("requestId", rs.getString("requestId"));
                row.put("classId",   rs.getString("classId"));
                row.put("courseId",  rs.getString("courseId") == null ? "—" : rs.getString("courseId"));
                String slot = rs.getString("dayTime");
                String st   = rs.getString("startTime");
                String et   = rs.getString("endTime");
                if (st != null && et != null) {
                    if (st.length() > 5) st = st.substring(0, 5);
                    if (et.length() > 5) et = et.substring(0, 5);
                    row.put("dayTime", (slot == null ? "" : slot.split(" ")[0]) + " " + st + "–" + et);
                } else {
                    row.put("dayTime", slot == null ? "—" : slot);
                }
                row.put("capacity", rs.getString("capacity"));
                row.put("reason",   rs.getString("reason"));
                row.put("status",   rs.getString("status"));
                rows.add(row);
            }
            swapRequestsTable.setItems(rows);
            if (swapRequestStatusLabel != null)
                swapRequestStatusLabel.setText(rows.isEmpty() ? "No swap requests found."
                        : rows.size() + " request(s) loaded.");
        } catch (SQLException e) {
            if (swapRequestStatusLabel != null)
                swapRequestStatusLabel.setText("DB error: " + e.getMessage());
        }
    }

    /** Admin approves the selected swap request → uses admin-chosen room from
     *  swapRoomPickerCombo, swaps in class_sessions + teacher_assignments,
     *  then DELETEs the request so it disappears from the dashboard. */
    @FXML
    public void onApproveSwapRequest(ActionEvent event) {
        Map<String, String> sel = swapRequestsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("No selection", "Select a swap request from the table."); return; }

        String roomSel = swapRoomPickerCombo.getValue();
        if (roomSel == null) { showError("No room selected", "Pick a room from the 'Assign Room' dropdown."); return; }

        String requestId = sel.get("requestId");
        String sessionId = sel.getOrDefault("classId", "");
        String newRoomId = extractId(roomSel);

        try (Connection c = DBConnection.getConnection()) {
            // Fetch current room for teacher_assignments sync
            String currentRoom = "", dayTime = "", startTime = "", endTime = "";
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT timetableSlot, startTime, endTime, roomNumber FROM class_sessions WHERE sessionId = ?")) {
                ps.setString(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dayTime     = rs.getString("timetableSlot");
                        startTime   = rs.getString("startTime");
                        endTime     = rs.getString("endTime");
                        currentRoom = rs.getString("roomNumber");
                        if (startTime != null && startTime.length() > 5) startTime = startTime.substring(0, 5);
                        if (endTime   != null && endTime.length()   > 5) endTime   = endTime.substring(0, 5);
                    }
                }
            }
            String dayOfWeek = deriveDayFromSlot(dayTime);

            c.setAutoCommit(false);
            try {
                // 1) Swap the room in class_sessions + revert status to UPCOMING
                try (PreparedStatement u = c.prepareStatement(
                        "UPDATE class_sessions SET roomNumber = ?, status = 'UPCOMING' WHERE sessionId = ?")) {
                    u.setString(1, newRoomId); u.setString(2, sessionId); u.executeUpdate();
                }

                // 2) Also update teacher_assignments.roomId (same as Override swap)
                try (PreparedStatement u = c.prepareStatement(
                        "UPDATE teacher_assignments SET roomId = ? WHERE roomId = ? AND dayOfWeek = ? AND startTime = ? AND endTime = ?")) {
                    u.setString(1, newRoomId);
                    u.setString(2, currentRoom == null ? "" : currentRoom);
                    u.setString(3, dayOfWeek);
                    u.setString(4, startTime);
                    u.setString(5, endTime);
                    u.executeUpdate();
                }

                // 3) DELETE the request from the table (clear it from admin view)
                try (PreparedStatement d = c.prepareStatement(
                        "DELETE FROM schedule_adjustment_requests WHERE requestId = ?")) {
                    d.setString(1, requestId); d.executeUpdate();
                }

                c.commit();

                swapRequestStatusLabel.setText("✓ Approved + assigned room " + newRoomId);
                swapRequestStatusLabel.setStyle("-fx-text-fill: #047857; -fx-font-weight: bold;");
                showInfo("Swap Approved ✓",
                        "Session " + sessionId + " moved to room " + roomSel + ".\n"
                      + "Request " + requestId + " has been cleared.");
                swapRoomPickerCombo.getItems().clear();
                loadSwapRequests();

            } catch (SQLException ex) {
                c.rollback(); throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            swapRequestStatusLabel.setText("✗ Error: " + e.getMessage());
            swapRequestStatusLabel.setStyle("-fx-text-fill: #DC2626;");
            showError("Approve failed", e.getMessage());
        }
    }

    /** Derives a canonical day name from a timetableSlot string (e.g. "Mon 09:00" → "Monday"). */
    private static String deriveDayFromSlot(String slot) {
        if (slot == null) return "";
        String s = slot.trim().toLowerCase();
        if (s.startsWith("mon")) return "Monday";
        if (s.startsWith("tue")) return "Tuesday";
        if (s.startsWith("wed")) return "Wednesday";
        if (s.startsWith("thu")) return "Thursday";
        if (s.startsWith("fri")) return "Friday";
        if (s.startsWith("sat")) return "Saturday";
        if (s.startsWith("sun")) return "Sunday";
        return slot;
    }

    /** Admin rejects the selected swap request → reverts session status + DELETEs the request. */
    @FXML
    public void onRejectSwapRequest(ActionEvent event) {
        Map<String, String> sel = swapRequestsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showError("No selection", "Select a swap request from the table."); return; }
        String requestId = sel.get("requestId");

        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 1) Revert the session status back to UPCOMING
                try (PreparedStatement r = c.prepareStatement(
                        "UPDATE class_sessions SET status = 'UPCOMING' WHERE sessionId = ? AND status = 'SWAP_PENDING'")) {
                    r.setString(1, sel.getOrDefault("classId", ""));
                    r.executeUpdate();
                }
                // 2) DELETE the request so it's cleared from the dashboard
                try (PreparedStatement d = c.prepareStatement(
                        "DELETE FROM schedule_adjustment_requests WHERE requestId = ?")) {
                    d.setString(1, requestId);
                    d.executeUpdate();
                }
                c.commit();

                swapRequestStatusLabel.setText("✗ Request " + requestId + " rejected and cleared.");
                swapRequestStatusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                showInfo("Swap Request Rejected",
                        "Request " + requestId + " has been rejected and removed.\n"
                      + "Session status reverted to UPCOMING.");
                swapRoomPickerCombo.getItems().clear();
                loadSwapRequests();
            } catch (SQLException ex) {
                c.rollback(); throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            swapRequestStatusLabel.setText("✗ Error: " + e.getMessage());
            swapRequestStatusLabel.setStyle("-fx-text-fill: #DC2626;");
            showError("Reject failed", e.getMessage());
        }
    }

    /** Refreshes the swap requests table. */
    @FXML
    public void onRefreshSwapRequests(ActionEvent event) {
        loadSwapRequests();
        swapRequestStatusLabel.setText("Refreshed.");
        swapRequestStatusLabel.setStyle("-fx-text-fill: #475569;");
    }

    // ==================================================================
    //  Logout
    // ==================================================================

    @FXML
    public void onLogoutClicked(ActionEvent event) {
        Alert confirm = new Alert(AlertType.CONFIRMATION, "Log out?", ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(ch -> {
            if (ch == ButtonType.OK) {
                UserSession.getInstance().clear();
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/ui/Login.fxml"));
                    Stage stage = (Stage) logoutButton.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("SAPCIS – Login"); stage.centerOnScreen();
                } catch (IOException ex) { showError("Nav failed", ex.getMessage()); }
            }
        });
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    /** Converts a java.sql.Time to minutes since midnight (timezone-safe). */
    private static int toMinsSinceMidnight(Time t) {
        @SuppressWarnings("deprecation")
        int h = t.getHours(), m = t.getMinutes();
        return h * 60 + m;
    }

    private static String extractId(String v) {
        if (v == null) return "";
        int i = v.indexOf(" — ");
        return i > 0 ? v.substring(0, i).trim() : v.trim();
    }

    private static String trim(TextField tf) {
        return tf.getText() == null ? "" : tf.getText().trim();
    }

    private void showInfo(String h, String b) {
        Alert a = new Alert(AlertType.INFORMATION, b, ButtonType.OK);
        a.setHeaderText(h); a.setTitle("SAPCIS");
        a.getDialogPane().setStyle("-fx-font-size:13px;"); a.showAndWait();
    }

    private void showError(String h, String b) {
        Alert a = new Alert(AlertType.ERROR, b, ButtonType.OK);
        a.setHeaderText(h); a.setTitle("SAPCIS");
        a.getDialogPane().setStyle("-fx-font-size:13px;"); a.showAndWait();
    }
}
