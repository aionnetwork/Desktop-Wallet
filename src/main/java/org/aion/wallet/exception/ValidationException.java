package org.aion.wallet.exception;

public class ValidationException extends Exception{

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(Throwable cause) {
        super(cause);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        return getCause() != null ? getCause().getMessage() : super.getMessage();
    }
}
