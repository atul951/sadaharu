package com.trinity.scheduler.service.timeslot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trinity.scheduler.entity.Classroom;
import com.trinity.scheduler.entity.CourseSection;
import com.trinity.scheduler.entity.SectionTimeslot;
import com.trinity.scheduler.entity.Teacher;
import com.trinity.scheduler.entity.enums.SectionStatus;
import com.trinity.scheduler.model.TimeSlot;
import com.trinity.scheduler.repository.SectionTimeslotRepository;
import com.trinity.scheduler.service.timeslot.TimeSlotService;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TimeSlotService.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 *
 * @author Atul Kumar
 */
@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private SectionTimeslotRepository timeslotRepository;

    private TimeSlotService timeSlotService;

    @BeforeEach
    void setUp() {
        timeSlotService = new TimeSlotService(timeslotRepository);
    }

    @Test
    @DisplayName("Should return true when assignment is valid")
    void shouldReturnTrueWhenAssignmentIsValid() {
        // Given
        CourseSection section = createCourseSection(1L, 1L, 1L, 4);
        Teacher teacher = createTeacher(1L);
        Classroom classroom = createClassroom(1L);
        TimeSlot timeslot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        when(timeslotRepository.hasTeacherOrRoomConflict(
                teacher.getId(), classroom.getId(), timeslot.day.getValue(),
                timeslot.startTime, timeslot.endTime)).thenReturn(false);
        when(timeslotRepository.calculateTeacherDailyHours(teacher.getId(), timeslot.day.getValue()))
                .thenReturn(2.0);

        // When
        boolean result = timeSlotService.canAssign(section, teacher, classroom, timeslot);

        // Then
        assertThat(result).isTrue();
        verify(timeslotRepository).hasTeacherOrRoomConflict(
                teacher.getId(), classroom.getId(), timeslot.day.getValue(),
                timeslot.startTime, timeslot.endTime);
        verify(timeslotRepository).calculateTeacherDailyHours(teacher.getId(), timeslot.day.getValue());
    }

    @Test
    @DisplayName("Should return false when teacher or room conflict exists")
    void shouldReturnFalseWhenTeacherOrRoomConflictExists() {
        // Given
        CourseSection section = createCourseSection(1L, 1L, 1L, 4);
        Teacher teacher = createTeacher(1L);
        Classroom classroom = createClassroom(1L);
        TimeSlot timeslot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        when(timeslotRepository.hasTeacherOrRoomConflict(
                teacher.getId(), classroom.getId(), timeslot.day.getValue(),
                timeslot.startTime, timeslot.endTime)).thenReturn(true);

        // When
        boolean result = timeSlotService.canAssign(section, teacher, classroom, timeslot);

        // Then
        assertThat(result).isFalse();
        verify(timeslotRepository, never()).calculateTeacherDailyHours(any(), any());
    }

    @Test
    @DisplayName("Should return false when teacher exceeds daily limit")
    void shouldReturnFalseWhenTeacherExceedsDailyLimit() {
        // Given
        CourseSection section = createCourseSection(1L, 1L, 1L, 4);
        Teacher teacher = createTeacher(1L);
        Classroom classroom = createClassroom(1L);
        TimeSlot timeslot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        when(timeslotRepository.hasTeacherOrRoomConflict(
                teacher.getId(), classroom.getId(), timeslot.day.getValue(),
                timeslot.startTime, timeslot.endTime)).thenReturn(false);
        when(timeslotRepository.calculateTeacherDailyHours(teacher.getId(), timeslot.day.getValue()))
                .thenReturn(3.5); // Already has 3.5 hours, adding 1 hour would exceed 4

        // When
        boolean result = timeSlotService.canAssign(section, teacher, classroom, timeslot);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when teacher daily hours is null")
    void shouldReturnTrueWhenTeacherDailyHoursIsNull() {
        // Given
        CourseSection section = createCourseSection(1L, 1L, 1L, 4);
        Teacher teacher = createTeacher(1L);
        Classroom classroom = createClassroom(1L);
        TimeSlot timeslot = new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        when(timeslotRepository.hasTeacherOrRoomConflict(
                teacher.getId(), classroom.getId(), timeslot.day.getValue(),
                timeslot.startTime, timeslot.endTime)).thenReturn(false);
        when(timeslotRepository.calculateTeacherDailyHours(teacher.getId(), timeslot.day.getValue()))
                .thenReturn(null);

        // When
        boolean result = timeSlotService.canAssign(section, teacher, classroom, timeslot);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should assign section to teacher, room, and timeslots")
    void shouldAssignSectionToTeacherRoomAndTimeslots() {
        // Given
        CourseSection section = createCourseSection(1L, 1L, 1L, 4);
        Teacher teacher = createTeacher(1L);
        Classroom classroom = createClassroom(1L);
        List<TimeSlot> timeslots = Arrays.asList(
                new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new TimeSlot(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0))
        );

        when(timeslotRepository.save(any(SectionTimeslot.class))).thenAnswer(invocation -> {
            SectionTimeslot st = invocation.getArgument(0);
            st.setId((long) (st.getDayOfWeek() + 100));
            return st;
        });

        // When
        timeSlotService.assign(section, teacher, classroom, timeslots);

        // Then
        assertThat(section.getTeacherId()).isEqualTo(teacher.getId());
        assertThat(section.getClassroomId()).isEqualTo(classroom.getId());
        assertThat(section.getStatus()).isEqualTo(SectionStatus.SCHEDULED);
        verify(timeslotRepository, times(2)).save(any(SectionTimeslot.class));
    }

    @Test
    @DisplayName("Should handle empty timeslots list")
    void shouldHandleEmptyTimeslotsList() {
        // Given
        CourseSection section = createCourseSection(1L, 1L, 1L, 4);
        Teacher teacher = createTeacher(1L);
        Classroom classroom = createClassroom(1L);
        List<TimeSlot> timeslots = Collections.emptyList();

        // When
        timeSlotService.assign(section, teacher, classroom, timeslots);

        // Then
        assertThat(section.getTeacherId()).isEqualTo(teacher.getId());
        assertThat(section.getClassroomId()).isEqualTo(classroom.getId());
        assertThat(section.getStatus()).isEqualTo(SectionStatus.SCHEDULED);
        verify(timeslotRepository, never()).save(any(SectionTimeslot.class));
    }

    // Helper methods

    private CourseSection createCourseSection(Long id, Long courseId, Long semesterId, Integer hoursPerWeek) {
        CourseSection section = new CourseSection(courseId, semesterId, 1, hoursPerWeek);
        section.setId(id);
        section.setStatus(SectionStatus.UNSCHEDULED);
        return section;
    }

    private Teacher createTeacher(Long id) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setFirstName("John");
        teacher.setLastName("Doe");
        return teacher;
    }

    private Classroom createClassroom(Long id) {
        Classroom classroom = new Classroom();
        classroom.setId(id);
        classroom.setName("Room 101");
        return classroom;
    }
}

