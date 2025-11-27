package com.trinity.scheduler.service.scheduler;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trinity.scheduler.entity.*;
import com.trinity.scheduler.entity.enums.SectionStatus;
import com.trinity.scheduler.model.ScheduleResult;
import com.trinity.scheduler.model.TimeSlot;
import com.trinity.scheduler.repository.*;
import com.trinity.scheduler.service.course.CourseSectionService;
import com.trinity.scheduler.service.timeslot.TimeSlotGenerator;
import com.trinity.scheduler.service.timeslot.TimeSlotService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Main scheduling algorithm.
 *
 * <p>Algorithm Steps:
 * <ol>
 *   <li>Analyze demand to determine sections needed</li>
 *   <li>Create section entities</li>
 *   <li>Sort by priority (core courses first, higher hours first)</li>
 *   <li>For each section:
 *     <ul>
 *       <li>Find eligible teachers (by specialization)</li>
 *       <li>Find eligible rooms (by room type)</li>
 *       <li>Try timeslot combinations</li>
 *       <li>Check constraints (no conflicts, daily limits)</li>
 *       <li>Assign if valid</li>
 *     </ul>
 *   </li>
 *   <li>Persist schedule transactionally</li>
 * </ol>
 *
 * @author Atul Kumar
 */
@Service
public class SemesterScheduler {
    private static final Logger log = LoggerFactory.getLogger(SemesterScheduler.class);

    private final DemandAnalyzer demandAnalyzer;
    private final CourseSectionService sectionService;
    private final TimeSlotService timeSlotService;
    private final TimeSlotGenerator timeSlotGenerator;
    private final TeacherRepository teacherRepository;
    private final ClassroomRepository classroomRepository;
    private final SpecializationRepository specializationRepository;

    /**
     * Constructor with dependency injection.
     */
    public SemesterScheduler(DemandAnalyzer demandAnalyzer,
                             TimeSlotGenerator timeSlotGenerator,
                             CourseSectionService courseSectionService,
                             TimeSlotService timeSlotService,
                             TeacherRepository teacherRepository,
                             ClassroomRepository classroomRepository,
                             SpecializationRepository specializationRepository) {
        this.demandAnalyzer = demandAnalyzer;
        this.timeSlotGenerator = timeSlotGenerator;
        this.sectionService = courseSectionService;
        this.timeSlotService = timeSlotService;
        this.teacherRepository = teacherRepository;
        this.classroomRepository = classroomRepository;
        this.specializationRepository = specializationRepository;
    }

    /**
     * Generates a schedule for a semester using the algorithm.
     *
     * @param semesterId the semester ID
     * @return schedule result with statistics
     */
    @Transactional
    public ScheduleResult generateSchedule(Long semesterId) {
        log.info("Starting schedule generation for semester {}", semesterId);

        try {
            // Analyze demand
            Map<Course, Integer> demand = demandAnalyzer.analyzeDemand(semesterId);
            log.info("Demand analysis: {} courses need scheduling", demand.size());

            // Create sections
            Map<Course, List<CourseSection>> courseSections = sectionService.createSections(demand, semesterId);

            // Schedule and persist each section
            int scheduled = 0;
            int failed = 0;
            Pair<Integer, Integer> result;
            for (Map.Entry<Course, List<CourseSection>> entry : courseSections.entrySet()) {
                result = scheduleCourseSections(entry.getKey(), entry.getValue());
                scheduled += result.getLeft();
                failed += result.getRight();
            }

            log.info("Scheduling complete: {} scheduled, {} failed", scheduled, failed);

            // Return result
            List<CourseSection> sections = courseSections.values().stream().flatMap(Collection::stream).toList();
            return new ScheduleResult(sections, scheduled, failed);

        } catch (Exception e) {
            log.error("Schedule generation failed", e);
            throw new RuntimeException("Schedule generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Attempts to schedule a single section.
     *
     * @param sections list of the sections in course
     * @return pair of (scheduled count, failed count)
     */
    private Pair<Integer, Integer> scheduleCourseSections(Course course, List<CourseSection> sections) {
        log.info("Scheduling {} sections for course {}", sections.size(), course.getCode());
        int scheduled = 0;
        int failed = 0;

        // Get specialization for the course
        Specialization specialization = specializationRepository.findById(course.getSpecializationId()).orElse(null);
        if (specialization == null) {
            log.error("No specialization found for course {}, skipping scheduling", course.getCode());
            return Pair.of(0, sections.size());
        }

        // Get eligible teachers and rooms for the course
        List<Teacher> teachers = teacherRepository.findBySpecializationId(course.getSpecializationId());
        List<Classroom> rooms = classroomRepository.findByRoomTypeId(specialization.getRoomTypeId());
        log.info("Found {} eligible teachers and {} eligible rooms", teachers.size(), rooms.size());

        for (CourseSection section : sections) {
            if (section.getStatus().equals(SectionStatus.SCHEDULED)) {
                scheduled++;
                log.debug("Section {} already scheduled for course {}, skipping creation", section.getId(), course.getId());
            } else if (scheduleSection(course, teachers, rooms, section)) {
                scheduled++;
            } else {
                failed++;
                log.debug("Failed to schedule section {} for course {}", section.getSectionNumber(), section.getCourseId());
            }
        }
        return Pair.of(scheduled, failed);
    }

    /**
     * Attempts to schedule a single section.
     *
     * @param section the section to schedule
     * @return true if successfully scheduled
     */
    private boolean scheduleSection(Course course, List<Teacher> teachers,
                                    List<Classroom> rooms, CourseSection section) {
        log.info("Scheduling section {} of course {} ({} hours/week)",
                section.getSectionNumber(), course.getCode(), section.getHoursPerWeek());

        List<TimeSlot> timeslotCombinations = timeSlotGenerator.generateAllPossibleSlots();

        // Try each combination
        for (Teacher teacher : teachers) {
            for (Classroom room : rooms) {
                List<TimeSlot> timeslotsForSection = new ArrayList<>();
                for (TimeSlot timeslot : timeslotCombinations) {
                    if (timeSlotService.canAssign(section, teacher, room, timeslot)) {
                        timeslotsForSection.add(timeslot);
                        if (timeslotsForSection.size() == section.getHoursPerWeek()) {
                            timeSlotService.assign(section, teacher, room, timeslotsForSection);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
