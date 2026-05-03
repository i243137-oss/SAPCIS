package exception;

public class RoomAlreadyBookedException extends Exception {
    public RoomAlreadyBookedException(String message) {
        super(message);
    }
}
