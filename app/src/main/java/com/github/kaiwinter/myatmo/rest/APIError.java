package com.github.kaiwinter.myatmo.rest;

public class APIError {

    public Error error;

    public class Error {
        public Integer code;
        public String message;
    }
}
