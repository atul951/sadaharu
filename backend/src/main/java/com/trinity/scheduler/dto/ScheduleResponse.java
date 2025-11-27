package com.trinity.scheduler.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for schedule generation.
 *
 * @author Atul Kumar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    private Long semesterId;
    private LocalDate semesterStartDate;
    private LocalDate semesterEndDate;
    private int sectionsCreated;
    private int sectionsScheduled;
    private int sectionsFailed;
    private List<SectionDTO> sections;
    private Map<String, Object> statistics;
}
