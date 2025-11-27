package com.trinity.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.trinity.scheduler.entity.StudentCourseHistory;

import java.util.List;

/**
 * Repository for StudentCourseHistory entities.
 *
 * @author Atul Kumar
 */
@Repository
public interface StudentCourseHistoryRepository extends JpaRepository<StudentCourseHistory, Long> {

    /**
     * Finds all course history for a student.
     */
    List<StudentCourseHistory> findByStudentId(Long studentId);

    /**
     * Checks if a student has passed a specific course.
     */
    @Query("""
            SELECT EXISTS (
                SELECT 1
                FROM StudentCourseHistory h
                WHERE h.studentId = :studentId
                AND h.courseId = :courseId
                AND h.status = 'passed'
            )
            """)
    boolean hasPassedCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
