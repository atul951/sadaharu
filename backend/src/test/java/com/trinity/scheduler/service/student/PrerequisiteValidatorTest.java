package com.trinity.scheduler.service.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trinity.scheduler.entity.Course;
import com.trinity.scheduler.repository.CourseRepository;
import com.trinity.scheduler.repository.StudentCourseHistoryRepository;
import com.trinity.scheduler.service.student.PrerequisiteValidator;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrerequisiteValidator.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 *
 * @author Atul Kumar
 */
@ExtendWith(MockitoExtension.class)
class PrerequisiteValidatorTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentCourseHistoryRepository historyRepository;

    private PrerequisiteValidator prerequisiteValidator;

    @BeforeEach
    void setUp() {
        prerequisiteValidator = new PrerequisiteValidator(
                courseRepository,
                historyRepository
        );
    }

    @Test
    @DisplayName("Should return true when course has no prerequisite")
    void shouldReturnTrueWhenCourseHasNoPrerequisite() {
        // Given
        Long studentId = 1L;
        Long courseId = 1L;
        Course course = createCourse(courseId, "MATH101", null);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // When
        boolean result = prerequisiteValidator.hasMetPrerequisites(studentId, courseId);

        // Then
        assertThat(result).isTrue();
        verify(historyRepository, never()).hasPassedCourse(any(), any());
    }

    @Test
    @DisplayName("Should return true when prerequisite is met")
    void shouldReturnTrueWhenPrerequisiteIsMet() {
        // Given
        Long studentId = 1L;
        Long courseId = 2L;
        Long prerequisiteId = 1L;
        Course course = createCourse(courseId, "MATH102", prerequisiteId);
        Course prerequisite = createCourse(prerequisiteId, "MATH101", null);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.findById(prerequisiteId)).thenReturn(Optional.of(prerequisite));
        when(historyRepository.hasPassedCourse(studentId, prerequisiteId)).thenReturn(true);

        // When
        boolean result = prerequisiteValidator.hasMetPrerequisites(studentId, courseId);

        // Then
        assertThat(result).isTrue();
        verify(historyRepository).hasPassedCourse(studentId, prerequisiteId);
    }

    @Test
    @DisplayName("Should return false when prerequisite is not met")
    void shouldReturnFalseWhenPrerequisiteIsNotMet() {
        // Given
        Long studentId = 1L;
        Long courseId = 2L;
        Long prerequisiteId = 1L;
        Course course = createCourse(courseId, "MATH102", prerequisiteId);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(historyRepository.hasPassedCourse(studentId, prerequisiteId)).thenReturn(false);

        // When
        boolean result = prerequisiteValidator.hasMetPrerequisites(studentId, courseId);

        // Then
        assertThat(result).isFalse();
        verify(historyRepository).hasPassedCourse(studentId, prerequisiteId);
    }

    @Test
    @DisplayName("Should return false when course not found")
    void shouldReturnFalseWhenCourseNotFound() {
        // Given
        Long studentId = 1L;
        Long courseId = 999L;
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        // When
        boolean result = prerequisiteValidator.hasMetPrerequisites(studentId, courseId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle recursive prerequisites")
    void shouldHandleRecursivePrerequisites() {
        // Given
        Long studentId = 1L;
        Long courseId = 3L;
        Long prerequisiteId1 = 2L;
        Long prerequisiteId2 = 1L;
        Course course3 = createCourse(courseId, "MATH201", prerequisiteId1);
        Course course2 = createCourse(prerequisiteId1, "MATH102", prerequisiteId2);
        Course course1 = createCourse(prerequisiteId2, "MATH101", null);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course3));
        when(courseRepository.findById(prerequisiteId1)).thenReturn(Optional.of(course2));
        when(courseRepository.findById(prerequisiteId2)).thenReturn(Optional.of(course1));
        when(historyRepository.hasPassedCourse(studentId, prerequisiteId1)).thenReturn(true);
        when(historyRepository.hasPassedCourse(studentId, prerequisiteId2)).thenReturn(true);

        // When
        boolean result = prerequisiteValidator.hasMetPrerequisites(studentId, courseId);

        // Then
        assertThat(result).isTrue();
        verify(historyRepository).hasPassedCourse(studentId, prerequisiteId1);
        verify(historyRepository).hasPassedCourse(studentId, prerequisiteId2);
    }

    @Test
    @DisplayName("Should get prerequisite chain")
    void shouldGetPrerequisiteChain() {
        // Given
        Long courseId = 3L;
        Long prerequisiteId1 = 2L;
        Long prerequisiteId2 = 1L;
        Course course3 = createCourse(courseId, "MATH201", prerequisiteId1);
        Course course2 = createCourse(prerequisiteId1, "MATH102", prerequisiteId2);
        Course course1 = createCourse(prerequisiteId2, "MATH101", null);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course3));
        when(courseRepository.findById(prerequisiteId1)).thenReturn(Optional.of(course2));
        when(courseRepository.findById(prerequisiteId2)).thenReturn(Optional.of(course1));

        // When
        List<Long> chain = prerequisiteValidator.getPrerequisiteChain(courseId);

        // Then
        assertThat(chain).hasSize(3);
        assertThat(chain).containsExactly(courseId, prerequisiteId1, prerequisiteId2);
    }

    @Test
    @DisplayName("Should get empty chain when course has no prerequisite")
    void shouldGetEmptyChainWhenCourseHasNoPrerequisite() {
        // Given
        Long courseId = 1L;
        Course course = createCourse(courseId, "MATH101", null);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // When
        List<Long> chain = prerequisiteValidator.getPrerequisiteChain(courseId);

        // Then
        assertThat(chain).hasSize(1);
        assertThat(chain).containsExactly(courseId);
    }

    @Test
    @DisplayName("Should get missing prerequisites")
    void shouldGetMissingPrerequisites() {
        // Given
        Long studentId = 1L;
        Long courseId = 3L;
        Long prerequisiteId1 = 2L;
        Long prerequisiteId2 = 1L;
        Course course3 = createCourse(courseId, "MATH201", prerequisiteId1);
        Course course2 = createCourse(prerequisiteId1, "MATH102", prerequisiteId2);
        Course course1 = createCourse(prerequisiteId2, "MATH101", null);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course3));
        when(courseRepository.findById(prerequisiteId1)).thenReturn(Optional.of(course2));
        when(courseRepository.findById(prerequisiteId2)).thenReturn(Optional.of(course1));
        when(historyRepository.hasPassedCourse(studentId, prerequisiteId1)).thenReturn(false);
        when(historyRepository.hasPassedCourse(studentId, prerequisiteId2)).thenReturn(true);

        // When
        List<Long> missing = prerequisiteValidator.getMissingPrerequisites(studentId, courseId);

        // Then
        assertThat(missing).hasSize(1);
        assertThat(missing).contains(prerequisiteId1);
    }

    @Test
    @DisplayName("Should return empty list when all prerequisites are met")
    void shouldReturnEmptyListWhenAllPrerequisitesAreMet() {
        // Given
        Long studentId = 1L;
        Long courseId = 2L;
        Long prerequisiteId = 1L;
        Course course = createCourse(courseId, "MATH102", prerequisiteId);
        Course prerequisite = createCourse(prerequisiteId, "MATH101", null);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseRepository.findById(prerequisiteId)).thenReturn(Optional.of(prerequisite));
        when(historyRepository.hasPassedCourse(studentId, prerequisiteId)).thenReturn(true);

        // When
        List<Long> missing = prerequisiteValidator.getMissingPrerequisites(studentId, courseId);

        // Then
        assertThat(missing).isEmpty();
    }

    // Helper methods

    private Course createCourse(Long id, String code, Long prerequisiteId) {
        Course course = new Course();
        course.setId(id);
        course.setCode(code);
        course.setName("Test Course");
        course.setPrerequisiteId(prerequisiteId);
        return course;
    }
}

