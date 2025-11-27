package com.trinity.scheduler.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.trinity.scheduler.dto.SectionDTO;
import com.trinity.scheduler.dto.StudentScheduleResponse;
import com.trinity.scheduler.entity.Semester;
import com.trinity.scheduler.entity.StudentEnrollment;
import com.trinity.scheduler.mapper.CourseSectionMapper;
import com.trinity.scheduler.model.Section;
import com.trinity.scheduler.service.course.CourseSectionService;
import com.trinity.scheduler.service.semester.SemesterService;
import com.trinity.scheduler.service.student.EnrollmentService;

import java.util.List;

/**
 * REST controller for scheduling courses for students.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Generating weekly course enrollment schedules.</li>
 * </ul>
 *
 * @author Atul Kumar
 */
@Slf4j
@RestController
@RequestMapping("/api/students")
@Tag(name = "Student Schedule Generation", description = "Gets course enrollment details for students")
public class StudentScheduleController {

    private final CourseSectionMapper courseSectionMapper;
    private final CourseSectionService courseSectionService;
    private final EnrollmentService enrollmentService;
    private final SemesterService semesterService;

    /**
     * Constructor with dependency injection.
     */
    public StudentScheduleController(CourseSectionMapper courseSectionMapper,
                                     CourseSectionService courseSectionService,
                                     EnrollmentService enrollmentService,
                                     SemesterService semesterService) {
        this.courseSectionMapper = courseSectionMapper;
        this.courseSectionService = courseSectionService;
        this.enrollmentService = enrollmentService;
        this.semesterService = semesterService;
    }

    /**
     * Gets a student's personal schedule for a semester.
     *
     * @param studentId  the student ID
     * @param semesterId the semester ID
     * @return student's enrolled sections
     */
    @GetMapping("/{studentId}/schedule")
    @Operation(summary = "Get student's personal schedule",
            description = "Returns all sections student is enrolled in")
    public ResponseEntity<StudentScheduleResponse> getStudentSchedule(
            @PathVariable Long studentId,
            @RequestParam Long semesterId) {

        // Get student enrollments for the semester
        List<StudentEnrollment> enrollments = enrollmentService.getStudentEnrollments(studentId, semesterId);

        // Get sections from course sections
        List<Section> sections = courseSectionService.getSectionsFromCourseSections(
                enrollments.stream().map(StudentEnrollment::getSection).toList());

        // Get semester
        Semester semester = semesterService.getSemesterById(semesterId);

        // Map entities to DTOs
        List<SectionDTO> sectionDtos = sections.stream()
                .map(courseSectionMapper::from)
                .toList();

        StudentScheduleResponse response = new StudentScheduleResponse(
                studentId,
                semesterId,
                semester.getStartDate(),
                semester.getEndDate(),
                sectionDtos.size(),
                sectionDtos
        );

        return ResponseEntity.ok(response);
    }
}
