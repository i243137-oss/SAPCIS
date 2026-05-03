package exception;

public class UnsupportedReportTypeException extends RuntimeException {
    public UnsupportedReportTypeException(String reportType) {
        super("Unsupported report type: " + reportType);
    }
}
