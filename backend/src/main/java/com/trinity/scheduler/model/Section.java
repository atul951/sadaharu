package com.trinity.scheduler.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import com.trinity.scheduler.entity.Classroom;
import com.trinity.scheduler.entity.Course;
import com.trinity.scheduler.entity.SectionTimeslot;
import com.trinity.scheduler.entity.Teacher;
import com.trinity.scheduler.entity.enums.SectionStatus;

/**
 * Model class representing a Course Section.
 */
@Getter
@Setter
public class Section {
    private Long id;
    private Integer sectionNumber;
    private Course course;
    private Teacher teacher;
    private Classroom classroom;
    private Integer capacity;
    private Integer hoursPerWeek;
    private SectionStatus status;
    private List<SectionTimeslot> timeslots;
}
