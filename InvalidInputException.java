package railway.exception;

/** Thrown when user input or a stored record fails validation. */
public class InvalidInputException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
