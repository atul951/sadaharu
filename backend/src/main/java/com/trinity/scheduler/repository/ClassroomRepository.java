package com.trinity.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trinity.scheduler.entity.Classroom;

import java.util.List;

/**
 * Repository for Classroom entities.
 * 
 * @author Atul Kumar
 */
@Repository
public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    /**
     * Finds classrooms by room type.
     */
    List<Classroom> findByRoomTypeId(Long roomTypeId);
}

