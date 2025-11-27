package com.trinity.scheduler.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for course section information.
 * 
 * @author Atul Kumar
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SectionDTO {
    private Long id;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Integer sectionNumber;
    private String teacherName;
    private String classroomName;
    private Integer hoursPerWeek;
    private List<TimeslotDTO> timeslots;
//    private int enrolledCount;
    private int capacity;
    private String status;
}

