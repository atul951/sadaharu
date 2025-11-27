package com.trinity.scheduler.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of enrollment validation.
 */
@Getter
public class EnrollmentValidationResult {
    private boolean valid;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
