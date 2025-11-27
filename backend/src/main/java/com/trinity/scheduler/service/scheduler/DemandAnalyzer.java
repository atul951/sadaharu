package com.trinity.scheduler.service.scheduler;

import org.springframework.stereotype.Service;

import com.trinity.scheduler.entity.Course;
import com.trinity.scheduler.entity.Semester;
import com.trinity.scheduler.entity.Student;
import com.trinity.scheduler.repository.CourseRepository;
import com.trinity.scheduler.repository.SemesterRepository;
import com.trinity.scheduler.repository.StudentCourseHistoryRepository;
import com.trinity.scheduler.repository.StudentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Analyzes course demand to determine how many sections are needed.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Get all courses for the semester (by semester_order matching semester's order_in_year)</li>
 *   <li>For each course, count eligible students:
 *     <ul>
 *       <li>Student grade level must be within course's grade_level_min/max</li>
 *       <li>If course has prerequisite, student must have passed it</li>
 *     </ul>
 *   </li>
 *   <li>Calculate sections needed: ceil(eligible_students / 10)</li>
 * </ol>
 *
 * @author Atul Kumar
 */
@Service
public class DemandAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(DemandAnalyzer.class);
    private static final int SECTION_CAPACITY = 10;

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final StudentCourseHistoryRepository historyRepository;
    private final SemesterRepository semesterRepository;

    /**
     * Constructor with dependency injection.
     *
     * @param courseRepository   course data access
     * @param studentRepository  student data access
     * @param historyRepository  course history data access
     * @param semesterRepository semester data access
     */
    public DemandAnalyzer(CourseRepository courseRepository,
                          StudentRepository studentRepository,
                          StudentCourseHistoryRepository historyRepository,
                          SemesterRepository semesterRepository) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.historyRepository = historyRepository;
        this.semesterRepository = semesterRepository;
    }

    /**
     * Analyzes demand for all courses in a semester.
     *
     * <p>Returns a map where:
     * <ul>
     *   <li>Key: Course ID</li>
     *   <li>Value: Number of sections needed</li>
     * </ul>
     *
     * @param semesterId the semester ID
     * @return map of course ID to number of sections needed
     * @throws IllegalArgumentException if semester not found
     */
    public Map<Course, Integer> analyzeDemand(Long semesterId) {
        log.info("Analyzing demand for semester {}", semesterId);

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found: " + semesterId));

        // Get courses for this semester (matching semester_order)
        List<Course> courses = courseRepository.findBySemesterOrder(semester.getOrderInYear());
        log.info("Found {} courses for semester order {}", courses.size(), semester.getOrderInYear());

        Map<Course, Integer> demandMap = new HashMap<>();

        for (Course course : courses) {
            int eligibleCount = countEligibleStudents(course, semester.getYear());
            int sectionsNeeded = (int) Math.ceil((double) eligibleCount / SECTION_CAPACITY);

            if (sectionsNeeded > 0) {
                demandMap.put(course, sectionsNeeded);
                log.info("Course {} ({}): {} eligible students, {} sections needed",
                        course.getCode(), course.getName(), eligibleCount, sectionsNeeded);
            }
        }

        log.info("Demand analysis complete: {} courses need scheduling", demandMap.size());
        return demandMap;
    }

    /**
     * Counts students eligible for a course.
     *
     * <p>Eligibility criteria:
     * <ul>
     *   <li>Student grade level within course's min/max range</li>
     *   <li>Prerequisites met (if any)</li>
     * </ul>
     *
     * @param course the course
     * @return count of eligible students
     */
    private int countEligibleStudents(Course course, Integer semesterYear) {
        log.info("Counting eligible active students for course {} ({}) with grade levels {}-{} and year {}",
                course.getCode(), course.getName(), course.getGradeLevelMin(), course.getGradeLevelMax(), semesterYear);
        List<Student> allStudents = studentRepository.findByGradeLevelAndYearAndStatus(
                course.getGradeLevelMin(),
                course.getGradeLevelMax(),
                semesterYear,
                "active"
        );
        log.info("Found {} active students in grade levels {}-{} for year {} for course {} ({})",
                allStudents.size(), course.getGradeLevelMin(), course.getGradeLevelMax(), semesterYear, course.getCode(), course.getName());
        int eligibleCount = 0;

        for (Student student : allStudents) {
            if (isStudentEligible(student, course)) {
                eligibleCount++;
            }
        }
        log.info("Total {} active students are eligible for course {} ({})", eligibleCount, course.getCode(), course.getName());
        return eligibleCount;
    }

    /**
     * Checks if a student is eligible for a course.
     *
     * @param student the student
     * @param course  the course
     * @return true if eligible
     */
    private boolean isStudentEligible(Student student, Course course) {
        // Check prerequisite
        if (course.getPrerequisiteId() != null) {
            return historyRepository.hasPassedCourse(student.getId(), course.getPrerequisiteId());
        }
        return true;
    }
}

