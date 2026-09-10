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
        if ("Classroom Utilization".equalsIgnoreCase(reportType)) {
            // Strategy A
            Object stats = computeUtilizationStats();
            return renderChart(stats);
        } else if ("Faculty Delays".equalsIgnoreCase(reportType)) {
            // Strategy B (alt SSD)
            Object stats = computeDelayStats();
            return renderDelayStatistics(stats);
        }
        throw new exception.UnsupportedReportTypeException(reportType);
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
