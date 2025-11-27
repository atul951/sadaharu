package com.trinity.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trinity.scheduler.entity.Semester;

/**
 * Repository for Semester entities.
 *
 * @author Atul Kumar
 */
@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
}
