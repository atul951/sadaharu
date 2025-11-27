package com.trinity.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-only entity for the specializations table (existing in database).
 *
 * @author Atul Kumar
 */
@Getter
@Setter
@Entity
@Table(name = "specializations")
public class Specialization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;
}
