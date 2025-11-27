package com.trinity.scheduler.entity.enums;

/**
 * Enumeration of possible statuses for a course section.
 * Represents the lifecycle of a section from creation through completion.
 *
 * @author Atul Kumar
 */
public enum SectionStatus {
    /**
     * Section created but not yet assigned teacher, room, or time slots.
     * Initial state after demand analysis creates sections.
     */
    UNSCHEDULED,

    /**
     * Section fully scheduled with teacher, room, and time slots assigned.
     * Ready for student enrollment.
     */
    SCHEDULED,

    /**
     * Semester in progress, students are enrolled and attending.
     */
    ACTIVE,

    /**
     * Semester completed, final grades recorded.
     */
    COMPLETED,

    /**
     * Section cancelled (e.g., low enrollment, teacher unavailable).
     */
    CANCELLED
}
