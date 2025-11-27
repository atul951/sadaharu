package com.trinity.scheduler.service.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trinity.scheduler.entity.*;
import com.trinity.scheduler.entity.enums.SectionStatus;
import com.trinity.scheduler.model.ScheduleResult;
import com.trinity.scheduler.model.TimeSlot;
import com.trinity.scheduler.repository.*;
import com.trinity.scheduler.service.course.CourseSectionService;
import com.trinity.scheduler.service.scheduler.DemandAnalyzer;
import com.trinity.scheduler.service.scheduler.SemesterScheduler;
import com.trinity.scheduler.service.timeslot.TimeSlotGenerator;
import com.trinity.scheduler.service.timeslot.TimeSlotService;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SemesterScheduler.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 *
 * @author Atul Kumar
 */
@ExtendWith(MockitoExtension.class)
class SemesterSchedulerTest {

    @Mock
    private DemandAnalyzer demandAnalyzer;

    @Mock
    private CourseSectionService sectionService;

    @Mock
    private TimeSlotService timeSlotService;

    @Mock
    private TimeSlotGenerator timeSlotGenerator;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private SpecializationRepository specializationRepository;

    private SemesterScheduler semesterScheduler;

    @BeforeEach
    void setUp() {
        semesterScheduler = new SemesterScheduler(
                demandAnalyzer,
                timeSlotGenerator,
                sectionService,
                timeSlotService,
                teacherRepository,
                classroomRepository,
                specializationRepository
        );
    }

    @Test
    @DisplayName("Should generate schedule successfully")
    void shouldGenerateScheduleSuccessfully() {
        // Given
        Long semesterId = 1L;
        Course course = createCourse(1L, "MATH101", 1L);
        Map<Course, Integer> demand = Map.of(course, 2);
        CourseSection section1 = createCourseSection(1L, course.getId(), semesterId, 1, 4);
        CourseSection section2 = createCourseSection(2L, course.getId(), semesterId, 2, 4);
        List<CourseSection> sections = Arrays.asList(section1, section2);
        Map<Course, List<CourseSection>> courseSections = Map.of(course, sections);

        Specialization specialization = createSpecialization(1L, 1L);
        Teacher teacher = createTeacher(1L, 1L);
        Classroom classroom = createClassroom(1L, 1L);
        List<TimeSlot> timeSlots = createTimeSlots(4);

        when(demandAnalyzer.analyzeDemand(semesterId)).thenReturn(demand);
        when(sectionService.createSections(demand, semesterId)).thenReturn(courseSections);
        when(specializationRepository.findById(course.getSpecializationId()))
                .thenReturn(Optional.of(specialization));
        when(teacherRepository.findBySpecializationId(course.getSpecializationId()))
                .thenReturn(Collections.singletonList(teacher));
        when(classroomRepository.findByRoomTypeId(specialization.getRoomTypeId()))
                .thenReturn(Collections.singletonList(classroom));
        when(timeSlotGenerator.generateAllPossibleSlots()).thenReturn(timeSlots);
        when(timeSlotService.canAssign(any(), any(), any(), any())).thenReturn(true);
        doNothing().when(timeSlotService).assign(any(), any(), any(), any());

        // When
        ScheduleResult result = semesterScheduler.generateSchedule(semesterId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.sections()).hasSize(2);
        assertThat(result.scheduled()).isEqualTo(2);
        assertThat(result.failed()).isEqualTo(0);
        verify(demandAnalyzer).analyzeDemand(semesterId);
        verify(sectionService).createSections(demand, semesterId);
    }

    @Test
    @DisplayName("Should handle scheduling failure for some sections")
    void shouldHandleSchedulingFailureForSomeSections() {
        // Given
        Long semesterId = 1L;
        Course course = createCourse(1L, "MATH101", 1L);
        Map<Course, Integer> demand = Map.of(course, 2);
        CourseSection section1 = createCourseSection(1L, course.getId(), semesterId, 1, 4);
        CourseSection section2 = createCourseSection(2L, course.getId(), semesterId, 2, 4);
        List<CourseSection> sections = Arrays.asList(section1, section2);
        Map<Course, List<CourseSection>> courseSections = Map.of(course, sections);

        Specialization specialization = createSpecialization(1L, 1L);
        Teacher teacher = createTeacher(1L, 1L);
        Classroom classroom = createClassroom(1L, 1L);
        List<TimeSlot> timeSlots = createTimeSlots(4);

        when(demandAnalyzer.analyzeDemand(semesterId)).thenReturn(demand);
        when(sectionService.createSections(demand, semesterId)).thenReturn(courseSections);
        when(specializationRepository.findById(course.getSpecializationId()))
                .thenReturn(Optional.of(specialization));
        when(teacherRepository.findBySpecializationId(course.getSpecializationId()))
                .thenReturn(Collections.singletonList(teacher));
        when(classroomRepository.findByRoomTypeId(specialization.getRoomTypeId()))
                .thenReturn(Collections.singletonList(classroom));
        when(timeSlotGenerator.generateAllPossibleSlots()).thenReturn(timeSlots);
        // First section can be assigned, second cannot
        when(timeSlotService.canAssign(eq(section1), any(), any(), any())).thenReturn(true);
        when(timeSlotService.canAssign(eq(section2), any(), any(), any())).thenReturn(false);
        doNothing().when(timeSlotService).assign(any(), any(), any(), any());

        // When
        ScheduleResult result = semesterScheduler.generateSchedule(semesterId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.scheduled()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should skip already scheduled sections")
    void shouldSkipAlreadyScheduledSections() {
        // Given
        Long semesterId = 1L;
        Course course = createCourse(1L, "MATH101", 1L);
        Map<Course, Integer> demand = Map.of(course, 1);
        CourseSection section = createCourseSection(1L, course.getId(), semesterId, 1, 4);
        section.setStatus(SectionStatus.SCHEDULED);
        List<CourseSection> sections = Collections.singletonList(section);
        Map<Course, List<CourseSection>> courseSections = Map.of(course, sections);

        when(demandAnalyzer.analyzeDemand(semesterId)).thenReturn(demand);
        when(sectionService.createSections(demand, semesterId)).thenReturn(courseSections);

        // When
        ScheduleResult result = semesterScheduler.generateSchedule(semesterId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.failed()).isEqualTo(1);
        verify(timeSlotService, never()).assign(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle missing specialization")
    void shouldHandleMissingSpecialization() {
        // Given
        Long semesterId = 1L;
        Course course = createCourse(1L, "MATH101", 1L);
        Map<Course, Integer> demand = Map.of(course, 1);
        CourseSection section = createCourseSection(1L, course.getId(), semesterId, 1, 4);
        List<CourseSection> sections = Collections.singletonList(section);
        Map<Course, List<CourseSection>> courseSections = Map.of(course, sections);

        when(demandAnalyzer.analyzeDemand(semesterId)).thenReturn(demand);
        when(sectionService.createSections(demand, semesterId)).thenReturn(courseSections);
        when(specializationRepository.findById(course.getSpecializationId()))
                .thenReturn(Optional.empty());

        // When
        ScheduleResult result = semesterScheduler.generateSchedule(semesterId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.scheduled()).isEqualTo(0);
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle exception during schedule generation")
    void shouldHandleExceptionDuringScheduleGeneration() {
        // Given
        Long semesterId = 1L;
        when(demandAnalyzer.analyzeDemand(semesterId))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            semesterScheduler.generateSchedule(semesterId);
        });
    }

    // Helper methods

    private Course createCourse(Long id, String code, Long specializationId) {
        Course course = new Course();
        course.setId(id);
        course.setCode(code);
        course.setName("Test Course");
        course.setSpecializationId(specializationId);
        course.setHoursPerWeek(4);
        return course;
    }

    private CourseSection createCourseSection(Long id, Long courseId, Long semesterId,
                                               Integer sectionNumber, Integer hoursPerWeek) {
        CourseSection section = new CourseSection(courseId, semesterId, sectionNumber, hoursPerWeek);
        section.setId(id);
        section.setStatus(SectionStatus.UNSCHEDULED);
        return section;
    }

    private Specialization createSpecialization(Long id, Long roomTypeId) {
        Specialization specialization = new Specialization();
        specialization.setId(id);
        specialization.setName("Mathematics");
        specialization.setRoomTypeId(roomTypeId);
        return specialization;
    }

    private Teacher createTeacher(Long id, Long specializationId) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setFirstName("John");
        teacher.setLastName("Doe");
        teacher.setSpecializationId(specializationId);
        return teacher;
    }

    private Classroom createClassroom(Long id, Long roomTypeId) {
        Classroom classroom = new Classroom();
        classroom.setId(id);
        classroom.setName("Room 101");
        classroom.setRoomTypeId(roomTypeId);
        return classroom;
    }

    private List<TimeSlot> createTimeSlots(int count) {
        List<TimeSlot> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            slots.add(new TimeSlot(
                    DayOfWeek.MONDAY,
                    LocalTime.of(9 + i, 0),
                    LocalTime.of(10 + i, 0)
            ));
        }
        return slots;
    }
}

