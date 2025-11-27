package com.trinity.scheduler.entity.enums;

/**
 * Enumeration of possible enrollment statuses.
 * Represents the lifecycle of a student's enrollment in a course section.
 *
 * @author Atul Kumar
 */
public enum EnrollmentStatus {
    /**
     * Student is actively enrolled in the section.
     * Counts toward section capacity and student's course load.
     */
    ENROLLED,

    /**
     * Student is on the waiting list.
     * Section is full, student will be enrolled if a spot opens.
     */
    WAITLISTED,

    /**
     * Student has withdrawn from the course.
     * No longer counts toward capacity or course load.
     */
    DROPPED,

    /**
     * Course has been completed.
     * Final grade recorded in student_course_history.
     */
    COMPLETED
}
