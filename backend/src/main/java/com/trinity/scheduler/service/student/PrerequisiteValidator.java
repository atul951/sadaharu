package com.trinity.scheduler.service.student;

import org.springframework.stereotype.Service;

import com.trinity.scheduler.entity.Course;
import com.trinity.scheduler.repository.CourseRepository;
import com.trinity.scheduler.repository.StudentCourseHistoryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates if a student has met prerequisites for a course.
 * 
 * <p>Checks student's course history to ensure all prerequisite courses
 * have been completed with 'passed' status.
 * 
 * @author Atul Kumar
 */
@Service
public class PrerequisiteValidator {
    private static final Logger log = LoggerFactory.getLogger(PrerequisiteValidator.class);

    private final CourseRepository courseRepository;
    private final StudentCourseHistoryRepository historyRepository;

    /**
     * Constructor with dependency injection.
     * 
     * @param courseRepository course data access
     * @param historyRepository course history data access
     */
    public PrerequisiteValidator(CourseRepository courseRepository,
                                StudentCourseHistoryRepository historyRepository) {
        this.courseRepository = courseRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * Validates if a student has met prerequisites for a course.
     * 
     * <p>Algorithm:
     * <ol>
     *   <li>Get the course</li>
     *   <li>If course has no prerequisite, return true</li>
     *   <li>Check if student has passed the prerequisite course</li>
     *   <li>Recursively check prerequisites of the prerequisite</li>
     * </ol>
     * 
     * @param studentId the student ID
     * @param courseId the course ID
     * @return true if prerequisites are met
     */
    public boolean hasMetPrerequisites(Long studentId, Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            log.warn("Course not found: {}", courseId);
            return false;
        }

        // No prerequisite required
        if (course.getPrerequisiteId() == null) {
            return true;
        }

        // Check if student passed the prerequisite
        boolean hasPassedPrereq = historyRepository.hasPassedCourse(
                studentId, course.getPrerequisiteId());

        if (!hasPassedPrereq) {
            log.debug("Student {} has not passed prerequisite {} for course {}",
                    studentId, course.getPrerequisiteId(), courseId);
            return false;
        }

        // Recursively check prerequisites of the prerequisite
        return hasMetPrerequisites(studentId, course.getPrerequisiteId());
    }

    /**
     * Gets the prerequisite chain for a course.
     * 
     * <p>Returns a list of course IDs representing the prerequisite chain,
     * ordered from the course itself back to the root prerequisite.
     * 
     * @param courseId the course ID
     * @return list of course IDs in prerequisite chain
     */
    public List<Long> getPrerequisiteChain(Long courseId) {
        List<Long> chain = new ArrayList<>();
        Long currentCourseId = courseId;

        while (currentCourseId != null) {
            chain.add(currentCourseId);
            Course course = courseRepository.findById(currentCourseId).orElse(null);
            if (course == null) {
                break;
            }
            currentCourseId = course.getPrerequisiteId();
        }

        return chain;
    }

    /**
     * Gets missing prerequisites for a student and course.
     * 
     * @param studentId the student ID
     * @param courseId the course ID
     * @return list of course IDs that are missing prerequisites
     */
    public List<Long> getMissingPrerequisites(Long studentId, Long courseId) {
        List<Long> missing = new ArrayList<>();
        List<Long> chain = getPrerequisiteChain(courseId);

        // Skip the course itself (first in chain)
        for (int i = 1; i < chain.size(); i++) {
            Long prereqId = chain.get(i);
            if (!historyRepository.hasPassedCourse(studentId, prereqId)) {
                missing.add(prereqId);
            }
        }

        return missing;
    }
}

