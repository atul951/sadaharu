package com.trinity.scheduler.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.trinity.scheduler.dto.EnrollmentRequest;
import com.trinity.scheduler.dto.EnrollmentResponse;
import com.trinity.scheduler.dto.EnrollmentValidationResult;
import com.trinity.scheduler.dto.StudentProgressResponse;
import com.trinity.scheduler.entity.Student;
import com.trinity.scheduler.entity.StudentCourseHistory;
import com.trinity.scheduler.entity.StudentEnrollment;
import com.trinity.scheduler.exception.EnrollmentException;
import com.trinity.scheduler.service.course.CourseService;
import com.trinity.scheduler.service.student.EnrollmentService;
import com.trinity.scheduler.service.student.StudentService;

import java.util.List;

/**
 * REST controller for operations for students.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Student enrollment operations</li>
 *   <li>Academic progress tracking</li>
 * </ul>
 *
 * @author Atul Kumar
 */
@Slf4j
@RestController
@RequestMapping("/api/students")
@Tag(name = "Student Operations", description = "Student enrollment and academic progress operations")
public class StudentController {
    private final CourseService courseService;
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;

    /**
     * Constructor with dependency injection.
     */
    public StudentController(CourseService courseService,
                             StudentService studentService,
                             EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;
    }

    /**
     * Enrolls a student in a section.
     *
     * @param request enrollment request with student and section IDs
     * @return enrollment response
     */
    @PostMapping("/{studentId}/enroll")
    @Operation(summary = "Enroll student in section", description = "Creates enrollment with validation")
    public ResponseEntity<EnrollmentResponse> enrollStudent(@PathVariable Long studentId,
            @Valid @RequestBody EnrollmentRequest request) {
        try {
            Student student = studentService.getStudentById(studentId);
            StudentEnrollment enrollment = enrollmentService.enrollStudentInCourse(
                    student.getId(), request.getSectionId());

            EnrollmentResponse response = new EnrollmentResponse();
            response.setId(enrollment.getId());
            response.setStudentId(enrollment.getStudentId());
            response.setSectionId(enrollment.getSection().getId());
            response.setStatus(enrollment.getStatus().name());
            response.setEnrolledAt(enrollment.getEnrolledAt());
            response.setMessage("Successfully enrolled");

            return ResponseEntity.ok(response);
        } catch (EnrollmentException e) {
            EnrollmentResponse response = new EnrollmentResponse();
            response.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Validates if a student can enroll in a section.
     *
     * @param studentId the student ID
     * @param sectionId the section ID
     * @return validation result
     */
    @GetMapping("/{studentId}/enroll/validate")
    @Operation(summary = "Validate enrollment",
            description = "Checks if student can enroll without actually enrolling")
    public ResponseEntity<EnrollmentValidationResult> validateStudentEnrollment(
            @PathVariable Long studentId,
            @RequestParam Long sectionId
    ) {
        EnrollmentValidationResult result = enrollmentService.validateEnrollment(studentId, sectionId);
        return ResponseEntity.ok(result);
    }

    /**
     * Gets a student's academic progress toward graduation.
     *
     * @param studentId the student ID
     * @return progress information
     */
    @GetMapping("/{studentId}/progress")
    @Operation(summary = "Get academic progress toward graduation",
            description = "Returns GPA, credits earned, graduation status")
    public ResponseEntity<StudentProgressResponse> getStudentProgress(@PathVariable Long studentId) {
        Student student = studentService.getStudentById(studentId);

        // Get passed courses
        List<StudentCourseHistory> courseHistories = studentService.getStudentCourseHistory(studentId);
        List<StudentCourseHistory> passedCourses = courseHistories.stream()
                .filter(StudentCourseHistory::isPassed).toList();

        // Calculate credits earned
        Integer creditsEarned = courseService.getCoursesCredits(passedCourses.stream()
                .map(StudentCourseHistory::getCourseId).toList());

        if(creditsEarned == null) {
                creditsEarned = 0;
        }

        StudentProgressResponse progress = new StudentProgressResponse(
                studentId,
                student.getFullName(),
                student.getGradeLevel(),
                creditsEarned,
                30,
                Math.max(0, 30 - creditsEarned),
                courseHistories.size(),
                passedCourses.size(),
                courseHistories.size() - passedCourses.size(),
                courseHistories.size(),
                creditsEarned >= (student.getGradeLevel() - 8) * 7.5
        );

        return ResponseEntity.ok(progress);
    }
}
