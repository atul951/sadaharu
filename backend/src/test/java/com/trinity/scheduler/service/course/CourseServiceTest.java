package com.trinity.scheduler.service.course;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trinity.scheduler.repository.CourseRepository;
import com.trinity.scheduler.service.course.CourseService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CourseService.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 *
 * @author Atul Kumar
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository);
    }

    @Test
    @DisplayName("Should return total credits for given course IDs")
    void shouldReturnTotalCreditsForCourseIds() {
        // Given
        List<Long> courseIds = Arrays.asList(1L, 2L, 3L);
        Integer expectedCredits = 9;

        when(courseRepository.countCreditsByCourseIdsIn(courseIds)).thenReturn(expectedCredits);

        // When
        Integer result = courseService.getCoursesCredits(courseIds);

        // Then
        assertThat(result).isEqualTo(expectedCredits);
        verify(courseRepository).countCreditsByCourseIdsIn(courseIds);
    }

    @Test
    @DisplayName("Should return zero credits for empty course IDs list")
    void shouldReturnZeroCreditsForEmptyCourseIdsList() {
        // Given
        List<Long> courseIds = Collections.emptyList();
        Integer expectedCredits = 0;

        when(courseRepository.countCreditsByCourseIdsIn(courseIds)).thenReturn(expectedCredits);

        // When
        Integer result = courseService.getCoursesCredits(courseIds);

        // Then
        assertThat(result).isEqualTo(0);
        verify(courseRepository).countCreditsByCourseIdsIn(courseIds);
    }

    @Test
    @DisplayName("Should return null when repository returns null")
    void shouldReturnNullWhenRepositoryReturnsNull() {
        // Given
        List<Long> courseIds = Arrays.asList(1L, 2L);

        when(courseRepository.countCreditsByCourseIdsIn(courseIds)).thenReturn(null);

        // When
        Integer result = courseService.getCoursesCredits(courseIds);

        // Then
        assertThat(result).isNull();
        verify(courseRepository).countCreditsByCourseIdsIn(courseIds);
    }
}

