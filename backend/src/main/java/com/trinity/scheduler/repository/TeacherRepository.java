package com.trinity.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trinity.scheduler.entity.Teacher;

import java.util.List;

/**
 * Repository for Teacher entities.
 * 
 * @author Atul Kumar
 */
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    /**
     * Finds teachers by specialization.
     */
    List<Teacher> findBySpecializationId(Long specializationId);
}

