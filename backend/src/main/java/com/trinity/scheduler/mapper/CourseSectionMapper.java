package com.trinity.scheduler.mapper;

import org.springframework.stereotype.Component;

import com.trinity.scheduler.dto.SectionDTO;
import com.trinity.scheduler.dto.TimeslotDTO;
import com.trinity.scheduler.entity.Classroom;
import com.trinity.scheduler.entity.Course;
import com.trinity.scheduler.entity.CourseSection;
import com.trinity.scheduler.entity.Teacher;
import com.trinity.scheduler.model.Section;

import java.util.Comparator;
import java.util.Map;

@Component
public class CourseSectionMapper {

    /**
     * Maps/Converts a CourseSection entity to a Section model.
     *
     * @param courseSection the CourseSection entity
     * @param courseMap     map of course IDs to Course entities
     * @param teacherMap    map of teacher IDs to Teacher entities
     * @param classroomMap  map of classroom IDs to Classroom entities
     * @return the mapped Section model
     */
    public Section from(
            CourseSection courseSection,
            Map<Long, Course> courseMap,
            Map<Long, Teacher> teacherMap,
            Map<Long, Classroom> classroomMap
    ) {
        Section section = new Section();
        section.setId(courseSection.getId());
        section.setSectionNumber(courseSection.getSectionNumber());
        section.setCapacity(courseSection.getCapacity());
        section.setHoursPerWeek(courseSection.getHoursPerWeek());
        section.setStatus(courseSection.getStatus());
        section.setCourse(courseMap.get(courseSection.getCourseId()));
        section.setTeacher(teacherMap.get(courseSection.getTeacherId()));
        section.setClassroom(classroomMap.get(courseSection.getClassroomId()));
        section.setTimeslots(courseSection.getTimeslots());
        return section;
    }

    /**
     * Maps/Converts a Section model to a SectionDTO.
     *
     * @param section the Section model
     * @return the mapped SectionDTO
     */
    public SectionDTO from(Section section) {
        SectionDTO sectionDto = new SectionDTO();
        sectionDto.setId(section.getId());
        sectionDto.setSectionNumber(section.getSectionNumber());
        sectionDto.setCapacity(section.getCapacity());
        sectionDto.setStatus(section.getStatus().name());
        sectionDto.setHoursPerWeek(section.getHoursPerWeek());

        if (section.getCourse() != null) {
            sectionDto.setCourseId(section.getCourse().getId());
            sectionDto.setCourseCode(section.getCourse().getCode());
            sectionDto.setCourseName(section.getCourse().getName());
        }
        if (section.getTeacher() != null) {
            sectionDto.setTeacherName(section.getTeacher().getFullName());
        }
        if (section.getClassroom() != null) {
            sectionDto.setClassroomName(section.getClassroom().getName());
        }

        sectionDto.setTimeslots(section
                .getTimeslots()
                .stream()
                .map(ts -> new TimeslotDTO(
                        ts.getDayOfWeek(),
                        ts.getDayOfWeekName(),
                        ts.getStartTime().toString(),
                        ts.getEndTime().toString()))
                .sorted(Comparator
                        .comparingInt(TimeslotDTO::getDayOfWeek)
                        .thenComparing(TimeslotDTO::getStartTime))
                .toList());

        return sectionDto;
    }
}
