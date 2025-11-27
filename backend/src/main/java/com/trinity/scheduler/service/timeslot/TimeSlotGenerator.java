package com.trinity.scheduler.service.timeslot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.trinity.scheduler.model.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

/**
 * Generates valid timeslot combinations for courses.
 *
 * <p>Rules:
 * <ul>
 *   <li>College hours: 9 AM - 5 PM</li>
 *   <li>Lunch break: 12 PM - 1 PM (no classes)</li>
 *   <li>Session length: 1 or 2 hours</li>
 *   <li>Must sum to required hours_per_week</li>
 * </ul>
 *
 * <p>Example: For 4 hours/week course, generates combinations like:
 * <ul>
 *   <li>Mon 9-10, Wed 10-11, Fri 2-4</li>
 *   <li>Tue 9-11, Thu 1-3</li>
 * </ul>
 *
 * @author Atul Kumar
 */
@Component
public class TimeSlotGenerator {
    private static final Logger log = LoggerFactory.getLogger(TimeSlotGenerator.class);

    private final Map<Integer, List<List<TimeSlot>>> timeslotCombinationsCache = new HashMap<>();

    /**
     * Generates all possible individual timeslots.
     *
     * <p>Creates 1-hour and 2-hour slots for each weekday, avoiding lunch break.
     *
     * @return list of all possible timeslots
     */
    public List<TimeSlot> generateAllPossibleSlots() {
        List<TimeSlot> slots = new ArrayList<>();
        List<TimeSlot> allPossibleSlots = List.of(
                new TimeSlot(DayOfWeek.SUNDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new TimeSlot(DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                new TimeSlot(DayOfWeek.SUNDAY, LocalTime.of(13, 0), LocalTime.of(14, 0)),
                new TimeSlot(DayOfWeek.SUNDAY, LocalTime.of(14, 0), LocalTime.of(15, 0)),
                new TimeSlot(DayOfWeek.SUNDAY, LocalTime.of(11, 0), LocalTime.of(12, 0)),
                new TimeSlot(DayOfWeek.SUNDAY, LocalTime.of(15, 0), LocalTime.of(16, 0)),
                new TimeSlot(DayOfWeek.SUNDAY, LocalTime.of(16, 0), LocalTime.of(17, 0))
        );
        DayOfWeek[] days = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        };

        for (TimeSlot slot : allPossibleSlots) {
            for (DayOfWeek day : days) {
                slots.add(slot.copyWithDay(day));
            }
        }

        return slots;
    }

    /**
     * Precomputes timeslot combinations for all courses.
     *
     * @param maxHoursPerWeek maximum hours per week among all courses
     */
    public void precomputeTimeSlotCombinationsForHours(Integer maxHoursPerWeek) {
        for (int hours = 1; hours <= maxHoursPerWeek; hours++) {
            timeslotCombinationsCache.putIfAbsent(hours, generateCombinations(hours));
        }
        log.info("Generated {} timeslot combinations for total {} hours/week",
                timeslotCombinationsCache.size(), maxHoursPerWeek);
    }

    /**
     * Generates all valid timeslot combinations for given hours per week.
     *
     * <p>Returns combinations sorted by preference (spread across more days is better).
     *
     * @param hoursPerWeek total hours needed
     * @return list of possible timeslot combinations
     */
    private List<List<TimeSlot>> generateCombinations(int hoursPerWeek) {
        List<TimeSlot> allSlots = generateAllPossibleSlots();
        List<List<TimeSlot>> combinations = new ArrayList<>();

        // Find combinations that sum to hoursPerWeek
        findCombinations(allSlots, hoursPerWeek, 0, new ArrayList<>(), combinations);

        // Sort by preference (spread across days)
        combinations.sort(Comparator.comparingInt(this::calculateSpreadScore).reversed());

        return combinations;
    }

    /**
     * Recursively finds combinations that sum to target hours.
     *
     * <p>Uses backtracking to explore all valid combinations.
     *
     * @param slots       all available slots
     * @param targetHours target total hours
     * @param startIdx    current index in slots list
     * @param current     current combination being built
     * @param result      accumulator for valid combinations
     */
    private void findCombinations(List<TimeSlot> slots, int targetHours, int startIdx,
                                  List<TimeSlot> current, List<List<TimeSlot>> result) {
        int currentHours = current.stream().mapToInt(TimeSlot::getDurationHours).sum();

        if (currentHours == targetHours) {
            result.add(new ArrayList<>(current));
            return;
        }

        if (currentHours > targetHours || startIdx >= slots.size()) {
            return;
        }

        // Limit combinations to prevent explosion
        if (result.size() >= 100) {
            return;
        }

        for (int i = startIdx; i < slots.size(); i++) {
            TimeSlot slot = slots.get(i);
            if (!hasConflict(current, slot)) {
                current.add(slot);
                findCombinations(slots, targetHours, i + 1, current, result);
                current.remove(current.size() - 1);
            }
        }
    }

    /**
     * Checks if a slot conflicts with existing slots.
     *
     * <p>Two slots conflict if they're on the same day and overlap in time.
     *
     * @param existing existing slots
     * @param newSlot  new slot to check
     * @return true if conflict exists
     */
    private boolean hasConflict(List<TimeSlot> existing, TimeSlot newSlot) {
        for (TimeSlot slot : existing) {
            if (slot.day == newSlot.day) {
                if (slot.startTime.isBefore(newSlot.endTime) &&
                        slot.endTime.isAfter(newSlot.startTime)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Calculates spread score (higher = more days).
     *
     * <p>Spreading across more days is preferred for student convenience.
     *
     * @param slots list of timeslots
     * @return number of unique days
     */
    private int calculateSpreadScore(List<TimeSlot> slots) {
        Set<DayOfWeek> uniqueDays = new HashSet<>();
        for (TimeSlot slot : slots) {
            uniqueDays.add(slot.day);
        }
        return uniqueDays.size();
    }
}
