package ui;

import controller.ReportController;
import exception.InvalidReportConfigException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

public class AdminReportUIController {

    @FXML
    private TextField reportTypeField;
    @FXML
    private TextField dateRangeField;
    @FXML
    private Label validationError;

    @FXML
    private void onGenerateReportClicked(ActionEvent event) {
        ReportController rc = new ReportController();
        try {
            rc.selectReport(reportTypeField.getText(), dateRangeField.getText());
            rc.generateReport(); // GoF Strategy runs inside ReportGenerator
            validationError.setText("Report generated successfully.");
        } catch (InvalidReportConfigException e) {
            validationError.setText(e.getMessage());
        } catch (Exception e) {
            validationError.setText("Error: " + e.getMessage());
        }
    }
}
