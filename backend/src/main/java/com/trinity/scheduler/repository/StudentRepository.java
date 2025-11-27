package com.trinity.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.trinity.scheduler.entity.Student;

import java.util.List;

/**
 * Repository for Student entities.
 *
 * @author Atul Kumar
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Finds active students by grade level and enrollment year.
     */
    @Query("SELECT s FROM Student s WHERE s.gradeLevel BETWEEN :gradeLevelMin AND :gradeLevelMax " +
            "AND s.enrollmentYear <= :year AND s.expectedGraduationYear >= :year AND status = :status")
    List<Student> findByGradeLevelAndYearAndStatus(Integer gradeLevelMin, Integer gradeLevelMax, Integer year, String status);
}
