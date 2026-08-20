package com.alwaysmoveforward.configurationmanager.exceptions;

/** Thrown when a request would violate a uniqueness rule (e.g. duplicate name). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

