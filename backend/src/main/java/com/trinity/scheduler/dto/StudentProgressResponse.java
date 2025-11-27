package com.trinity.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for student academic progress information.
 *
 * @author Atul Kumar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProgressResponse {
    private Long studentId;
    private String studentName;
    private Integer gradeLevel;
    private Integer creditsEarned;
    private Integer creditsRequired;
    private Integer creditsRemaining;
    private Integer coursesCompleted;
    private Integer coursesPassed;
    private Integer coursesFailed;
    private Integer coursesEnrolled;
    private Boolean onTrackForGraduation;
}
