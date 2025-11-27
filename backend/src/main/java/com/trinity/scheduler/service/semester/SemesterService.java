package com.trinity.scheduler.service.semester;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.trinity.scheduler.entity.Semester;
import com.trinity.scheduler.repository.CourseSectionRepository;
import com.trinity.scheduler.repository.SectionTimeslotRepository;
import com.trinity.scheduler.repository.SemesterRepository;

/**
 * Service class for Semester.
 */
@Service
public class SemesterService {
    private static final Logger log = LoggerFactory.getLogger(SemesterService.class);
    private final CourseSectionRepository courseSectionRepository;
    private final SemesterRepository semesterRepository;
    private final SectionTimeslotRepository sectionTimeslotRepository;

    public SemesterService(SemesterRepository semesterRepository,
                           CourseSectionRepository courseSectionRepository,
                           SectionTimeslotRepository sectionTimeslotRepository
    ) {
        this.courseSectionRepository = courseSectionRepository;
        this.semesterRepository = semesterRepository;
        this.sectionTimeslotRepository = sectionTimeslotRepository;
    }

    /**
     * Retrieves a Semester by its ID.
     *
     * @param semesterId the ID of the semester
     * @return the Semester entity
     * @throws IllegalArgumentException if the semester is not found
     */
    public Semester getSemesterById(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found with ID: " + semesterId));
    }

    /**
     * Reverts all schedule data by deleting all course sections and section timeslots.
     */
    @Transactional
    public void revertAllScheduleData() {
        log.info("Reverting all schedule data by deleting all course sections and section timeslots.");
        courseSectionRepository.deleteAll();
        sectionTimeslotRepository.deleteAll();
    }

    /**
     * Reverts schedule data for a specific semester by deleting course sections associated with that semester.
     *
     * @param semesterId the ID of the semester
     */
    @Transactional
    public void revertScheduleData(final Long semesterId) {
        log.info("Reverting schedule data for semester {} by deleting associated course sections.", semesterId);
        courseSectionRepository.deleteBySemesterId(semesterId);
    }
}
