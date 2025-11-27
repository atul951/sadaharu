package com.trinity.scheduler.service.course;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for Course Sections.
 */
@Service
public class CourseSectionService {
    private static final Logger log = LoggerFactory.getLogger(CourseSectionService.class);
    private final CourseSectionMapper courseSectionMapper;
    private final CourseRepository courseRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherRepository teacherRepository;

    /**
     * Constructor with dependency injection.
     */
    public CourseSectionService(
            CourseSectionMapper courseSectionMapper,
            CourseRepository courseRepository,
            CourseSectionRepository courseSectionRepository,
            ClassroomRepository classroomRepository,
            TeacherRepository teacherRepository
    ) {
        this.courseSectionMapper = courseSectionMapper;
        this.courseRepository = courseRepository;
        this.courseSectionRepository = courseSectionRepository;
        this.classroomRepository = classroomRepository;
        this.teacherRepository = teacherRepository;
    }

    /**
     * Retrieves sections by semester with optional filters.
     *
     * @param semesterId the semester ID
     * @param courseId   optional course ID filter
     * @param status     optional status filter
     * @return list of course sections
     */
    public List<CourseSection> getSectionsBySemester(Long semesterId, Long courseId, SectionStatus status) {
        if (courseId != null) {
            return courseSectionRepository.findByCourseIdAndSemesterId(courseId, semesterId);
        } else if (status != null) {
            return courseSectionRepository.findBySemesterIdAndStatus(semesterId, status);
        } else {
            return courseSectionRepository.findBySemesterId(semesterId);
        }
    }

    /**
     * Maps CourseSection entities to Section models.
     *
     * @param courseSections list of course sections
     * @return list of Section models
     */
    public List<Section> getSectionsFromCourseSections(List<CourseSection> courseSections) {
        // Fetch related courses and map them for easy access
        Map<Long, Course> coursesMap = getCoursesOfSections(courseSections)
                .stream().collect(Collectors.toMap(Course::getId, course -> course));

        // Fetch related teachers and map them for easy access
        Map<Long, Teacher> teachersMap = getTeachersOfSections(courseSections)
                .stream().collect(Collectors.toMap(Teacher::getId, teacher -> teacher));

        // Fetch related classrooms and map them for easy access
        Map<Long, Classroom> classroomsMap = getClassroomsOfSections(courseSections)
                .stream().collect(Collectors.toMap(Classroom::getId, classroom -> classroom));

        return courseSections.stream()
                .map(section -> courseSectionMapper.from(
                        section,
                        coursesMap,
                        teachersMap,
                        classroomsMap
                ))
                .toList();
    }

    /**
     * Retrieves courses for the given sections.
     *
     * @param sections list of course sections
     * @return list of courses
     */
    private List<Course> getCoursesOfSections(List<CourseSection> sections) {
        List<Long> courseIds = sections.stream()
                .map(CourseSection::getCourseId)
                .distinct()
                .toList();
        return courseRepository.findAllById(courseIds);
    }

    /**
     * Retrieves teachers for the given sections.
     *
     * @param sections list of course sections
     * @return list of teachers
     */
    private List<Teacher> getTeachersOfSections(List<CourseSection> sections) {
        List<Long> teacherIds = sections.stream()
                .map(CourseSection::getTeacherId)
                .distinct()
                .toList();
        return teacherRepository.findAllById(teacherIds);
    }

    /**
     * Retrieves classrooms for the given sections.
     *
     * @param sections list of course sections
     * @return list of classrooms
     */
    private List<Classroom> getClassroomsOfSections(List<CourseSection> sections) {
        List<Long> classroomIds = sections.stream()
                .map(CourseSection::getClassroomId)
                .distinct()
                .toList();
        return classroomRepository.findAllById(classroomIds);
    }

    /**
     * Creates section entities based on demand.
     *
     * @param demand     map of course to section count
     * @param semesterId semester ID
     * @return map of course to created sections
     */
    public Map<Course, List<CourseSection>> createSections(Map<Course, Integer> demand, Long semesterId) {
        Map<Course, List<CourseSection>> courseSections = new HashMap<>();

        for (Map.Entry<Course, Integer> entry : demand.entrySet()) {
            Course course = entry.getKey();
            int sectionCount = entry.getValue();

            // Check if sections already exist for this course and semester
            List<CourseSection> sections = courseSectionRepository.findByCourseIdAndSemesterId(course.getId(), semesterId);
            if (!sections.isEmpty()) {
                log.info("Sections already exist for course {} in semester {}, skipping creation",
                        course.getCode(), semesterId);
                courseSections.put(course, sections);
                continue;
            } else {
                sections = new ArrayList<>();
            }

            log.info("Creating {} sections for course {} ({} hours/week)",
                    sectionCount, course.getCode(), course.getHoursPerWeek());

            for (int i = 1; i <= sectionCount; i++) {
                CourseSection section = new CourseSection(course.getId(), semesterId, i, course.getHoursPerWeek());
                section = courseSectionRepository.save(section);
                sections.add(section);
            }

            // Sort by priority
            sortByPriority(sections);

            log.info("Created {} sections for course {}", sections.size(), course.getCode());
            courseSections.put(course, sections);
        }

        return courseSections;
    }

    /**
     * Sorts sections by priority.
     *
     * <p>Priority order:
     * <ol>
     *   <li>Core courses before electives (lower course code typically means core)</li>
     *   <li>Higher hours per week first (harder to schedule)</li>
     * </ol>
     *
     * @param sections list of sections to sort
     */
    private void sortByPriority(List<CourseSection> sections) {
        sections.sort((s1, s2) -> {
            // Sort by hours per week descending
            int hourCompare = Integer.compare(s2.getHoursPerWeek(), s1.getHoursPerWeek());
            if (hourCompare != 0) {
                return hourCompare;
            }
            // Then by course ID (lower IDs are typically core courses)
            return Long.compare(s1.getCourseId(), s2.getCourseId());
        });
    }
}
