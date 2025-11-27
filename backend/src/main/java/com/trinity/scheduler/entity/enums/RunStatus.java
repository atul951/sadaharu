package com.trinity.scheduler.entity.enums;

/**
 * Enumeration of possible statuses for a scheduling run.
 *
 * @author Atul Kumar
 */
public enum RunStatus {
    /**
     * Schedule generation is currently in progress.
     */
    IN_PROGRESS,

    /**
     * Schedule generation completed successfully.
     */
    COMPLETED,

    /**
     * Schedule generation failed due to an error.
     */
    FAILED,

    /**
     * Schedule generation was manually cancelled.
     */
    CANCELLED
}
