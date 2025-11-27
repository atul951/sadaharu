package com.trinity.scheduler.exception;

/**
 * Exception thrown when enrollment validation fails.
 */
public class EnrollmentException extends RuntimeException {
    public EnrollmentException(String message) {
        super(message);
    }
}
