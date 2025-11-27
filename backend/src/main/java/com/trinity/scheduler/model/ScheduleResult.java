package com.trinity.scheduler.model;
import java.util.List;

import com.trinity.scheduler.entity.CourseSection;

/**
 * Result of schedule generation.
 */
public record ScheduleResult(List<CourseSection> sections, int scheduled, int failed) {
}
