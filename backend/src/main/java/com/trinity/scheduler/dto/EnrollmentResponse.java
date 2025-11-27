package com.trinity.scheduler.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for enrollment operations.
 *
 * @author Atul Kumar
 */
public class EnrollmentResponse {
    private Long id;
    private Long studentId;
    private Long sectionId;
    private String status;
    private LocalDateTime enrolledAt;
    private String message;

    public EnrollmentResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
