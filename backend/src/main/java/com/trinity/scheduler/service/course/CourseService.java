package com.trinity.scheduler.service.course;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.trinity.scheduler.repository.CourseRepository;

import java.util.List;

/**
 * Service class for Course operations.
 *
 * @author Atul Kumar
 */
@Service
public class CourseService {
    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * Retrieves total credits for the given course ids.
     *
     * @param courseIds list of course IDs
     * @return total credits for the courses
     */
    public Integer getCoursesCredits(List<Long> courseIds) {
        return courseRepository.countCreditsByCourseIdsIn(courseIds);
    }
}
