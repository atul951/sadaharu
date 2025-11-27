package com.trinity.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for timeslot information.
 *
 * @author Atul Kumar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeslotDTO {
    private Integer dayOfWeek;
    private String dayName;
    private String startTime;
    private String endTime;
}
