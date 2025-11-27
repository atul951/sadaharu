package com.trinity.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.trinity.scheduler.entity.StudentEnrollment;

import java.util.List;
import java.util.Optional;

/**
 * Repository for StudentEnrollment entities.
 * Provides data access methods for student enrollments including:
 * - Finding enrollments by student or section
 * - Checking enrollment status
 * - Detecting time conflicts
 * - Counting course load
 *
 * @author Atul Kumar
 */
@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    /**
     * Finds a student's enrollment in a specific section.
     *
     * @param studentId the student ID
     * @param sectionId the section ID
     * @return optional enrollment
     */
    @Query("SELECT e FROM StudentEnrollment e WHERE e.studentId = :studentId AND e.section.id = :sectionId")
    Optional<StudentEnrollment> findByStudentIdAndSectionId(
            @Param("studentId") Long studentId,
            @Param("sectionId") Long sectionId
    );

    /**
     * Counts active enrollments for a student in a semester.
     * Used to enforce the 5-course maximum.
     *
     * @param studentId  the student ID
     * @param semesterId the semester ID
     * @return count of active enrollments
     */
    @Query("""
            SELECT COUNT(e)
            FROM StudentEnrollment e
            JOIN e.section cs
            WHERE e.studentId = :studentId
            AND cs.semesterId = :semesterId
            AND e.status IN ('ENROLLED', 'WAITLISTED')
            """)
    long countActiveEnrollments(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId
    );

    /**
     * Finds all active enrollments for a student in a semester.
     *
     * @param studentId  the student ID
     * @param semesterId the semester ID
     * @return list of enrollments
     */
    @Query("""
            SELECT e
            FROM StudentEnrollment e
            JOIN e.section cs
            WHERE e.studentId = :studentId
            AND cs.semesterId = :semesterId
            AND e.status IN ('ENROLLED', 'WAITLISTED')
            """)
    List<StudentEnrollment> findActiveEnrollmentsBySemester(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId
    );

    /**
     * Checks if a student has a time conflict with a proposed section.
     * Returns true if the student is already enrolled in a section that
     * has overlapping time slots.
     *
     * @param studentId the student ID
     * @param dayOfWeek the day of week
     * @param startTime the start time (as string HH:MM:SS)
     * @param endTime   the end time (as string HH:MM:SS)
     * @return true if conflict exists
     */
    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
            FROM student_enrollments se
            JOIN course_sections cs ON se.section_id = cs.id
            JOIN section_timeslots st ON cs.id = st.section_id
            WHERE se.student_id = :studentId
            AND se.status = 'ENROLLED'
            AND st.day_of_week = :dayOfWeek
            AND st.start_time < :endTime
            AND st.end_time > :startTime
            """, nativeQuery = true)
    Integer hasTimeConflict(
            @Param("studentId") Long studentId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime
    );

    /**
     * Checks if a student is already enrolled in a course (any section) in a semester.
     * Prevents duplicate enrollment in the same course.
     *
     * @param studentId  the student ID
     * @param courseId   the course ID
     * @param semesterId the semester ID
     * @return true if already enrolled
     */
    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM StudentEnrollment e
            JOIN e.section cs
            WHERE e.studentId = :studentId
            AND cs.courseId = :courseId
            AND cs.semesterId = :semesterId
            AND e.status IN ('ENROLLED', 'WAITLISTED')
            """)
    boolean isEnrolledInCourse(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("semesterId") Long semesterId
    );
}
