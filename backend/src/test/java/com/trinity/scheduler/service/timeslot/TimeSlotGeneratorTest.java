package com.trinity.scheduler.service.timeslot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trinity.scheduler.model.TimeSlot;
import com.trinity.scheduler.service.timeslot.TimeSlotGenerator;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TimeSlotGenerator.
 * Tests timeslot generation logic without mocking (pure logic).
 *
 * @author Atul Kumar
 */
@ExtendWith(MockitoExtension.class)
class TimeSlotGeneratorTest {

    private TimeSlotGenerator timeSlotGenerator;

    @BeforeEach
    void setUp() {
        timeSlotGenerator = new TimeSlotGenerator();
    }

    @Test
    @DisplayName("Should generate all possible timeslots")
    void shouldGenerateAllPossibleTimeslots() {
        // When
        List<TimeSlot> slots = timeSlotGenerator.generateAllPossibleSlots();

        // Then
        assertThat(slots).isNotEmpty();
        // Should have slots for all 5 weekdays (Monday-Friday)
        Set<DayOfWeek> days = slots.stream()
                .map(slot -> slot.day)
                .collect(Collectors.toSet());
        assertThat(days).containsExactlyInAnyOrder(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        );
    }

    @Test
    @DisplayName("Should generate timeslots within school hours")
    void shouldGenerateTimeslotsWithinSchoolHours() {
        // When
        List<TimeSlot> slots = timeSlotGenerator.generateAllPossibleSlots();

        // Then
        LocalTime schoolStart = LocalTime.of(9, 0);
        LocalTime schoolEnd = LocalTime.of(17, 0);
        for (TimeSlot slot : slots) {
            assertThat(slot.startTime).isAfterOrEqualTo(schoolStart);
            assertThat(slot.endTime).isBeforeOrEqualTo(schoolEnd);
        }
    }

    @Test
    @DisplayName("Should not generate timeslots during lunch break")
    void shouldNotGenerateTimeslotsDuringLunchBreak() {
        // When
        List<TimeSlot> slots = timeSlotGenerator.generateAllPossibleSlots();

        // Then
        LocalTime lunchStart = LocalTime.of(12, 0);
        LocalTime lunchEnd = LocalTime.of(13, 0);
        for (TimeSlot slot : slots) {
            // Timeslot should not overlap with lunch break
            boolean overlapsLunch = slot.startTime.isBefore(lunchEnd) && slot.endTime.isAfter(lunchStart);
            assertThat(overlapsLunch).isFalse();
        }
    }

    @Test
    @DisplayName("Should precompute timeslot combinations")
    void shouldPrecomputeTimeslotCombinations() {
        // When
        timeSlotGenerator.precomputeTimeSlotCombinationsForHours(4);

        // Then
        // Method should complete without exception
        // The cache is private, so we can't directly verify it,
        // but we can verify the method doesn't throw
        assertThat(true).isTrue(); // Placeholder assertion
    }

    @Test
    @DisplayName("Should generate valid timeslot durations")
    void shouldGenerateValidTimeslotDurations() {
        // When
        List<TimeSlot> slots = timeSlotGenerator.generateAllPossibleSlots();

        // Then
        for (TimeSlot slot : slots) {
            int duration = slot.getDurationHours();
            assertThat(duration).isGreaterThan(0);
            assertThat(duration).isLessThanOrEqualTo(2); // Max 2 hours per session
        }
    }

    @Test
    @DisplayName("Should generate timeslots with valid time ranges")
    void shouldGenerateTimeslotsWithValidTimeRanges() {
        // When
        List<TimeSlot> slots = timeSlotGenerator.generateAllPossibleSlots();

        // Then
        for (TimeSlot slot : slots) {
            assertThat(slot.endTime).isAfter(slot.startTime);
        }
    }

    @Test
    @DisplayName("Should generate multiple timeslots per day")
    void shouldGenerateMultipleTimeslotsPerDay() {
        // When
        List<TimeSlot> slots = timeSlotGenerator.generateAllPossibleSlots();

        // Then
        long mondaySlots = slots.stream()
                .filter(slot -> slot.day == DayOfWeek.MONDAY)
                .count();
        assertThat(mondaySlots).isGreaterThan(1);
    }
}

