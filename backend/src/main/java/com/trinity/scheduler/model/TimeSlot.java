package com.trinity.scheduler.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Represents a single timeslot.
 */
public class TimeSlot {
    public final DayOfWeek day;
    public final LocalTime startTime;
    public final LocalTime endTime;

    /**
     * Creates a timeslot.
     *
     * @param day       day of week
     * @param startTime start time
     * @param endTime   end time
     */
    public TimeSlot(DayOfWeek day, LocalTime startTime, LocalTime endTime) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Gets duration in hours.
     *
     * @return duration in hours
     */
    public int getDurationHours() {
        return (endTime.toSecondOfDay() - startTime.toSecondOfDay()) / 3600;
    }

    @Override
    public String toString() {
        return String.format("%s %s-%s", day, startTime, endTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot timeSlot)) return false;
        return day == timeSlot.day &&
                startTime.equals(timeSlot.startTime) &&
                endTime.equals(timeSlot.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, startTime, endTime);
    }

    public TimeSlot copyWithDay(DayOfWeek day) {
        return new TimeSlot(day, this.startTime, this.endTime);
    }
}
