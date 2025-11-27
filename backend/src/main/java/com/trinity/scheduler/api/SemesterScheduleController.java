package com.trinity.scheduler.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trinity.scheduler.dto.ScheduleResponse;
import com.trinity.scheduler.dto.SectionDTO;
import com.trinity.scheduler.entity.CourseSection;
import com.trinity.scheduler.entity.Semester;
import com.trinity.scheduler.mapper.CourseSectionMapper;
import com.trinity.scheduler.model.ScheduleResult;
import com.trinity.scheduler.model.Section;
import com.trinity.scheduler.service.course.CourseSectionService;
import com.trinity.scheduler.service.scheduler.SemesterScheduler;
import com.trinity.scheduler.service.semester.SemesterService;

import static com.trinity.scheduler.entity.enums.SectionStatus.SCHEDULED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for scheduling courses for semesters.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Generating weekly course schedules.</li>
 * </ul>
 *
 * @author Atul Kumar
 */
@RestController
@RequestMapping("/api/semesters")
@Tag(name = "Semester Schedule Generation", description = "Generates weekly master course schedules for semesters")
public class SemesterScheduleController {
    private static final Logger log = LoggerFactory.getLogger(SemesterScheduleController.class);

    private final SemesterScheduler semesterScheduler;
    private final CourseSectionMapper courseSectionMapper;
    private final CourseSectionService courseSectionService;
    private final SemesterService semesterService;

    /**
     * Constructor with dependency injection.
     */
    public SemesterScheduleController(SemesterScheduler semesterScheduler,
                                      CourseSectionMapper courseSectionMapper,
                                      CourseSectionService courseSectionService,
                                      SemesterService semesterService) {
        this.semesterScheduler = semesterScheduler;
        this.courseSectionMapper = courseSectionMapper;
        this.courseSectionService = courseSectionService;
        this.semesterService = semesterService;
    }

    /**
     * Generates a master weekly schedule for all courses in the given semester.
     *
     * <p>The algorithm uses simple placement to assign teachers, rooms,
     * and time slots to course sections while respecting all constraints.
     *
     * @param semesterId the semester ID to generate schedule for
     * @return generated schedule
     */
    @PostMapping("{semesterId}/schedule")
    @Operation(summary = "Generate semester schedule",
            description = "Generates a master weekly schedule for all courses in the given semester.")
    public ResponseEntity<ScheduleResponse> generateSchedule(@PathVariable Long semesterId) {
        log.info("Generating weekly course schedule for semester {}", semesterId);

        // Get semester
        Semester semester = semesterService.getSemesterById(semesterId);

        // Generate schedule
        ScheduleResult result = semesterScheduler.generateSchedule(semesterId);

        // Get sections from scheduled course sections
        List<Section> sections = courseSectionService.getSectionsFromCourseSections(result.sections());

        // Convert to DTO
        List<SectionDTO> sectionDtos = sections.stream()
                .map(courseSectionMapper::from)
                .toList();

        // Build statistics
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total_sections", result.sections().size());
        statistics.put("scheduled", result.scheduled());
        statistics.put("failed", result.failed());
        statistics.put("success_rate",
                result.sections().size() > 0 ?
                        (double) result.scheduled() / result.sections().size() : 0.0);

        ScheduleResponse response = new ScheduleResponse(
                semesterId,
                semester.getStartDate(),
                semester.getEndDate(),
                result.sections().size(),
                result.scheduled(),
                result.failed(),
                sectionDtos,
                statistics
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Generates a master weekly schedule for all courses in the given semester.
     *
     * <p>The algorithm uses simple placement to assign teachers, rooms,
     * and time slots to course sections while respecting all constraints.
     *
     * @param semesterId the semester ID to generate schedule for
     * @return generated schedule
     */
    @GetMapping("/{semesterId}")
    @Operation(summary = "Fetch semester schedule",
            description = "Fetches the master weekly schedule for all courses in the given semester.")
    public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable Long semesterId) {
        log.info("Generating weekly course schedule for semester {}", semesterId);

        // Get semester
        Semester semester = semesterService.getSemesterById(semesterId);

        // Get course sections from semester
        List<CourseSection> courseSections = courseSectionService.getSectionsBySemester(semesterId, null, null);

        // Get sections from semester course sections
        List<Section> sections = courseSectionService.getSectionsFromCourseSections(courseSections);

        // Convert to DTO
        List<SectionDTO> sectionDtos = sections.stream()
                .map(courseSectionMapper::from)
                .toList();

        int scheduledCount = (int) sections.stream().filter(s -> SCHEDULED == s.getStatus()).count();

        // Build statistics
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total_sections", sections.size());
        statistics.put("scheduled", scheduledCount);
        statistics.put("failed", sections.size() - scheduledCount);
        statistics.put("success_rate", sections.size() > 0 ?
                (double) scheduledCount / sections.size() : 0.0);

        ScheduleResponse response = new ScheduleResponse(
                semesterId,
                semester.getStartDate(),
                semester.getEndDate(),
                sections.size(),
                scheduledCount,
                sections.size() - scheduledCount,
                sectionDtos,
                statistics
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes the complete schedule for a semester.
     *
     * @param semesterId the semester ID
     * @return schedule with all sections
     */
    @DeleteMapping("/{semesterId}/delete")
    @Operation(summary = "Delete schedule for semester",
            description = "Delete all scheduled sections for a semester")
    public ResponseEntity<String> deleteSchedule(@PathVariable Long semesterId) {
        try {
            semesterService.revertScheduleData(semesterId);
        } catch (Exception e) {
            log.error("All schedule data delete fail for semester {}: {}", semesterId, e.getMessage());
            return ResponseEntity.ok("All schedule data for semester %s delete fail.".formatted(semesterId));
        }
        return ResponseEntity.ok("All schedule data delete complete for semester %s.".formatted(semesterId));
    }

    /**
     * Gets the complete schedule for a semester.
     *
     * @return schedule with all sections
     */
    @DeleteMapping("/all")
    @Operation(summary = "Delete All schedule",
            description = "Delete all sections for all semester")
    public ResponseEntity<String> deleteScheduleAll() {
        try {
            semesterService.revertAllScheduleData();
        } catch (Exception e) {
            log.error("All schedule data delete fail for semester: {}", e.getMessage());
            return ResponseEntity.ok("All schedule data failed");
        }
        return ResponseEntity.ok("All schedule data delete successful");
    }
}
