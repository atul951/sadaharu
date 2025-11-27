package com.trinity.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.trinity.scheduler.entity.SectionTimeslot;

import java.time.LocalTime;
import java.util.List;

/**
 * Repository for SectionTimeslot entities.
 * Provides data access methods for section timeslots including:
 * - Finding timeslots by section
 * - Detecting time conflicts for teachers and classrooms
 * - Querying timeslots by day of week
 *
 * @author Atul Kumar
 */
@Repository
public interface SectionTimeslotRepository extends JpaRepository<SectionTimeslot, Long> {

    /**
     * Finds all timeslots for a given section.
     *
     * @param sectionId the section ID
     * @return list of timeslots
     */
    @Query("SELECT st FROM SectionTimeslot st WHERE st.section.id = :sectionId")
    List<SectionTimeslot> findBySectionId(@Param("sectionId") Long sectionId);

    /**
     * Checks if a teacher has a conflict at a specific time.
     * Returns true if the teacher is already scheduled during the given time window.
     *
     * @param teacherId   the teacher ID
     * @param classroomId the classroom ID
     * @param dayOfWeek   the day of week
     * @param startTime   the start time
     * @param endTime     the end time
     * @return true if conflict exists
     */
    @Query("""
            SELECT CASE WHEN COUNT(st) > 0 THEN true ELSE false END
            FROM SectionTimeslot st
            JOIN st.section cs
            WHERE (cs.teacherId = :teacherId OR cs.classroomId = :classroomId)
            AND st.dayOfWeek = :dayOfWeek
            AND st.startTime < :endTime
            AND st.endTime > :startTime
            AND cs.status != 'CANCELLED'
            """)
    boolean hasTeacherOrRoomConflict(
            @Param("teacherId") Long teacherId,
            @Param("classroomId") Long classroomId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * Calculates total teaching hours for a teacher on a specific day.
     * Used to enforce the 4-hour daily limit.
     *
     * @param teacherId the teacher ID
     * @param dayOfWeek the day of week
     * @return total hours (as decimal, e.g., 3.5)
     */
    @Query(value = """
            SELECT COALESCE(SUM(
                (strftime('%s', st.end_time) - strftime('%s', st.start_time)) / 3600.0
            ), 0)
            FROM section_timeslots st
            JOIN course_sections cs ON st.section_id = cs.id
            WHERE cs.teacher_id = :teacherId
            AND st.day_of_week = :dayOfWeek
            AND cs.status != 'CANCELLED'
            """, nativeQuery = true)
    Double calculateTeacherDailyHours(
            @Param("teacherId") Long teacherId,
            @Param("dayOfWeek") Integer dayOfWeek
    );
}
