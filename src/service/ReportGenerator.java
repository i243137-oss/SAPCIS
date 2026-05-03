package service;

/**
 * // GRASP Pattern: Pure Fabrication + GoF Strategy
 * Used in UC-12
 */
public class ReportGenerator {

    /**
     * // GoF Pattern: Strategy
     * Strategy selected by reportType string
     */
    public Object buildReport(String reportType, Object historicalDataSet) throws Exception {
        if (reportType.equals("Classroom Utilization")) {
            // Strategy A
            Object stats = computeUtilizationStats();
            return renderChart(stats);
        } else if (reportType.equals("Faculty Delays")) {
            // Strategy B (alt SSD)
            Object stats = computeDelayStats();
            return renderDelayStatistics(stats);
        }
        throw new Exception("UnsupportedReportTypeException: " + reportType); // Matches the exception logic in plan
    }

    public Object computeUtilizationStats() {
        System.out.println("Computing utilization stats...");
        return new Object();
    }

    public Object computeDelayStats() {
        System.out.println("Computing delay stats...");
        return new Object();
    }

    public Object renderChart(Object stats) {
        System.out.println("Rendering utilization chart...");
        return "Utilization Chart Object";
    }

    public Object renderDelayStatistics(Object stats) {
        System.out.println("Rendering delay statistics...");
        return "Delay Statistics Object";
    }
}
