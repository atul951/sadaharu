package com.trinity.scheduler.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import com.trinity.scheduler.entity.enums.EnrollmentStatus;

/**
 * Entity representing a student's enrollment in a course section.
 * Tracks the relationship between students and the sections they're registered for.
 * Manages enrollment lifecycle from registration through completion or withdrawal.
 * Business rules enforced by service layer:
 * - Student must meet prerequisites before enrollment
 * - No time conflicts with other enrolled sections
 * - Maximum 5 courses per semester per student
 * - Section must have available capacity
 *
 * @author Atul Kumar
 */
@Entity
@Table(name = "student_enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "section_id"}))
public class StudentEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to students table.
     * Identifies which student this enrollment is for.
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * The section this enrollment is for.
     * Many-to-one relationship with CourseSection.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private CourseSection section;

    /**
     * Current enrollment status.
     * - ENROLLED: Active registration
     * - WAITLISTED: On waiting list (section full)
     * - DROPPED: Student withdrew
     * - COMPLETED: Course finished, grade recorded
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    /**
     * Timestamp when student enrolled.
     */
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    /**
     * Timestamp when student dropped the course.
     * Null if not dropped.
     */
    @Column(name = "dropped_at")
    private LocalDateTime droppedAt;

    /**
     * Default constructor required by JPA.
     */
    public StudentEnrollment() {
    }

    /**
     * Constructor for creating a new enrollment.
     *
     * @param studentId the student ID
     * @param section   the section to enroll in
     */
    public StudentEnrollment(Long studentId, CourseSection section) {
        this.studentId = studentId;
        this.section = section;
    }

    /**
     * Marks this enrollment as dropped.
     * Sets status to DROPPED and records the drop timestamp.
     */
    public void drop() {
        this.status = EnrollmentStatus.DROPPED;
        this.droppedAt = LocalDateTime.now();
    }

    /**
     * Marks this enrollment as completed.
     * Called when semester ends and grades are recorded.
     */
    public void complete() {
        this.status = EnrollmentStatus.COMPLETED;
    }

    /**
     * Checks if this enrollment is active (enrolled or waitlisted).
     *
     * @return true if enrolled or waitlisted
     */
    @Transient
    public boolean isActive() {
        return status == EnrollmentStatus.ENROLLED || status == EnrollmentStatus.WAITLISTED;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public CourseSection getSection() {
        return section;
    }

    public void setSection(CourseSection section) {
        this.section = section;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public LocalDateTime getDroppedAt() {
        return droppedAt;
    }

    public void setDroppedAt(LocalDateTime droppedAt) {
        this.droppedAt = droppedAt;
    }
}
