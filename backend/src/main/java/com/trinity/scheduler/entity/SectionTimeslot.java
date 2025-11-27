package com.trinity.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Entity representing a specific time slot when a course section meets.
 * A section can have multiple timeslots to fulfill its hours_per_week requirement.
 * For example, a 4-hour/week course might meet:
 * - Monday 9-10am (1 hour)
 * - Wednesday 10-11am (1 hour)
 * - Friday 2-4pm (2 hours)
 * Constraints enforced:
 * - Day must be Monday-Friday (1-5)
 * - Times must be within college hours (9am-5pm)
 * - No overlap with lunch break (12pm-1pm)
 * - End time must be after start time
 *
 * @author Atul Kumar
 */
@Getter
@Setter
@Entity
@Table(name = "section_timeslots")
public class SectionTimeslot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The section this timeslot belongs to.
     * Many-to-one relationship with CourseSection.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private CourseSection section;

    /**
     * Day of week when this timeslot occurs.
     * 1 = Monday, 2 = Tuesday, ..., 5 = Friday
     * <p>
     * Stored as integer in database for compatibility with SQLite.
     */
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    /**
     * Start time of this session.
     * Must be >= 09:00:00 and < end_time.
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * End time of this session.
     * Must be <= 17:00:00 and > start_time.
     * Cannot cross lunch break (12:00-13:00).
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Timestamp when this timeslot was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Default constructor required by JPA.
     */
    public SectionTimeslot() {
    }

    /**
     * Constructor for creating a new timeslot.
     *
     * @param dayOfWeek     the day of week (MONDAY through FRIDAY)
     * @param startTime     the start time
     * @param endTime       the end time
     * @param courseSection the associated course section
     */
    public SectionTimeslot(CourseSection courseSection, DayOfWeek dayOfWeek,
                           LocalTime startTime, LocalTime endTime) {
        this.section = courseSection;
        this.dayOfWeek = dayOfWeek.getValue();
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Gets the day of week as a DayOfWeek enum.
     *
     * @return the day of week
     */
    @Transient
    public String getDayOfWeekName() {
        return DayOfWeek.of(dayOfWeek).name();
    }

    /**
     * Calculates the duration of this timeslot in hours.
     *
     * @return duration in hours (e.g., 1.0, 1.5, 2.0)
     */
    @Transient
    public double getDurationHours() {
        return (endTime.toSecondOfDay() - startTime.toSecondOfDay()) / 3600.0;
    }

    /**
     * Checks if this timeslot overlaps with another timeslot.
     * Only checks if they're on the same day.
     *
     * @param other the other timeslot
     * @return true if they overlap
     */
    public boolean overlapsWith(SectionTimeslot other) {
        if (!this.dayOfWeek.equals(other.dayOfWeek)) {
            return false;
        }
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }

    /**
     * Checks if this timeslot is during the lunch break (12:00-13:00).
     *
     * @return true if any part of this timeslot overlaps with lunch
     */
    @Transient
    public boolean isDuringLunch() {
        LocalTime lunchStart = LocalTime.of(12, 0);
        LocalTime lunchEnd = LocalTime.of(13, 0);
        return this.startTime.isBefore(lunchEnd) && this.endTime.isAfter(lunchStart);
    }

    /**
     * Checks if this timeslot is within college hours (9am-5pm).
     *
     * @return true if within college hours
     */
    @Transient
    public boolean isWithinCollegeHours() {
        LocalTime collegeStart = LocalTime.of(9, 0);
        LocalTime collegeEnd = LocalTime.of(17, 0);
        return !this.startTime.isBefore(collegeStart) && !this.endTime.isAfter(collegeEnd);
    }

    /**
     * Validates this timeslot against all constraints.
     *
     * @throws IllegalStateException if any constraint is violated
     */
    public void validate() {
        if (dayOfWeek < 1 || dayOfWeek > 5) {
            throw new IllegalStateException("Day of week must be 1-5 (Monday-Friday)");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalStateException("End time must be after start time");
        }
        if (!isWithinCollegeHours()) {
            throw new IllegalStateException("Timeslot must be within college hours (9am-5pm)");
        }
        if (isDuringLunch()) {
            throw new IllegalStateException("Timeslot cannot overlap with lunch break (12pm-1pm)");
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s %s-%s", section.getSectionNumber(), dayOfWeek, startTime, endTime);
    }
}
