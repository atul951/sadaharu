package com.trinity.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trinity.scheduler.entity.Specialization;

/**
 * Repository for Specialization entities.
 * 
 * @author Atul Kumar
 */
@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, Long> {
}
