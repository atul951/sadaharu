package com.trinity.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-only entity for the student_course_history table (existing in database).
 * Tracks student's past course completions with pass/fail status.
 * Used for prerequisite validation.
 *
 * @author Atul Kumar
 */
@Getter
@Setter
@Entity
@Table(name = "student_course_history")
public class StudentCourseHistory {

    @Id
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "semester_id")
    private Long semesterId;

    private String status; // 'passed' or 'failed'

    public boolean isPassed() {
        return "passed".equalsIgnoreCase(status);
    }
}
