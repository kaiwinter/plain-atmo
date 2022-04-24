package com.github.kaiwinter.myatmo.login;

public class ServiceError {
    private final ErrorType errorType;
    private final String message;
    ServiceError(ErrorType errorType, String message) {
        this.errorType = errorType;
        this.message = message;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public enum ErrorType {
        /**
         * User has to login again.
         */
        NEED_TO_RELOGIN,
        /**
         * Just show a message to the user.
         */
        MESSAGE_ONLY
    }
}