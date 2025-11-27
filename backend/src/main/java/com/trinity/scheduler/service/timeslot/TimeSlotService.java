package com.trinity.scheduler.service.timeslot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.trinity.scheduler.entity.Classroom;
import com.trinity.scheduler.entity.CourseSection;
import com.trinity.scheduler.entity.SectionTimeslot;
import com.trinity.scheduler.entity.Teacher;
import com.trinity.scheduler.entity.enums.SectionStatus;
import com.trinity.scheduler.model.TimeSlot;
import com.trinity.scheduler.repository.SectionTimeslotRepository;

import java.util.List;

/**
 * Service class for Time Slots.
 */
@Service
public class TimeSlotService {
    private static final Logger log = LoggerFactory.getLogger(TimeSlotService.class);
    private static final int MAX_CLASS_SESSION_HOURS = 2;
    private static final int MAX_TEACHER_DAILY_HOURS = 4;
    private final SectionTimeslotRepository timeslotRepository;

    public TimeSlotService(SectionTimeslotRepository timeslotRepository) {
        this.timeslotRepository = timeslotRepository;
    }

    /**
     * Checks if a section can be assigned to teacher, room, and timeslots.
     *
     * @param section  the section
     * @param teacher  the teacher
     * @param room     the room
     * @param timeslot the timeslot
     * @return true if assignment is valid
     */
    public boolean canAssign(CourseSection section, Teacher teacher, Classroom room, TimeSlot timeslot) {
        // Check the timeslot for conflicts
        // Check teacher or room conflict
        boolean teacherRoomConflict = timeslotRepository.hasTeacherOrRoomConflict(
                teacher.getId(), room.getId(), timeslot.day.getValue(), timeslot.startTime, timeslot.endTime);
        if (teacherRoomConflict) {
            log.debug("Conflict detected for section {} teacher {} or room {} on day {} from {} to {}",
                    section.getId(), teacher.getFullName(), room.getName(), timeslot.day, timeslot.startTime, timeslot.endTime);
            return false;
        }

        // Check teacher daily limit
        Double dailyHours = timeslotRepository.calculateTeacherDailyHours(teacher.getId(), timeslot.day.getValue());
        if (dailyHours != null && dailyHours + timeslot.getDurationHours() > MAX_TEACHER_DAILY_HOURS) {
            log.debug("Teacher {} exceeds daily limit on day {} with existing hours {}",
                    teacher.getFullName(), timeslot.day, dailyHours);
            return false;
        }

        return true;
    }

    /**
     * Assigns a section to teacher, room, and timeslots.
     *
     * @param section   the section
     * @param teacher   the teacher
     * @param room      the room
     * @param timeslots the timeslots
     */
    public void assign(CourseSection section, Teacher teacher, Classroom room, List<TimeSlot> timeslots) {
        section.setTeacherId(teacher.getId());
        section.setClassroomId(room.getId());
        section.setStatus(SectionStatus.SCHEDULED);

        // Create timeslot entities
        for (TimeSlot slot : timeslots) {
            SectionTimeslot timeslot = new SectionTimeslot(section, slot.day, slot.startTime, slot.endTime);
            timeslot = timeslotRepository.save(timeslot);
            section.addTimeslot(timeslot);
        }
        log.info("Assigned section {} to teacher {} in room {}",
                section.getSectionNumber(), teacher.getFullName(), room.getName());
    }
}
