package com.trinity.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-only entity for the students table (existing in database).
 *
 * @author Atul Kumar
 */
@Getter
@Setter
@Entity
@Table(name = "students")
public class Student {

    @Id
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "grade_level")
    private Integer gradeLevel;

    @Column(name = "enrollment_year")
    private Integer enrollmentYear;

    @Column(name = "expected_graduation_year")
    private Integer expectedGraduationYear;

    @Column(name = "status", length = 20)
    private String status;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
