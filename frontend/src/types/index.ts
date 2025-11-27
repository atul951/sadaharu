/**
 * TypeScript type definitions for Trinity College Scheduler frontend
 */

export interface Timeslot {
  id: number;
  sectionId: number;
  dayOfWeek: number; // 1=Mon, 5=Fri
  dayName: string;
  startTime: string; // HH:mm format
  endTime: string;
  durationHours: number;
}

export interface Section {
  id: number;
  courseId: number;
  courseCode: string;
  courseName: string;
  semesterId: number;
  sectionNumber: number;
  teacherId: number | null;
  teacherName: string | null;
  classroomId: number | null;
  classroomName: string | null;
  capacity: number;
  enrolledCount: number;
  hoursPerWeek: number;
  status: 'UNSCHEDULED' | 'SCHEDULED' | 'CANCELLED';
  timeslots: Timeslot[];
}

export interface ScheduleResponse {
  semesterId: number;
  algorithm: string;
  sectionsCreated: number;
  sectionsScheduled: number;
  sectionsFailed: number;
  sections: Section[];
  semesterStartDate: string;
  semesterEndDate: string;
  statistics: {
    success_rate: number;
    execution_time_ms: number;
    [key: string]: any;
  };
}

export interface EnrollmentRequest {
  studentId: number;
  sectionId: number;
}

export interface EnrollmentResponse {
  success: boolean;
  message: string;
  enrollment: {
    id: number;
    studentId: number;
    sectionId: number;
    status: 'ENROLLED' | 'WAITLISTED' | 'DROPPED';
    enrolledAt: string;
  } | null;
  conflicts: string[];
}

export interface ValidationResponse {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

export interface StudentSchedule {
  studentId: number;
  semesterId: number;
  enrolledCourses: number;
  semesterStartDate: string;
  semesterEndDate: string;
  sections: Section[];
}

export interface AcademicProgress {
  studentId: number;
  studentName: string;
  gradeLevel: number;
  creditsEarned: number;
  creditsRequired: number;
  creditsRemaining: number;
  coursesCompleted: number;
  coursesEnrolled: number;
  onTrackForGraduation: boolean;
}

export interface Semester {
  id: number;
  name: string;
  orderInYear: number;
  startDate: string;
  endDate: string;
}

export interface Student {
  id: number;
  firstName: string;
  lastName: string;
  gradeLevel: number;
  email: string;
}

// FullCalendar event type
export interface CalendarEvent {
  id: string;
  title: string;
  start: Date;
  end: Date;
  timeSlot: string;
  courseCode: string;
  courseName: string;
  classroomName: string | null;
  sectionNumber: number;
  teacherName: string | null;
}

export interface CalendarEventExtended {
  id: string;
  title: string;
  start: Date;
  end: Date;
  backgroundColor: string
  events: CalendarEvent[];
}

