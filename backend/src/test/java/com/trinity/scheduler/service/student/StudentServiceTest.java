package com.trinity.scheduler.service.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trinity.scheduler.entity.Student;
import com.trinity.scheduler.entity.StudentCourseHistory;
import com.trinity.scheduler.repository.StudentCourseHistoryRepository;
import com.trinity.scheduler.repository.StudentRepository;
import com.trinity.scheduler.service.student.StudentService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StudentService.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 *
 * @author Atul Kumar
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentCourseHistoryRepository studentCourseHistoryRepository;

    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentService = new StudentService(
                studentRepository,
                studentCourseHistoryRepository
        );
    }

    @Test
    @DisplayName("Should retrieve student by ID")
    void shouldRetrieveStudentById() {
        // Given
        Long studentId = 1L;
        Student expectedStudent = createStudent(studentId, "John", "Doe", 9);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(expectedStudent));

        // When
        Student result = studentService.getStudentById(studentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(studentId);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getGradeLevel()).isEqualTo(9);
        verify(studentRepository).findById(studentId);
    }

    @Test
    @DisplayName("Should throw exception when student not found")
    void shouldThrowExceptionWhenStudentNotFound() {
        // Given
        Long studentId = 999L;
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> studentService.getStudentById(studentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Student not found with ID: " + studentId);
    }

    @Test
    @DisplayName("Should retrieve student course history")
    void shouldRetrieveStudentCourseHistory() {
        // Given
        Long studentId = 1L;
        StudentCourseHistory history1 = createHistory(1L, studentId, 1L);
        StudentCourseHistory history2 = createHistory(2L, studentId, 2L);
        List<StudentCourseHistory> expectedHistory = Arrays.asList(history1, history2);

        when(studentCourseHistoryRepository.findByStudentId(studentId)).thenReturn(expectedHistory);

        // When
        List<StudentCourseHistory> result = studentService.getStudentCourseHistory(studentId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(history1, history2);
        verify(studentCourseHistoryRepository).findByStudentId(studentId);
    }

    @Test
    @DisplayName("Should return empty list when student has no course history")
    void shouldReturnEmptyListWhenStudentHasNoCourseHistory() {
        // Given
        Long studentId = 1L;
        when(studentCourseHistoryRepository.findByStudentId(studentId))
                .thenReturn(Collections.emptyList());

        // When
        List<StudentCourseHistory> result = studentService.getStudentCourseHistory(studentId);

        // Then
        assertThat(result).isEmpty();
        verify(studentCourseHistoryRepository).findByStudentId(studentId);
    }

    // Helper methods

    private Student createStudent(Long id, String firstName, String lastName, Integer gradeLevel) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setGradeLevel(gradeLevel);
        student.setEnrollmentYear(2020);
        student.setExpectedGraduationYear(2024);
        student.setStatus("active");
        return student;
    }

    private StudentCourseHistory createHistory(Long id, Long studentId, Long courseId) {
        StudentCourseHistory history = new StudentCourseHistory();
        history.setId(id);
        history.setStudentId(studentId);
        history.setCourseId(courseId);
        history.setStatus("passed");
        return history;
    }
}

