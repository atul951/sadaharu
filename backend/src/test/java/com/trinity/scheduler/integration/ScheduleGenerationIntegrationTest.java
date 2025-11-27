package com.trinity.scheduler.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.trinity.scheduler.entity.CourseSection;
import com.trinity.scheduler.entity.SectionTimeslot;
import com.trinity.scheduler.entity.enums.SectionStatus;
import com.trinity.scheduler.model.ScheduleResult;
import com.trinity.scheduler.repository.CourseSectionRepository;
import com.trinity.scheduler.repository.SectionTimeslotRepository;
import com.trinity.scheduler.service.scheduler.SemesterScheduler;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for schedule generation.
 * Tests the complete scheduling workflow end-to-end with real database.
 * Verifies that generated schedules satisfy all hard constraints.
 *
 * @author Atul Kumar
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:database/trinity_college.sqlite",
        "spring.jpa.hibernate.ddl-auto=update"
})
@Transactional
class ScheduleGenerationIntegrationTest {

    @Autowired
    private SemesterScheduler semesterScheduler;

    @Autowired
    private CourseSectionRepository sectionRepository;

    @Autowired
    private SectionTimeslotRepository timeslotRepository;


    @Test
    @DisplayName("Should generate complete schedule with no constraint violations")
    void shouldGenerateCompleteSchedule() {
        // Given
        Long semesterId = 1L;

        // When
        ScheduleResult result = semesterScheduler.generateSchedule(semesterId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.sections()).isNotEmpty();
        assertThat(result.scheduled()).isGreaterThan(0);

        // Verify sections were persisted
        List<CourseSection> persistedSections = sectionRepository.findBySemesterIdAndStatus(
                semesterId, SectionStatus.SCHEDULED);
        assertThat(persistedSections).isNotEmpty();

        // Verify all scheduled sections have timeslots
        for (CourseSection section : persistedSections) {
            List<SectionTimeslot> timeslots = timeslotRepository.findBySectionId(section.getId());
            assertThat(timeslots).isNotEmpty();

            // Verify hours match (allow for small floating point differences)
            double totalHours = timeslots.stream()
                    .mapToDouble(SectionTimeslot::getDurationHours)
                    .sum();
            assertThat(totalHours).isCloseTo(section.getHoursPerWeek(), org.assertj.core.data.Offset.offset(0.1));
        }
    }

    @Test
    @DisplayName("Should not schedule any classes during lunch break")
    void shouldNotScheduleClassesDuringLunch() {
        // Given
        Long semesterId = 1L;
        LocalTime lunchStart = LocalTime.of(12, 0);
        LocalTime lunchEnd = LocalTime.of(13, 0);

        // When
        semesterScheduler.generateSchedule(semesterId);

        // Then
        List<CourseSection> sections = sectionRepository.findBySemesterIdAndStatus(
                semesterId, SectionStatus.SCHEDULED);

        for (CourseSection section : sections) {
            List<SectionTimeslot> timeslots = timeslotRepository.findBySectionId(section.getId());

            for (SectionTimeslot slot : timeslots) {
                // No timeslot should overlap with lunch (12-1 PM)
                boolean overlapsLunch = slot.getStartTime().isBefore(lunchEnd) &&
                        slot.getEndTime().isAfter(lunchStart);
                assertThat(overlapsLunch)
                        .withFailMessage("Section %d has timeslot during lunch: %s",
                                section.getId(), slot)
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("Should not create teacher conflicts")
    void shouldNotCreateTeacherConflicts() {
        // Given
        Long semesterId = 1L;

        // When
        semesterScheduler.generateSchedule(semesterId);

        // Then
        List<CourseSection> sections = sectionRepository.findBySemesterIdAndStatus(
                semesterId, SectionStatus.SCHEDULED);

        // Group sections by teacher (exclude sections with no teacher assigned, i.e., teacherId == null or 0)
        Map<Long, List<CourseSection>> sectionsByTeacher = sections.stream()
                .filter(s -> s.getTeacherId() != null && s.getTeacherId() > 0)
                .collect(Collectors.groupingBy(CourseSection::getTeacherId));

        // Check each teacher's schedule for conflicts
        for (Map.Entry<Long, List<CourseSection>> entry : sectionsByTeacher.entrySet()) {
            List<CourseSection> teacherSections = entry.getValue();

            // Get all timeslots for this teacher
            List<SectionTimeslot> allTimeslots = teacherSections.stream()
                    .flatMap(s -> timeslotRepository.findBySectionId(s.getId()).stream())
                    .toList();

            // Check for overlaps
            for (int i = 0; i < allTimeslots.size(); i++) {
                for (int j = i + 1; j < allTimeslots.size(); j++) {
                    SectionTimeslot slot1 = allTimeslots.get(i);
                    SectionTimeslot slot2 = allTimeslots.get(j);

                    boolean overlaps = slot1.overlapsWith(slot2);
                    assertThat(overlaps)
                            .withFailMessage("Teacher %d has conflicting timeslots: %s and %s",
                                    entry.getKey(), slot1, slot2)
                            .isFalse();
                }
            }
        }
    }

    @Test
    @DisplayName("Should not create room conflicts")
    void shouldNotCreateRoomConflicts() {
        // Given
        Long semesterId = 1L;

        // When
        semesterScheduler.generateSchedule(semesterId);

        // Then
        List<CourseSection> sections = sectionRepository.findBySemesterIdAndStatus(
                semesterId, SectionStatus.SCHEDULED);

        // Group sections by classroom (exclude sections with no classroom assigned, i.e., classroomId == null or 0)
        Map<Long, List<CourseSection>> sectionsByRoom = sections.stream()
                .filter(s -> s.getClassroomId() != null && s.getClassroomId() > 0)
                .collect(Collectors.groupingBy(CourseSection::getClassroomId));

        // Check each room's schedule for conflicts
        for (Map.Entry<Long, List<CourseSection>> entry : sectionsByRoom.entrySet()) {
            List<CourseSection> roomSections = entry.getValue();

            // Get all timeslots for this room
            List<SectionTimeslot> allTimeslots = roomSections.stream()
                    .flatMap(s -> timeslotRepository.findBySectionId(s.getId()).stream())
                    .toList();

            // Check for overlaps
            for (int i = 0; i < allTimeslots.size(); i++) {
                for (int j = i + 1; j < allTimeslots.size(); j++) {
                    SectionTimeslot slot1 = allTimeslots.get(i);
                    SectionTimeslot slot2 = allTimeslots.get(j);

                    boolean overlaps = slot1.overlapsWith(slot2);
                    assertThat(overlaps)
                            .withFailMessage("Room %d has conflicting timeslots: %s and %s",
                                    entry.getKey(), slot1, slot2)
                            .isFalse();
                }
            }
        }
    }

    @Test
    @DisplayName("Should respect teacher daily hour limits")
    void shouldRespectTeacherDailyLimits() {
        // Given
        Long semesterId = 1L;
        int MAX_DAILY_HOURS = 4;

        // When
        semesterScheduler.generateSchedule(semesterId);

        // Then
        List<CourseSection> sections = sectionRepository.findBySemesterIdAndStatus(
                semesterId, SectionStatus.SCHEDULED);

        // Group sections by teacher (exclude sections with no teacher assigned, i.e., teacherId == null or 0)
        Map<Long, List<CourseSection>> sectionsByTeacher = sections.stream()
                .filter(s -> s.getTeacherId() != null && s.getTeacherId() > 0)
                .collect(Collectors.groupingBy(CourseSection::getTeacherId));

        // Check each teacher's daily hours
        for (Map.Entry<Long, List<CourseSection>> entry : sectionsByTeacher.entrySet()) {
            List<CourseSection> teacherSections = entry.getValue();

            // Get all timeslots for this teacher
            List<SectionTimeslot> allTimeslots = teacherSections.stream()
                    .flatMap(s -> timeslotRepository.findBySectionId(s.getId()).stream())
                    .toList();

            // Group by day and sum hours
            Map<Integer, Double> hoursByDay = allTimeslots.stream()
                    .collect(Collectors.groupingBy(
                            SectionTimeslot::getDayOfWeek,
                            Collectors.summingDouble(SectionTimeslot::getDurationHours)
                    ));

            // Verify no day exceeds limit (allow small floating point differences)
            for (Map.Entry<Integer, Double> dayEntry : hoursByDay.entrySet()) {
                assertThat(dayEntry.getValue())
                        .withFailMessage("Teacher %d exceeds %d hour daily limit on day %d: %.1f hours",
                                entry.getKey(), MAX_DAILY_HOURS, dayEntry.getKey(), dayEntry.getValue())
                        .isLessThanOrEqualTo((double) MAX_DAILY_HOURS + 0.1);
            }
        }
    }

    @Test
    @DisplayName("Should keep all timeslots within school hours")
    void shouldKeepTimeslotsWithinSchoolHours() {
        // Given
        Long semesterId = 1L;
        LocalTime schoolStart = LocalTime.of(9, 0);
        LocalTime schoolEnd = LocalTime.of(17, 0);

        // When
        semesterScheduler.generateSchedule(semesterId);

        // Then
        List<CourseSection> sections = sectionRepository.findBySemesterIdAndStatus(
                semesterId, SectionStatus.SCHEDULED);

        for (CourseSection section : sections) {
            List<SectionTimeslot> timeslots = timeslotRepository.findBySectionId(section.getId());

            for (SectionTimeslot slot : timeslots) {
                assertThat(slot.getStartTime())
                        .withFailMessage("Section %d starts before school hours: %s",
                                section.getId(), slot.getStartTime())
                        .isAfterOrEqualTo(schoolStart);

                assertThat(slot.getEndTime())
                        .withFailMessage("Section %d ends after school hours: %s",
                                section.getId(), slot.getEndTime())
                        .isBeforeOrEqualTo(schoolEnd);
            }
        }
    }
}
