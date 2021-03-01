package com.github.kaiwinter.myatmo.rest;

public class APIError {

    public Error error;

    public static class Error {
        public Integer code;
        public String message;
    }
}
