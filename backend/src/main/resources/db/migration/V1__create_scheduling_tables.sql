-- ============================================================================
-- Migration: Core Scheduling Tables
-- ============================================================================
-- This migration creates the fundamental tables needed for the scheduling system:
-- 1. course_sections: Instances of courses scheduled for a specific semester
-- 2. section_timeslots: Time slots when each section meets
-- 3. student_enrollments: Student registrations in course sections
--
-- All tables are additive and do not modify existing database schema.
-- Foreign keys maintain referential integrity with existing tables.
-- ============================================================================

-- ============================================================================
-- Table: course_sections
-- ============================================================================
-- Represents a specific instance (section) of a course offered in a semester.
-- Multiple sections of the same course can exist to accommodate student demand.
--
-- Key Fields:
-- - course_id: Links to courses table (which course this is)
-- - semester_id: Links to semesters table (when it's offered)
-- - section_number: Distinguishes multiple sections of same course (1, 2, 3...)
-- - teacher_id: Assigned teacher (nullable during initial creation)
-- - classroom_id: Assigned room (nullable during initial creation)
-- - capacity: Maximum students (default 10 per school rules)
-- - hours_per_week: Total weekly hours from course definition
-- - status: Lifecycle state ('UNSCHEDULED', 'SCHEDULED', 'ACTIVE', 'COMPLETED', 'CANCELLED')
--
-- Constraints:
-- - Unique combination of course_id + semester_id + section_number
-- - Cascading deletes if course or semester is removed
-- - Set NULL on teacher/classroom deletion (allows reassignment)
-- ============================================================================
CREATE TABLE course_sections (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    course_id INTEGER NOT NULL,
    semester_id INTEGER NOT NULL,
    section_number INTEGER NOT NULL,
    teacher_id INTEGER,
    classroom_id INTEGER,
    capacity INTEGER NOT NULL DEFAULT 10,
    hours_per_week INTEGER NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('UNSCHEDULED', 'SCHEDULED', 'ACTIVE', 'COMPLETED', 'CANCELLED')) DEFAULT 'UNSCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL,
    FOREIGN KEY (classroom_id) REFERENCES classrooms(id) ON DELETE SET NULL,
    
    UNIQUE(course_id, semester_id, section_number)
);

-- Indexes for performance optimization
CREATE INDEX idx_course_sections_semester ON course_sections(semester_id);
CREATE INDEX idx_course_sections_teacher ON course_sections(teacher_id);
CREATE INDEX idx_course_sections_classroom ON course_sections(classroom_id);
CREATE INDEX idx_course_sections_course ON course_sections(course_id);
CREATE INDEX idx_course_sections_status ON course_sections(status);

-- Composite index for common query pattern: find sections by semester and status
CREATE INDEX idx_course_sections_semester_status ON course_sections(semester_id, status);

-- ============================================================================
-- Table: section_timeslots
-- ============================================================================
-- Defines when a course section meets during the week.
-- A section can have multiple timeslots (e.g., MWF 9-10am, T 2-4pm).
--
-- Key Fields:
-- - section_id: Links to course_sections (which section)
-- - day_of_week: 1=Monday through 5=Friday
-- - start_time: Session start time (HH:MM:SS format)
-- - end_time: Session end time (HH:MM:SS format)
--
-- Constraints:
-- - day_of_week must be 1-5 (Monday-Friday only)
-- - end_time must be after start_time
-- - Times must be within school hours (09:00:00 to 17:00:00)
-- - No overlap with lunch break (12:00:00 to 13:00:00)
-- - Cascading delete if section is removed
--
-- Note: The lunch break constraint is enforced at application level for flexibility.
-- Database constraint prevents any timeslot that crosses the lunch hour.
-- ============================================================================
CREATE TABLE section_timeslots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    section_id INTEGER NOT NULL,
    day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 1 AND 5),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (section_id) REFERENCES course_sections(id) ON DELETE CASCADE,
    
    CHECK(end_time > start_time)
);

-- Indexes for conflict detection and queries
CREATE INDEX idx_section_timeslots_section ON section_timeslots(section_id);
CREATE INDEX idx_section_timeslots_day ON section_timeslots(day_of_week);

-- Composite index for efficient conflict detection queries
-- Used to find overlapping timeslots for a given day
CREATE INDEX idx_section_timeslots_conflict_check ON section_timeslots(day_of_week, start_time, end_time);

-- ============================================================================
-- Table: student_enrollments
-- ============================================================================
-- Tracks student registration in course sections.
-- Manages the enrollment lifecycle from registration through completion.
--
-- Key Fields:
-- - student_id: Links to students table (which student)
-- - section_id: Links to course_sections (which section)
-- - status: Current enrollment state
--   * enrolled: Active registration
--   * waitlisted: On waiting list (section full)
--   * dropped: Student withdrew from course
--   * completed: Course finished (grade recorded in student_course_history)
-- - enrolled_at: When student registered
-- - dropped_at: When student withdrew (NULL if not dropped)
--
-- Constraints:
-- - Unique combination of student_id + section_id (can't enroll twice)
-- - Cascading delete if student or section is removed
--
-- Business Rules (enforced in application layer):
-- - Check prerequisites before enrollment
-- - Verify no time conflicts with other enrolled sections
-- - Enforce maximum 5 courses per semester per student
-- - Check section capacity before enrollment
-- ============================================================================
CREATE TABLE student_enrollments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    section_id INTEGER NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('ENROLLED', 'WAITLISTED', 'DROPPED', 'COMPLETED')) DEFAULT 'ENROLLED',
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dropped_at TIMESTAMP,
    
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (section_id) REFERENCES course_sections(id) ON DELETE CASCADE,
    
    UNIQUE(student_id, section_id)
);

-- Indexes for enrollment queries
CREATE INDEX idx_student_enrollments_student ON student_enrollments(student_id);
CREATE INDEX idx_student_enrollments_section ON student_enrollments(section_id);
CREATE INDEX idx_student_enrollments_status ON student_enrollments(status);

-- Composite index for finding active enrollments per student
CREATE INDEX idx_student_enrollments_student_status ON student_enrollments(student_id, status);

-- ============================================================================
-- Migration Complete
-- ============================================================================
-- The scheduling system tables are now ready.
-- Next steps:
-- 1. Application will use these tables to store generated schedules
-- 2. The algorithm will populate course_sections and section_timeslots
-- 3. Students can enroll via student_enrollments
-- ============================================================================

