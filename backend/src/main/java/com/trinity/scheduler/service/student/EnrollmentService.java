package com.trinity.scheduler.service.student;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trinity.scheduler.dto.EnrollmentValidationResult;
import com.trinity.scheduler.entity.CourseSection;
import com.trinity.scheduler.entity.SectionTimeslot;
import com.trinity.scheduler.entity.Student;
import com.trinity.scheduler.entity.StudentEnrollment;
import com.trinity.scheduler.entity.enums.EnrollmentStatus;
import com.trinity.scheduler.entity.enums.SectionStatus;
import com.trinity.scheduler.exception.EnrollmentException;
import com.trinity.scheduler.repository.CourseSectionRepository;
import com.trinity.scheduler.repository.SectionTimeslotRepository;
import com.trinity.scheduler.repository.StudentEnrollmentRepository;
import com.trinity.scheduler.repository.StudentRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Service for managing student enrollments in course sections.
 *
 * <p>Handles enrollment validation and processing including:
 * <ul>
 *   <li>Prerequisite validation</li>
 *   <li>Time conflict detection</li>
 *   <li>Capacity checking</li>
 *   <li>Course load limits (max 5 courses per semester)</li>
 * </ul>
 *
 * @author Atul Kumar
 */
@Service
public class EnrollmentService {
    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
    private static final int MAX_COURSES_PER_SEMESTER = 5;

    private final StudentEnrollmentRepository enrollmentRepository;
    private final CourseSectionRepository sectionRepository;
    private final PrerequisiteValidator prerequisiteValidator;
    private final SectionTimeslotRepository timeslotRepository;
    private final StudentRepository studentRepository;

    /**
     * Constructor with dependency injection.
     */
    public EnrollmentService(StudentEnrollmentRepository enrollmentRepository,
                             CourseSectionRepository sectionRepository,
                             PrerequisiteValidator prerequisiteValidator,
                             StudentRepository studentRepository,
                             SectionTimeslotRepository timeslotRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.timeslotRepository = timeslotRepository;
        this.prerequisiteValidator = prerequisiteValidator;
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Enrolls a student in a course section.
     *
     * <p>Validation steps:
     * <ol>
     *   <li>Check if section exists and is available</li>
     *   <li>Check if student already enrolled in this section</li>
     *   <li>Check if student already enrolled in same course (different section)</li>
     *   <li>Check prerequisites</li>
     *   <li>Check time conflicts</li>
     *   <li>Check course load limit (5 courses max)</li>
     *   <li>Check section capacity</li>
     * </ol>
     *
     * @param studentId the student ID
     * @param sectionId the section ID
     * @return the created enrollment
     * @throws EnrollmentException if validation fails
     */
    @Transactional
    public StudentEnrollment enrollStudentInCourse(Long studentId, Long sectionId) {
        log.info("Processing enrollment for student {} in section {}", studentId, sectionId);

        // 1. Check if section exists
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EnrollmentException("Section not found: " + sectionId));

        if (section.getStatus() != SectionStatus.SCHEDULED &&
                section.getStatus() != SectionStatus.ACTIVE) {
            throw new EnrollmentException("Section is not available for enrollment");
        }

        // 2. Check if already enrolled in this section
        if (enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId).isPresent()) {
            throw new EnrollmentException("Student already enrolled in this section");
        }

        // 3. Check if already enrolled in same course (different section)
        if (enrollmentRepository.isEnrolledInCourse(studentId, section.getCourseId(), section.getSemesterId())) {
            throw new EnrollmentException("Student already enrolled in this course");
        }

        // 4. Check prerequisites
        if (!prerequisiteValidator.hasMetPrerequisites(studentId, section.getCourseId())) {
            List<Long> missing = prerequisiteValidator.getMissingPrerequisites(
                    studentId, section.getCourseId());
            throw new EnrollmentException("Prerequisites not met. Missing courses: " + missing);
        }

        // 5. Check time conflicts
        if (hasTimeConflict(studentId, sectionId)) {
            throw new EnrollmentException("Time conflict with existing enrollment");
        }

        // 6. Check course load limit
        long currentCourseCount = enrollmentRepository.countActiveEnrollments(
                studentId, section.getSemesterId());
        if (currentCourseCount >= MAX_COURSES_PER_SEMESTER) {
            throw new EnrollmentException(
                    "Maximum course load reached (" + MAX_COURSES_PER_SEMESTER + " courses)");
        }

        // 7. Check section capacity
        if (section.isFull()) {
            // Add to waitlist
            log.info("Section {} is full, adding student {} to waitlist", sectionId, studentId);
            StudentEnrollment enrollment = new StudentEnrollment(studentId, section);
            enrollment.setStatus(EnrollmentStatus.WAITLISTED);
            return enrollmentRepository.save(enrollment);
        }

        // All validations passed - enroll student
        StudentEnrollment enrollment = new StudentEnrollment(studentId, section);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment = enrollmentRepository.save(enrollment);

        log.info("Successfully enrolled student {} in section {}", studentId, sectionId);
        return enrollment;
    }

    /**
     * Drops a student from a section.
     *
     * @param studentId the student ID
     * @param sectionId the section ID
     * @throws EnrollmentException if enrollment not found
     */
    @Transactional
    public void drop(Long studentId, Long sectionId) {
        StudentEnrollment enrollment = enrollmentRepository
                .findByStudentIdAndSectionId(studentId, sectionId)
                .orElseThrow(() -> new EnrollmentException("Enrollment not found"));

        enrollment.drop();
        enrollmentRepository.save(enrollment);

        log.info("Student {} dropped from section {}", studentId, sectionId);
    }

    /**
     * Gets all enrollments for a student in a semester.
     *
     * @param studentId  the student ID
     * @param semesterId the semester ID
     * @return list of enrollments
     */
    public List<StudentEnrollment> getStudentEnrollments(Long studentId, Long semesterId) {
        return enrollmentRepository.findActiveEnrollmentsBySemester(studentId, semesterId);
    }

    /**
     * Validates if a student can enroll in a section without actually enrolling.
     *
     * @param studentId the student ID
     * @param sectionId the section ID
     * @return validation result with details
     */
    public EnrollmentValidationResult validateEnrollment(Long studentId, Long sectionId) {
        EnrollmentValidationResult result = new EnrollmentValidationResult();
        result.setValid(true);

        try {
            // Run all validation checks
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                result.setValid(false);
                result.addError("Student not found");
                return result;
            }

            CourseSection section = sectionRepository.findById(sectionId).orElse(null);
            if (section == null) {
                result.setValid(false);
                result.addError("Section not found");
                return result;
            }

            if (enrollmentRepository.findByStudentIdAndSectionId(studentId, sectionId).isPresent()) {
                result.setValid(false);
                result.addError("Already enrolled in this section");
            }

            if (enrollmentRepository.isEnrolledInCourse(studentId, section.getCourseId(), section.getSemesterId())) {
                result.setValid(false);
                result.addError("Already enrolled in this course");
            }

            if (!prerequisiteValidator.hasMetPrerequisites(studentId, section.getCourseId())) {
                result.setValid(false);
                result.addError("Prerequisites not met");
            }

            if (hasTimeConflict(studentId, sectionId)) {
                result.setValid(false);
                result.addError("Time conflict with existing enrollment");
            }

            long currentCourseCount = enrollmentRepository.countActiveEnrollments(
                    studentId, section.getSemesterId());
            if (currentCourseCount >= MAX_COURSES_PER_SEMESTER) {
                result.setValid(false);
                result.addError("Maximum course load reached");
            }

            if (section.isFull()) {
                result.addWarning("Section is full - will be waitlisted");
            }

        } catch (Exception e) {
            result.setValid(false);
            result.addError("Validation error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Checks if enrolling in a section would create a time conflict.
     *
     * <p>A conflict exists if the student is already enrolled in another
     * section that meets at an overlapping time.
     *
     * @param studentId the student ID
     * @param sectionId the section ID to check
     * @return true if a conflict exists
     */
    private boolean hasTimeConflict(Long studentId, Long sectionId) {
        // Get timeslots for the proposed section
        List<SectionTimeslot> proposedTimeslots = timeslotRepository.findBySectionId(sectionId);

        if (proposedTimeslots.isEmpty()) {
            log.warn("No timeslots found for section {}", sectionId);
            return false;
        }

        // Check each timeslot for conflicts
        for (SectionTimeslot proposedSlot : proposedTimeslots) {
            Integer hasConflict = enrollmentRepository.hasTimeConflict(
                    studentId,
                    proposedSlot.getDayOfWeek(),
                    proposedSlot.getStartTime().toString(),
                    proposedSlot.getEndTime().toString()
            );

            if (hasConflict > 0) {
                log.debug("Time conflict detected for student {} on {} at {}-{}",
                        studentId, proposedSlot.getDayOfWeekName(),
                        proposedSlot.getStartTime(), proposedSlot.getEndTime());
                return true;
            }
        }

        return false;
    }
}
