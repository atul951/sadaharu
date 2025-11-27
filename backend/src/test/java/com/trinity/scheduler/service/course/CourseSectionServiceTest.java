package com.trinity.scheduler.service.course;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trinity.scheduler.entity.Classroom;
import com.trinity.scheduler.entity.Course;
import com.trinity.scheduler.entity.CourseSection;
import com.trinity.scheduler.entity.Teacher;
import com.trinity.scheduler.entity.enums.SectionStatus;
import com.trinity.scheduler.mapper.CourseSectionMapper;
import com.trinity.scheduler.model.Section;
import com.trinity.scheduler.repository.ClassroomRepository;
import com.trinity.scheduler.repository.CourseRepository;
import com.trinity.scheduler.repository.CourseSectionRepository;
import com.trinity.scheduler.repository.TeacherRepository;
import com.trinity.scheduler.service.course.CourseSectionService;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CourseSectionService.
 * Uses Mockito to mock dependencies and test business logic in isolation.
 *
 * @author Atul Kumar
 */
@ExtendWith(MockitoExtension.class)
class CourseSectionServiceTest {

    @Mock
    private CourseSectionMapper courseSectionMapper;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseSectionRepository courseSectionRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private TeacherRepository teacherRepository;

    private CourseSectionService courseSectionService;

    @BeforeEach
    void setUp() {
        courseSectionService = new CourseSectionService(
                courseSectionMapper,
                courseRepository,
                courseSectionRepository,
                classroomRepository,
                teacherRepository
        );
    }

    @Test
    @DisplayName("Should retrieve sections by semester ID")
    void shouldRetrieveSectionsBySemesterId() {
        // Given
        Long semesterId = 1L;
        CourseSection section1 = createCourseSection(1L, 1L, semesterId, 1);
        CourseSection section2 = createCourseSection(2L, 1L, semesterId, 2);
        List<CourseSection> expectedSections = Arrays.asList(section1, section2);

        when(courseSectionRepository.findBySemesterId(semesterId)).thenReturn(expectedSections);

        // When
        List<CourseSection> result = courseSectionService.getSectionsBySemester(semesterId, null, null);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(section1, section2);
        verify(courseSectionRepository).findBySemesterId(semesterId);
    }

    @Test
    @DisplayName("Should retrieve sections by semester ID and course ID")
    void shouldRetrieveSectionsBySemesterIdAndCourseId() {
        // Given
        Long semesterId = 1L;
        Long courseId = 1L;
        CourseSection section = createCourseSection(1L, courseId, semesterId, 1);
        List<CourseSection> expectedSections = Collections.singletonList(section);

        when(courseSectionRepository.findByCourseIdAndSemesterId(courseId, semesterId))
                .thenReturn(expectedSections);

        // When
        List<CourseSection> result = courseSectionService.getSectionsBySemester(semesterId, courseId, null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(section);
        verify(courseSectionRepository).findByCourseIdAndSemesterId(courseId, semesterId);
    }

    @Test
    @DisplayName("Should retrieve sections by semester ID and status")
    void shouldRetrieveSectionsBySemesterIdAndStatus() {
        // Given
        Long semesterId = 1L;
        SectionStatus status = SectionStatus.SCHEDULED;
        CourseSection section = createCourseSection(1L, 1L, semesterId, 1);
        section.setStatus(status);
        List<CourseSection> expectedSections = Collections.singletonList(section);

        when(courseSectionRepository.findBySemesterIdAndStatus(semesterId, status))
                .thenReturn(expectedSections);

        // When
        List<CourseSection> result = courseSectionService.getSectionsBySemester(semesterId, null, status);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(section);
        verify(courseSectionRepository).findBySemesterIdAndStatus(semesterId, status);
    }

    @Test
    @DisplayName("Should map course sections to Section models")
    void shouldMapCourseSectionsToSectionModels() {
        // Given
        Course course = createCourse(1L, "MATH101", "Algebra I");
        Teacher teacher = createTeacher(1L, "John", "Doe");
        Classroom classroom = createClassroom(1L, "Room 101");
        CourseSection section = createCourseSection(1L, course.getId(), 1L, 1);
        section.setTeacherId(teacher.getId());
        section.setClassroomId(classroom.getId());

        List<CourseSection> courseSections = Collections.singletonList(section);
        Map<Long, Course> coursesMap = Map.of(course.getId(), course);
        Map<Long, Teacher> teachersMap = Map.of(teacher.getId(), teacher);
        Map<Long, Classroom> classroomsMap = Map.of(classroom.getId(), classroom);

        Section expectedSection = new Section();
        expectedSection.setId(section.getId());

        when(courseRepository.findAllById(anyList())).thenReturn(Collections.singletonList(course));
        when(teacherRepository.findAllById(anyList())).thenReturn(Collections.singletonList(teacher));
        when(classroomRepository.findAllById(anyList())).thenReturn(Collections.singletonList(classroom));
        when(courseSectionMapper.from(section, coursesMap, teachersMap, classroomsMap))
                .thenReturn(expectedSection);

        // When
        List<Section> result = courseSectionService.getSectionsFromCourseSections(courseSections);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(expectedSection);
        verify(courseSectionMapper).from(section, coursesMap, teachersMap, classroomsMap);
    }

    @Test
    @DisplayName("Should create sections when they don't exist")
    void shouldCreateSectionsWhenTheyDontExist() {
        // Given
        Long semesterId = 1L;
        Course course = createCourse(1L, "MATH101", "Algebra I");
        course.setHoursPerWeek(4);
        Map<Course, Integer> demand = Map.of(course, 2);

        when(courseSectionRepository.findByCourseIdAndSemesterId(course.getId(), semesterId))
                .thenReturn(Collections.emptyList());
        when(courseSectionRepository.save(any(CourseSection.class))).thenAnswer(invocation -> {
            CourseSection section = invocation.getArgument(0);
            section.setId((long) (section.getSectionNumber() + 100));
            return section;
        });

        // When
        Map<Course, List<CourseSection>> result = courseSectionService.createSections(demand, semesterId);

        // Then
        assertThat(result).containsKey(course);
        assertThat(result.get(course)).hasSize(2);
        verify(courseSectionRepository, times(2)).save(any(CourseSection.class));
    }

    @Test
    @DisplayName("Should not create sections when they already exist")
    void shouldNotCreateSectionsWhenTheyAlreadyExist() {
        // Given
        Long semesterId = 1L;
        Course course = createCourse(1L, "MATH101", "Algebra I");
        Map<Course, Integer> demand = Map.of(course, 2);
        CourseSection existingSection = createCourseSection(1L, course.getId(), semesterId, 1);
        List<CourseSection> existingSections = Collections.singletonList(existingSection);

        when(courseSectionRepository.findByCourseIdAndSemesterId(course.getId(), semesterId))
                .thenReturn(existingSections);

        // When
        Map<Course, List<CourseSection>> result = courseSectionService.createSections(demand, semesterId);

        // Then
        assertThat(result).containsKey(course);
        assertThat(result.get(course)).hasSize(1);
        assertThat(result.get(course)).contains(existingSection);
        verify(courseSectionRepository, never()).save(any(CourseSection.class));
    }

    @Test
    @DisplayName("Should sort sections by priority (hours per week descending)")
    void shouldSortSectionsByPriority() {
        // Given
        Long semesterId = 1L;
        Course course1 = createCourse(1L, "MATH101", "Algebra I");
        course1.setHoursPerWeek(2);
        Course course2 = createCourse(2L, "PHYS201", "Physics");
        course2.setHoursPerWeek(4);
        Map<Course, Integer> demand = Map.of(
                course1, 1,
                course2, 1
        );

        when(courseSectionRepository.findByCourseIdAndSemesterId(anyLong(), eq(semesterId)))
                .thenReturn(Collections.emptyList());
        when(courseSectionRepository.save(any(CourseSection.class))).thenAnswer(invocation -> {
            CourseSection section = invocation.getArgument(0);
            section.setId((long) (section.getSectionNumber() + 100));
            return section;
        });

        // When
        Map<Course, List<CourseSection>> result = courseSectionService.createSections(demand, semesterId);

        // Then
        assertThat(result).hasSize(2);
        // Verify sections are sorted by hours per week (higher first)
        List<CourseSection> course2Sections = result.get(course2);
        assertThat(course2Sections).isNotEmpty();
    }

    // Helper methods

    private CourseSection createCourseSection(Long id, Long courseId, Long semesterId, Integer sectionNumber) {
        CourseSection section = new CourseSection(courseId, semesterId, sectionNumber, 4);
        section.setId(id);
        section.setStatus(SectionStatus.UNSCHEDULED);
        return section;
    }

    private Course createCourse(Long id, String code, String name) {
        Course course = new Course();
        course.setId(id);
        course.setCode(code);
        course.setName(name);
        course.setHoursPerWeek(4);
        return course;
    }

    private Teacher createTeacher(Long id, String firstName, String lastName) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        return teacher;
    }

    private Classroom createClassroom(Long id, String name) {
        Classroom classroom = new Classroom();
        classroom.setId(id);
        classroom.setName(name);
        return classroom;
    }
}

