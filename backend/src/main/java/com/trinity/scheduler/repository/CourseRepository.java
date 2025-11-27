package com.trinity.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.trinity.scheduler.entity.Course;

import java.util.List;

/**
 * Repository for Course entities.
 *
 * @author Atul Kumar
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Finds courses by semester order (1=Fall, 2=Spring).
     */
    List<Course> findBySemesterOrder(Integer semesterOrder);

    /**
     * Counts credits by course ids.
     */
    @Query("SELECT SUM(c.credits) FROM Course c WHERE c.id IN :courseIds")
    Integer countCreditsByCourseIdsIn(@Param("courseIds") List<Long> courseIds);
}
