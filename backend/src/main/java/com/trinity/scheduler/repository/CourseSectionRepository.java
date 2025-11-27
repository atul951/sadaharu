package com.trinity.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.trinity.scheduler.entity.CourseSection;
import com.trinity.scheduler.entity.enums.SectionStatus;

import java.util.List;

/**
 * Repository for CourseSection entities.
 * Provides data access methods for course sections including:
 * - Finding sections by semester, course, teacher, or classroom
 * - Detecting scheduling conflicts
 * - Querying section availability
 *
 * @author Atul Kumar
 */
@Repository
public interface CourseSectionRepository extends JpaRepository<CourseSection, Long> {

    /**
     * Finds all sections for a given semester.
     *
     * @param semesterId the semester ID
     * @return list of sections
     */
    List<CourseSection> findBySemesterId(Long semesterId);

    /**
     * Finds all sections for a given semester with a specific status.
     *
     * @param semesterId the semester ID
     * @param status     the section status
     * @return list of sections
     */
    List<CourseSection> findBySemesterIdAndStatus(Long semesterId, SectionStatus status);

    /**
     * Finds all sections for a given course in a semester.
     *
     * @param courseId   the course ID
     * @param semesterId the semester ID
     * @return list of sections
     */
    List<CourseSection> findByCourseIdAndSemesterId(Long courseId, Long semesterId);

    /**
     * Counts the number of sections for a course in a semester.
     *
     * @param courseId   the course ID
     * @param semesterId the semester ID
     * @return count of sections
     */
    long countByCourseIdAndSemesterId(Long courseId, Long semesterId);

    /**
     * Deletes all sections for a given semester.
     * Used when regenerating a schedule.
     *
     * @param semesterId the semester ID
     */
    @Modifying
    @Query("DELETE FROM CourseSection cs WHERE cs.semesterId = :semesterId")
    void deleteBySemesterId(@Param("semesterId") Long semesterId);
}
