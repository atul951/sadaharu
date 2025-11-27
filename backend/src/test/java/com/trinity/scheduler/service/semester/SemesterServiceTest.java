package com.trinity.scheduler.service.semester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trinity.scheduler.entity.Semester;
import com.trinity.scheduler.repository.CourseSectionRepository;
import com.trinity.scheduler.repository.SectionTimeslotRepository;
import com.trinity.scheduler.repository.SemesterRepository;
import com.trinity.scheduler.service.semester.SemesterService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SemesterService.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 *
 * @author Atul Kumar
 */
@ExtendWith(MockitoExtension.class)
class SemesterServiceTest {

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private CourseSectionRepository courseSectionRepository;

    @Mock
    private SectionTimeslotRepository sectionTimeslotRepository;

    private SemesterService semesterService;

    @BeforeEach
    void setUp() {
        semesterService = new SemesterService(
                semesterRepository,
                courseSectionRepository,
                sectionTimeslotRepository
        );
    }

    @Test
    @DisplayName("Should retrieve semester by ID")
    void shouldRetrieveSemesterById() {
        // Given
        Long semesterId = 1L;
        Semester expectedSemester = createSemester(semesterId, 1, 2024);

        when(semesterRepository.findById(semesterId)).thenReturn(Optional.of(expectedSemester));

        // When
        Semester result = semesterService.getSemesterById(semesterId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(semesterId);
        assertThat(result.getOrderInYear()).isEqualTo(1);
        assertThat(result.getYear()).isEqualTo(2024);
        verify(semesterRepository).findById(semesterId);
    }

    @Test
    @DisplayName("Should throw exception when semester not found")
    void shouldThrowExceptionWhenSemesterNotFound() {
        // Given
        Long semesterId = 999L;
        when(semesterRepository.findById(semesterId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> semesterService.getSemesterById(semesterId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Semester not found with ID: " + semesterId);
    }

    @Test
    @DisplayName("Should revert all schedule data")
    void shouldRevertAllScheduleData() {
        // When
        semesterService.revertAllScheduleData();

        // Then
        verify(courseSectionRepository).deleteAll();
        verify(sectionTimeslotRepository).deleteAll();
    }

    @Test
    @DisplayName("Should revert schedule data for specific semester")
    void shouldRevertScheduleDataForSpecificSemester() {
        // Given
        Long semesterId = 1L;

        // When
        semesterService.revertScheduleData(semesterId);

        // Then
        verify(courseSectionRepository).deleteBySemesterId(semesterId);
        verify(sectionTimeslotRepository, never()).deleteAll();
    }

    // Helper methods

    private Semester createSemester(Long id, Integer orderInYear, Integer year) {
        Semester semester = new Semester();
        semester.setId(id);
        semester.setOrderInYear(orderInYear);
        semester.setYear(year);
        semester.setName("Fall " + year);
        return semester;
    }
}

