package exception;

public class InvalidReportConfigException extends RuntimeException {
    public InvalidReportConfigException(String message) {
        super(message);
    }
}
