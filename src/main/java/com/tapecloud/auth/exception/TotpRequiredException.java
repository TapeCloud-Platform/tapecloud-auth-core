package com.tapecloud.auth.exception;

public class TotpRequiredException extends RuntimeException {

    public TotpRequiredException(String message) {
        super(message);
    }
}
