package com.trinity.scheduler.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for student schedule information.
 *
 * @author Atul Kumar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentScheduleResponse {
    private Long studentId;
    private Long semesterId;
    private LocalDate semesterStartDate;
    private LocalDate semesterEndDate;
    private Integer enrolledCourses;
    private List<SectionDTO> sections;
}
