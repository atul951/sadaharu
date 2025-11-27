package com.trinity.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-only entity for the courses table (existing in database).
 * Represents course definitions with prerequisites, credit hours,
 * and grade level requirements.
 *
 * @author Atul Kumar
 */
@Getter
@Setter
@Entity
@Table(name = "courses")
public class Course {

    @Id
    private Long id;

    private String code;
    private String name;
    private Integer credits;

    @Column(name = "hours_per_week")
    private Integer hoursPerWeek;

    @Column(name = "specialization_id")
    private Long specializationId;

    @Column(name = "prerequisite_id")
    private Long prerequisiteId;

    @Column(name = "semester_order")
    private Integer semesterOrder;

    @Column(name = "grade_level_min")
    private Integer gradeLevelMin;

    @Column(name = "grade_level_max")
    private Integer gradeLevelMax;
}
