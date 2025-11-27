package com.trinity.scheduler.service.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trinity.scheduler.dto.EnrollmentValidationResult;
import com.trinity.scheduler.entity.CourseSection;
import com.trinity.scheduler.entity.SectionTimeslot;
import com.trinity.scheduler.entity.Student;
import com.trinity.scheduler.entity.StudentEnrollment;
import com.trinity.scheduler.entity.enums.EnrollmentStatus;
import com.trinity.scheduler.entity.enums.SectionStatus;
import com.trinity.scheduler.exception.EnrollmentException;
import com.trinity.scheduler.repository.CourseSectionRepository;
import com.trinity.scheduler.repository.SectionTimeslotRepository;
import com.trinity.scheduler.repository.StudentEnrollmentRepository;
import com.trinity.scheduler.repository.StudentRepository;
import com.trinity.scheduler.service.student.EnrollmentService;
import com.trinity.scheduler.service.student.PrerequisiteValidator;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnrollmentService.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 *
 * @author Atul Kumar
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private CourseSectionRepository sectionRepository;

    @Mock
    private PrerequisiteValidator prerequisiteValidator;

    @Mock
    private SectionTimeslotRepository timeslotRepository;

    @Mock
    private StudentRepository studentRepository;

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(
                enrollmentRepository,
                sectionRepository,
                prerequisiteValidator,
                studentRepository,
                timeslotRepository
        );
    }

    @Test
    @DisplayName("Should enroll student successfully")
    void shouldEnrollStudentSuccessfully() {
        // Given
        Long studentId = 1L;
        Long sectionId = 1L;
        CourseSection section = createCourseSection(sectionId, 1L, 1L, 4);
        section.setStatus(SectionStatus.SCHEDULED);

        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.isEnrolledInCourse(studentId, section.getCourseId(), section.getSemesterId()))
                .thenReturn(false);
        when(prerequisiteValidator.hasMetPrerequisites(studentId, section.getCourseId())).thenReturn(true);
        when(timeslotRepository.findBySectionId(sectionId)).thenReturn(Collections.emptyList());

        when(enrollmentRepository.countActiveEnrollments(studentId, section.getSemesterId())).thenReturn(2L);
        when(enrollmentRepository.save(any(StudentEnrollment.class))).thenAnswer(invocation -> {
            StudentEnrollment enrollment = invocation.getArgument(0);
            enrollment.setId(1L);
            return enrollment;
        });

        // When
        StudentEnrollment result = enrollmentService.enrollStudentInCourse(studentId, sectionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        verify(enrollmentRepository).save(any(StudentEnrollment.class));
    }

    @Test
    @DisplayName("Should throw exception when section not found")
    void shouldThrowExceptionWhenSectionNotFound() {
        // Given
        Long studentId = 1L;
        Long sectionId = 999L;
        when(sectionRepository.findById(sectionId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollStudentInCourse(studentId, sectionId))
                .isInstanceOf(EnrollmentException.class)
                .hasMessageContaining("Section not found");
    }

    @Test
    @DisplayName("Should throw exception when section not available")
    void shouldThrowExceptionWhenSectionNotAvailable() {
        // Given
        Long studentId = 1L;
        Long sectionId = 1L;
        CourseSection section = createCourseSection(sectionId, 1L, 1L, 4);
        section.setStatus(SectionStatus.UNSCHEDULED);

        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));

        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollStudentInCourse(studentId, sectionId))
                .isInstanceOf(EnrollmentException.class)
                .hasMessageContaining("Section is not available");
    }

    @Test
    @DisplayName("Should throw exception when already enrolled in section")
    void shouldThrowExceptionWhenAlreadyEnrolledInSection() {
        // Given
        Long studentId = 1L;
        Long sectionId = 1L;
        CourseSection section = createCourseSection(sectionId, 1L, 1L, 4);
        section.setStatus(SectionStatus.SCHEDULED);
        StudentEnrollment existingEnrollment = new StudentEnrollment();

        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId))
                .thenReturn(Optional.of(existingEnrollment));

        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollStudentInCourse(studentId, sectionId))
                .isInstanceOf(EnrollmentException.class)
                .hasMessageContaining("already enrolled");
    }

    @Test
    @DisplayName("Should throw exception when prerequisites not met")
    void shouldThrowExceptionWhenPrerequisitesNotMet() {
        // Given
        Long studentId = 1L;
        Long sectionId = 1L;
        CourseSection section = createCourseSection(sectionId, 1L, 1L, 4);
        section.setStatus(SectionStatus.SCHEDULED);

        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.isEnrolledInCourse(studentId, section.getCourseId(), section.getSemesterId()))
                .thenReturn(false);
        when(prerequisiteValidator.hasMetPrerequisites(studentId, section.getCourseId())).thenReturn(false);
        when(prerequisiteValidator.getMissingPrerequisites(studentId, section.getCourseId()))
                .thenReturn(List.of(100L));

        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollStudentInCourse(studentId, sectionId))
                .isInstanceOf(EnrollmentException.class)
                .hasMessageContaining("Prerequisites not met");
    }

    @Test
    @DisplayName("Should throw exception when time conflict exists")
    void shouldThrowExceptionWhenTimeConflictExists() {
        // Given
        Long studentId = 1L;
        Long sectionId = 1L;
        CourseSection section = createCourseSection(sectionId, 1L, 1L, 4);
        section.setStatus(SectionStatus.SCHEDULED);
        SectionTimeslot timeslot = createTimeslot(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0));

        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.isEnrolledInCourse(studentId, section.getCourseId(), section.getSemesterId()))
                .thenReturn(false);
        when(prerequisiteValidator.hasMetPrerequisites(studentId, section.getCourseId())).thenReturn(true);
        when(timeslotRepository.findBySectionId(sectionId)).thenReturn(List.of(timeslot));
        when(enrollmentRepository.hasTimeConflict(anyLong(), anyInt(), anyString(), anyString()))
                .thenReturn(1);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollStudentInCourse(studentId, sectionId))
                .isInstanceOf(EnrollmentException.class)
                .hasMessageContaining("Time conflict");
    }

    @Test
    @DisplayName("Should throw exception when course load limit reached")
    void shouldThrowExceptionWhenCourseLoadLimitReached() {
        // Given
        Long studentId = 1L;
        Long sectionId = 1L;
        CourseSection section = createCourseSection(sectionId, 1L, 1L, 4);
        section.setStatus(SectionStatus.SCHEDULED);

        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.isEnrolledInCourse(studentId, section.getCourseId(), section.getSemesterId()))
                .thenReturn(false);
        when(prerequisiteValidator.hasMetPrerequisites(studentId, section.getCourseId())).thenReturn(true);
        when(timeslotRepository.findBySectionId(sectionId)).thenReturn(Collections.emptyList());

        when(enrollmentRepository.countActiveEnrollments(studentId, section.getSemesterId())).thenReturn(5L);

        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollStudentInCourse(studentId, sectionId))
                .isInstanceOf(EnrollmentException.class)
                .hasMessageContaining("Maximum course load");
    }

    @Test
    @DisplayName("Should waitlist student when section is full")
    void shouldWaitlistStudentWhenSectionIsFull() {
        // Given
        Long studentId = 1L;
        Long sectionId = 1L;
        CourseSection section = createCourseSection(sectionId, 1L, 1L, 4);
        section.setStatus(SectionStatus.SCHEDULED);
        section.setCapacity(10);
        // Make section full by setting enrolled count
        for (int i = 0; i < 10; i++) {
            StudentEnrollment enrollment = new StudentEnrollment((long) (i + 100), section);
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            section.addEnrollment(enrollment);
        }

        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.isEnrolledInCourse(studentId, section.getCourseId(), section.getSemesterId()))
                .thenReturn(false);
        when(prerequisiteValidator.hasMetPrerequisites(studentId, section.getCourseId())).thenReturn(true);
        when(timeslotRepository.findBySectionId(sectionId)).thenReturn(Collections.emptyList());

        when(enrollmentRepository.countActiveEnrollments(studentId, section.getSemesterId())).thenReturn(2L);
        when(enrollmentRepository.save(any(StudentEnrollment.class))).thenAnswer(invocation -> {
            StudentEnrollment enrollment = invocation.getArgument(0);
            enrollment.setId(1L);
            return enrollment;
        });

        // When
        StudentEnrollment result = enrollmentService.enrollStudentInCourse(studentId, sectionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
        verify(enrollmentRepository).save(any(StudentEnrollment.class));
    }

    @Test
    @DisplayName("Should drop student from section")
    void shouldDropStudentFromSection() {
        // Given
        Long studentId = 1L;
        Long sectionId = 1L;
        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setId(1L);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId))
                .thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        // When
        enrollmentService.drop(studentId, sectionId);

        // Then
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    @DisplayName("Should throw exception when dropping non-existent enrollment")
    void shouldThrowExceptionWhenDroppingNonExistentEnrollment() {
        // Given
        Long studentId = 1L;
        Long sectionId = 1L;
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentService.drop(studentId, sectionId))
                .isInstanceOf(EnrollmentException.class)
                .hasMessageContaining("Enrollment not found");
    }

    @Test
    @DisplayName("Should get student enrollments for semester")
    void shouldGetStudentEnrollmentsForSemester() {
        // Given
        Long studentId = 1L;
        Long semesterId = 1L;
        List<StudentEnrollment> expectedEnrollments = List.of(new StudentEnrollment());

        when(enrollmentRepository.findActiveEnrollmentsBySemester(studentId, semesterId))
                .thenReturn(expectedEnrollments);

        // When
        List<StudentEnrollment> result = enrollmentService.getStudentEnrollments(studentId, semesterId);

        // Then
        assertThat(result).isEqualTo(expectedEnrollments);
        verify(enrollmentRepository).findActiveEnrollmentsBySemester(studentId, semesterId);
    }

    @Test
    @DisplayName("Should validate enrollment successfully")
    void shouldValidateEnrollmentSuccessfully() {
        // Given
        Long studentId = 1L;
        Long sectionId = 1L;
        Student student = createStudent(studentId);
        CourseSection section = createCourseSection(sectionId, 1L, 1L, 4);
        section.setStatus(SectionStatus.SCHEDULED);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.isEnrolledInCourse(studentId, section.getCourseId(), section.getSemesterId()))
                .thenReturn(false);
        when(prerequisiteValidator.hasMetPrerequisites(studentId, section.getCourseId())).thenReturn(true);
        when(timeslotRepository.findBySectionId(sectionId)).thenReturn(Collections.emptyList());

        when(enrollmentRepository.countActiveEnrollments(studentId, section.getSemesterId())).thenReturn(2L);

        // When
        EnrollmentValidationResult result = enrollmentService.validateEnrollment(studentId, sectionId);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should return invalid validation when student not found")
    void shouldReturnInvalidValidationWhenStudentNotFound() {
        // Given
        Long studentId = 999L;
        Long sectionId = 1L;
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // When
        EnrollmentValidationResult result = enrollmentService.validateEnrollment(studentId, sectionId);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Student not found");
    }

    // Helper methods

    private CourseSection createCourseSection(Long id, Long courseId, Long semesterId, Integer hoursPerWeek) {
        CourseSection section = new CourseSection(courseId, semesterId, 1, hoursPerWeek);
        section.setId(id);
        section.setStatus(SectionStatus.UNSCHEDULED);
        section.setCapacity(10);
        return section;
    }

    private Student createStudent(Long id) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setGradeLevel(9);
        return student;
    }

    private SectionTimeslot createTimeslot(Long id, DayOfWeek day, LocalTime start, LocalTime end) {
        CourseSection section = createCourseSection(1L, 1L, 1L, 4);
        SectionTimeslot timeslot = new SectionTimeslot(section, day, start, end);
        timeslot.setId(id);
        return timeslot;
    }
}

