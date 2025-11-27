package com.trinity.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-only entity for the classrooms table (existing in database).
 *
 * @author Atul Kumar
 */
@Getter
@Setter
@Entity
@Table(name = "classrooms")
public class Classroom {

    @Id
    private Long id;

    private String name;

    @Column(name = "room_type_id")
    private Long roomTypeId;

    private Integer capacity;
}
