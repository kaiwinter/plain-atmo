package com.github.kaiwinter.myatmo.util;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

public class ExceptionUtil {
    /**
     * Unwraps an exception which is thrown by the netatmo-api library.
     *
     * @param e the exception
     * @return a String representation for the exception
     */
    public static String unwrapException(Throwable e) {
        Throwable cause = e.getCause();
        if (cause instanceof OAuthProblemException) {
            return ((OAuthProblemException) e.getCause()).getError();
        } else if (cause instanceof OAuthSystemException) {
            return ((OAuthSystemException) e.getCause()).getMessage();
        }
        return e.getMessage();
    }
}
