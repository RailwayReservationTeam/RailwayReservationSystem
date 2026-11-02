package railway.exception;

/**
 * Thrown when a seat cannot be allocated in a coach class and the
 * waiting list for that class cannot accept the passenger either.
 */
public class SeatNotAvailableException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String classCode;

    public SeatNotAvailableException(String classCode, String message) {
        super(message);
        this.classCode = classCode;
    }

    public String getClassCode() {
        return classCode;
    }
}
