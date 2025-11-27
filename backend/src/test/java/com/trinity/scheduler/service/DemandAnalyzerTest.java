package com.trinity.scheduler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trinity.scheduler.entity.Course;
import com.trinity.scheduler.entity.Semester;
import com.trinity.scheduler.entity.Student;
import com.trinity.scheduler.repository.CourseRepository;
import com.trinity.scheduler.repository.SemesterRepository;
import com.trinity.scheduler.repository.StudentCourseHistoryRepository;
import com.trinity.scheduler.repository.StudentRepository;
import com.trinity.scheduler.service.scheduler.DemandAnalyzer;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DemandAnalyzer service.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 *
 * @author Atul Kumar
 */
@ExtendWith(MockitoExtension.class)
class DemandAnalyzerTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentCourseHistoryRepository historyRepository;

    @Mock
    private SemesterRepository semesterRepository;

    private DemandAnalyzer demandAnalyzer;

    @BeforeEach
    void setUp() {
        demandAnalyzer = new DemandAnalyzer(
                courseRepository,
                studentRepository,
                historyRepository,
                semesterRepository
        );
    }

    @Test
    @DisplayName("Should throw exception when semester not found")
    void shouldThrowExceptionWhenSemesterNotFound() {
        // Given
        when(semesterRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> demandAnalyzer.analyzeDemand(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Semester not found");
    }

    @Test
    @DisplayName("Should analyze demand for courses")
    void shouldAnalyzeDemandForCourses() {
        // Given
        Semester semester = createSemester(1L, 1, 2024); // Fall semester
        Course course1 = createCourse(1L, 1, 9, 12, null);
        Course course2 = createCourse(2L, 1, 9, 10, null);

        Student student1 = createStudent(100L, 9, 2024);
        Student student2 = createStudent(101L, 10, 2024);
        Student student3 = createStudent(102L, 11, 2024);

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findBySemesterOrder(1)).thenReturn(Arrays.asList(course1, course2));
        when(studentRepository.findByGradeLevelAndYearAndStatus(9, 12, 2024, "active"))
                .thenReturn(Arrays.asList(student1, student2, student3));
        when(studentRepository.findByGradeLevelAndYearAndStatus(9, 10, 2024, "active"))
                .thenReturn(Arrays.asList(student1, student2));
        // when(historyRepository.hasPassedCourse(any(), any())).thenReturn(true);

        // When
        Map<Course, Integer> demand = demandAnalyzer.analyzeDemand(1L);

        // Then
        assertThat(demand).isNotEmpty();
        assertThat(demand).containsKey(course1);
        assertThat(demand).containsKey(course2);

        // Course 1: 3 eligible students (grades 9-12) → 1 section
        assertThat(demand.get(course1)).isEqualTo(1);

        // Course 2: 2 eligible students (grades 9-10) → 1 section
        assertThat(demand.get(course2)).isEqualTo(1);
    }

    @Test
    @DisplayName("Should calculate correct section count for high demand")
    void shouldCalculateCorrectSectionCountForHighDemand() {
        // Given
        Semester semester = createSemester(1L, 1, 2024);
        Course course = createCourse(1L, 1, 9, 12, null);

        // Create 25 eligible students → should need 3 sections (ceil(25/10))
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            students.add(createStudent((long) (100 + i), 9, 2024));
        }

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findBySemesterOrder(1)).thenReturn(Collections.singletonList(course));
        when(studentRepository.findByGradeLevelAndYearAndStatus(9, 12, 2024, "active")).thenReturn(students);
        // when(historyRepository.hasPassedCourse(any(), any())).thenReturn(true);

        // When
        Map<Course, Integer> demand = demandAnalyzer.analyzeDemand(1L);

        // Then
        assertThat(demand.get(course)).isEqualTo(3); // ceil(25/10) = 3
    }

    @Test
    @DisplayName("Should exclude students without prerequisites")
    void shouldExcludeStudentsWithoutPrerequisites() {
        // Given
        Semester semester = createSemester(1L, 1, 2024);
        Course course = createCourse(1L, 1, 9, 12, 100L); // Has prerequisite

        Student student1 = createStudent(100L, 9, 2024);
        Student student2 = createStudent(101L, 9, 2024);

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findBySemesterOrder(1)).thenReturn(Collections.singletonList(course));
        when(studentRepository.findByGradeLevelAndYearAndStatus(9, 12, 2024, "active"))
                .thenReturn(Arrays.asList(student1, student2));

        // Only student1 has passed prerequisite
        when(historyRepository.hasPassedCourse(100L, 100L)).thenReturn(true);
        when(historyRepository.hasPassedCourse(101L, 100L)).thenReturn(false);

        // When
        Map<Course, Integer> demand = demandAnalyzer.analyzeDemand(1L);

        // Then
        assertThat(demand.get(course)).isEqualTo(1); // Only 1 eligible student
    }

    @Test
    @DisplayName("Should exclude students outside grade level range")
    void shouldExcludeStudentsOutsideGradeLevelRange() {
        // Given
        Semester semester = createSemester(1L, 1, 2024);
        Course course = createCourse(1L, 1, 10, 11, null); // Only grades 10-11

        Student student2 = createStudent(101L, 10, 2024); // Eligible
        Student student3 = createStudent(102L, 11, 2024); // Eligible

        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(courseRepository.findBySemesterOrder(1)).thenReturn(Collections.singletonList(course));
        // Repository filters by grade level range, so only students 10-11 are returned
        when(studentRepository.findByGradeLevelAndYearAndStatus(10, 11, 2024, "active"))
                .thenReturn(Arrays.asList(student2, student3));

        // When
        Map<Course, Integer> demand = demandAnalyzer.analyzeDemand(1L);

        // Then
        assertThat(demand.get(course)).isEqualTo(1); // Only 2 eligible students → 1 section
    }

    // Helper methods to create test objects

    private Semester createSemester(Long id, Integer orderInYear, Integer year) {
        Semester semester = new Semester();
        semester.setId(id);
        semester.setOrderInYear(orderInYear);
        semester.setYear(year);
        return semester;
    }

    private Course createCourse(Long id, Integer semesterOrder, Integer minGrade,
                                Integer maxGrade, Long prerequisiteId) {
        Course course = new Course();
        course.setId(id);
        course.setSemesterOrder(semesterOrder);
        course.setGradeLevelMin(minGrade);
        course.setGradeLevelMax(maxGrade);
        course.setPrerequisiteId(prerequisiteId);
        return course;
    }

    private Student createStudent(Long id, Integer gradeLevel, Integer enrollmentYear) {
        Student student = new Student();
        student.setId(id);
        student.setGradeLevel(gradeLevel);
        student.setEnrollmentYear(enrollmentYear);
        student.setExpectedGraduationYear(enrollmentYear + 4); // 4 years to graduate
        student.setStatus("active");
        return student;
    }
}
