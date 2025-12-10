package cz.oluwagbemiga.eutax.exceptions;

/**
 * Signals failures that occur while authenticating the user before starting the app.
 */
public class LoginException extends RuntimeException {

    public LoginException(String message) {
        super(message);
    }

    public LoginException(String message, Throwable cause) {
        super(message, cause);
    }
}

