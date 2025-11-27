package com.trinity.scheduler.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trinity.scheduler.dto.SectionDTO;
import com.trinity.scheduler.entity.CourseSection;
import com.trinity.scheduler.entity.enums.SectionStatus;
import com.trinity.scheduler.mapper.CourseSectionMapper;
import com.trinity.scheduler.model.Section;
import com.trinity.scheduler.service.course.CourseSectionService;

import java.util.List;

/**
 * REST controller for operations for course sections.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Querying course sections</li>
 * </ul>
 *
 * @author Atul Kumar
 */
@Slf4j
@RestController
@RequestMapping("/api/sections")
@Tag(name = "Course Sections Operations", description = "Course sections operations")
public class CourseSectionsController {

    private final CourseSectionMapper courseSectionMapper;
    private final CourseSectionService courseSectionService;

    /**
     * Constructor with dependency injection.
     */
    public CourseSectionsController(CourseSectionMapper courseSectionMapper,
                                    CourseSectionService courseSectionService
    ) {
        this.courseSectionMapper = courseSectionMapper;
        this.courseSectionService = courseSectionService;
    }

    /**
     * Lists all the course sections with optional filters.
     *
     * @param semesterId the semester ID
     * @param courseId   optional course ID filter
     * @param status     optional status filter
     * @return list of sections
     */
    @GetMapping
    @Operation(summary = "List sections with filters",
            description = "Browse available course sections")
    public ResponseEntity<List<SectionDTO>> getCourseSections(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) SectionStatus status
    ) {

        // Get all course sections based on filters
        List<CourseSection> courseSections = courseSectionService.getSectionsBySemester(semesterId, courseId, status);

        // Get sections from course sections
        List<Section> sections = courseSectionService.getSectionsFromCourseSections(courseSections);

        // Map entities to DTOs
        List<SectionDTO> sectionDtos = sections.stream()
                .map(courseSectionMapper::from)
                .toList();

        return ResponseEntity.ok(sectionDtos);
    }
}
