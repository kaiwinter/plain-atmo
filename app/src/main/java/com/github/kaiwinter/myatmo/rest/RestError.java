package com.github.kaiwinter.myatmo.rest;

public class RestError {

    /**
     * A parsed REST error body of Netatmo related REST responses.
     * <pre>
     *  {
     *     "error": {
     *         "code": int,
     *         "message": "string"     //optional
     *     }
     *  }
     * </pre>
     */
    public static class ApiError {
        public Error error;

        public static class Error {
            public Integer code;
            public String message;
        }
    }

    /**
     * A parsed REST error body of Oauth2 related REST responses.
     * <pre>
     *  {
     *     "error": "Request error type",
     *     "error_description": "Request error desc"
     *  }
     * </pre>
     */
    public static class OauthError {

        public String error;
        public String errorDescription;

    }
}
