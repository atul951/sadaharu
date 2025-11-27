package com.trinity.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.trinity.scheduler.entity.enums.EnrollmentStatus;
import com.trinity.scheduler.entity.enums.SectionStatus;

/**
 * Entity representing a specific instance (section) of a course offered in a semester.
 * A course section is created when a course is scheduled for a particular semester.
 * Multiple sections of the same course can exist to accommodate student demand.
 * Each section is assigned a teacher, classroom, and specific time slots.
 * Example: MAT101 (Algebra I) in Fall 2024 might have 3 sections:
 * - Section 1: Teacher John Smith, Room 205, MWF 9-10am
 * - Section 2: Teacher Mary Johnson, Room 206, TTh 10-12pm
 * - Section 3: Teacher John Smith, Room 205, MWF 2-3pm
 *
 * @author Atul Kumar
 */
@Getter
@Setter
@Entity
@Table(name = "course_sections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "semester_id", "section_number"}))
public class CourseSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to courses table.
     * Identifies which course this section is for (e.g., MAT101, ENG201).
     */
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /**
     * Foreign key to semesters table.
     * Identifies when this section is offered (e.g., Fall 2024, Spring 2025).
     */
    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    /**
     * Section number to distinguish multiple sections of the same course.
     * Starts at 1 and increments (1, 2, 3, ...).
     */
    @Column(name = "section_number", nullable = false)
    private Integer sectionNumber;

    /**
     * Foreign key to teachers table.
     * Nullable during initial creation, assigned by scheduling algorithm.
     */
    @Column(name = "teacher_id")
    private Long teacherId = 0L;

    /**
     * Foreign key to classrooms table.
     * Nullable during initial creation, assigned by scheduling algorithm.
     */
    @Column(name = "classroom_id")
    private Long classroomId = 0L;

    /**
     * Maximum number of students that can enroll in this section.
     * Default is 10 per college policy (matches classroom capacity).
     */
    @Column(nullable = false)
    private Integer capacity = 10;

    /**
     * Total weekly hours for this course (copied from course definition).
     * Used by scheduling algorithm to allocate appropriate time slots.
     * Example: 4 hours/week might be split as MWF 1h + T 1h, or TTh 2h each.
     */
    @Column(name = "hours_per_week", nullable = false)
    private Integer hoursPerWeek;

    /**
     * Current lifecycle status of the section.
     * - unscheduled: Created but not yet assigned teacher/room/times
     * - scheduled: Fully assigned and ready for enrollment
     * - active: Semester in progress, students enrolled
     * - completed: Semester finished
     * - cancelled: Section was cancelled (low enrollment, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SectionStatus status = SectionStatus.UNSCHEDULED;

    /**
     * Timestamp when this section was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Timestamp of last update to this section.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Time slots when this section meets.
     * One-to-many relationship with SectionTimeslot.
     * Cascade ALL ensures timeslots are created/deleted with section.
     */
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SectionTimeslot> timeslots = new ArrayList<>();

    /**
     * Student enrollments in this section.
     * One-to-many relationship with StudentEnrollment.
     */
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StudentEnrollment> enrollments = new ArrayList<>();

    /**
     * Default constructor required by JPA.
     */
    public CourseSection() {
    }

    /**
     * Constructor for creating a new section.
     *
     * @param courseId      the course this section is for
     * @param semesterId    the semester this section is offered in
     * @param sectionNumber the section number (1, 2, 3, ...)
     * @param hoursPerWeek  total weekly hours from course definition
     */
    public CourseSection(Long courseId, Long semesterId, Integer sectionNumber, Integer hoursPerWeek) {
        this.courseId = courseId;
        this.semesterId = semesterId;
        this.sectionNumber = sectionNumber;
        this.hoursPerWeek = hoursPerWeek;
    }

    /**
     * Adds a timeslot to this section.
     * Maintains bidirectional relationship.
     *
     * @param timeslot the timeslot to add
     */
    public void addTimeslot(SectionTimeslot timeslot) {
        timeslots.add(timeslot);
        timeslot.setSection(this);
    }

    /**
     * Removes a timeslot from this section.
     * Maintains bidirectional relationship.
     *
     * @param timeslot the timeslot to remove
     */
    public void removeTimeslot(SectionTimeslot timeslot) {
        timeslots.remove(timeslot);
        timeslot.setSection(null);
    }

    /**
     * Adds an enrollment to this section.
     * Maintains bidirectional relationship.
     *
     * @param enrollment the enrollment to add
     */
    public void addEnrollment(StudentEnrollment enrollment) {
        enrollments.add(enrollment);
        enrollment.setSection(this);
    }

    /**
     * Gets the current number of enrolled students.
     * Only counts students with status ENROLLED (not waitlisted or dropped).
     *
     * @return count of enrolled students
     */
    public int getEnrolledCount() {
        return (int) enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
    }

    /**
     * Checks if this section is full.
     *
     * @return true if enrolled count equals or exceeds capacity
     */
    public boolean isFull() {
        return getEnrolledCount() >= capacity;
    }

    /**
     * Gets the number of available spots.
     *
     * @return capacity minus enrolled count
     */
    public int getAvailableSpots() {
        return Math.max(0, capacity - getEnrolledCount());
    }

    /**
     * Lifecycle callback to update the updatedAt timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
