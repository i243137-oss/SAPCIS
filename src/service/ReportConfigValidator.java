package service;

/**
 * // GRASP Pattern: Pure Fabrication
 * Used in UC-12
 */
public class ReportConfigValidator {

    public boolean validateInputs(String reportType, String dateRange) {
        System.out.println("Validating report inputs: " + reportType + ", " + dateRange);
        return checkDateRangeValidity(dateRange) && (reportType != null && !reportType.isEmpty());
    }

    public boolean checkDateRangeValidity(String dateRange) {
        System.out.println("Checking date range validity for: " + dateRange);
        return dateRange != null && !dateRange.isEmpty();
    }
}
