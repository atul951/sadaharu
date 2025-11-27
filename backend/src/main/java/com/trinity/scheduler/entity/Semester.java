package com.trinity.scheduler.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

import com.trinity.scheduler.converters.LocalDateConverter;

import lombok.Getter;
import lombok.Setter;

/**
 * Read-only entity for the semesters table (existing in database).
 *
 * @author Atul Kumar
 */
@Getter
@Setter
@Entity
@Table(name = "semesters")
public class Semester {

    @Id
    private Long id;

    private String name;
    private Integer year;

    @Column(name = "order_in_year")
    private Integer orderInYear;

    @Column(name = "start_date")
    @Convert(converter = LocalDateConverter.class)
    private LocalDate startDate;

    @Column(name = "end_date")
    @Convert(converter = LocalDateConverter.class)
    private LocalDate endDate;

    @Column(name = "is_active")
    private Boolean isActive;
}
