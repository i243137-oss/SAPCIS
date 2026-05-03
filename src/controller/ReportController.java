package controller;

import service.ReportConfigValidator;
import service.SessionRepository;
import service.ReportGenerator;
import exception.InvalidReportConfigException;

/**
 * // GRASP Pattern: Controller + Pure Fabrication (ReportConfigValidator, ReportGenerator, SessionRepository) + GoF Strategy Pattern
 * Used in UC-12
 */
public class ReportController {
    private ReportConfigValidator reportConfigValidator;
    private SessionRepository sessionRepository;
    private ReportGenerator reportGenerator;
    
    private String pendingReportType;
    private String pendingDateRange;

    public ReportController() {
        this.reportConfigValidator = new ReportConfigValidator();
        this.sessionRepository = new SessionRepository();
        this.reportGenerator = new ReportGenerator();
    }

    public void openAnalyticsTab() {
        System.out.println("ReportController: Analytics tab opened.");
    }

    public void selectReport(String reportType, String dateRange) {
        this.pendingReportType = reportType;
        this.pendingDateRange = dateRange;
        System.out.println("ReportController: Report selected. Waiting for confirmation.");
    }

    public void generateReport() throws Exception {
        // Pure Fabrication: isolates input validation responsibility
        boolean valid = reportConfigValidator.validateInputs(pendingReportType, pendingDateRange);
        if (!valid) {
            throw new InvalidReportConfigException("Invalid date range or report type.");
        }
        
        Object historicalDataSet = sessionRepository.fetchHistoricalData(pendingReportType, pendingDateRange);
        
        // GoF Strategy: reportType selects the algorithm inside buildReport()
        Object report = reportGenerator.buildReport(pendingReportType, historicalDataSet);
        
        System.out.println("ReportController: Report generated successfully.");
    }
}
