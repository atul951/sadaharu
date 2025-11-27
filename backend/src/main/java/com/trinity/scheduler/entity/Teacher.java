package com.trinity.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-only entity for the teachers table (existing in database).
 *
 * @author Atul Kumar
 */
@Getter
@Setter
@Entity
@Table(name = "teachers")
public class Teacher {

    @Id
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "specialization_id")
    private Long specializationId;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
