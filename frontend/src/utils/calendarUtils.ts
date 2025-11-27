/**
 * Utility functions for calendar event conversion and manipulation
 */

import type { Section, CalendarEvent, CalendarEventExtended } from '../types';

// const DAY_MAP: Record<number, number> = {
//   1: 1, // Monday
//   2: 2, // Tuesday
//   3: 3, // Wednesday
//   4: 4, // Thursday
//   5: 5, // Friday
// };

// const COLORS = [
//   '#1976d2', // Blue
//   '#388e3c', // Green
//   '#d32f2f', // Red
//   '#7b1fa2', // Purple
//   '#f57c00', // Orange
//   '#0097a7', // Cyan
//   '#c2185b', // Pink
//   '#ffffff', // White
//   '#5d4037', // Brown
//   '#455a64', // Blue Grey
//   '#689f38', // Light Green
// ];

/**
 * Get a consistent color for a course based on its ID
 */
// export const getCourseColor = (courseId: number): string => {
//   return '#ffffff';// COLORS[courseId % COLORS.length];
// };

/**
 * Set today to next monday
 * @param today input current day
 * @returns 
 */
export const calculateNextMonday = (today: Date): Date => {
  const day = today.getDay();
  if (day == 1) return today;
  
  today.setDate(today.getDate() + 7-today.getDay()+1)
  return today;
}

/**
 * Convert sections with timeslots to FullCalendar events
 * 
 * @param sections - Array of sections with timeslots
 * @param referenceDate - Reference date for the week (defaults to current week's Monday)
 * @returns Array of calendar events
 */
export const sectionsToCalendarEvents = (
  sections: Section[],
  referenceDate?: Date
): CalendarEventExtended[] => {
  const events: CalendarEvent[] = [];
  
  // Get Monday of the current week as reference
  const refDate = calculateNextMonday(referenceDate as Date);
  // const refDate = new Date(referenceDate);

//   let currentDate = referenceDate;

//   const ssDate = "2022-01-17";

  sections.forEach((section) => {
//     const color = getCourseColor(section.courseId);

    section.timeslots.forEach((timeslot, index) => {
      // Calculate the date for this day of week
      const eventDate = new Date(refDate);
      eventDate.setDate(refDate.getDate() + (timeslot.dayOfWeek - 1));

      // // Parse start and end times
      const [startHour, startMinute] = timeslot.startTime.split(':').map(Number);
      const [endHour, endMinute] = timeslot.endTime.split(':').map(Number);

      const startDateTime = new Date(eventDate);
      startDateTime.setHours(startHour, startMinute, 0, 0);

      const endDateTime = new Date(eventDate);
      endDateTime.setHours(endHour, endMinute, 0, 0);

      events.push({
        id: `${section.id}-${index+1}`,
        title: `${section.courseCode} (${section.sectionNumber})`,
        start: startDateTime,
        end: endDateTime,
        timeSlot: timeslot.dayName + ',  ' + timeslot.startTime+' - '+timeslot.endTime,
        // backgroundColor: color,
        // borderColor: color,
        courseCode: section.courseCode,
        courseName: section.courseName,
        classroomName: section.classroomName,
        sectionNumber: section.sectionNumber,
        teacherName: section.teacherName,
      });
    });
  });

  const k = groupByStartDateAndEndDate(events);

  return k;
};

const groupByStartDateAndEndDate = (events: CalendarEvent[]): CalendarEventExtended[] => {
  const eventsMap = events.reduce((acc, e) => {
    const key = `${formatDate(e.start)}|${formatDate(e.end)}`;
    if (!acc.has(key)) {
      acc.set(key, []);
    }
    acc.get(key)!.push(e);
    return acc;
  }, new Map<string, CalendarEvent[]>());

  const finalEvents: CalendarEventExtended[] = [];

  for (let [key, value] of eventsMap) {
    finalEvents.push({
      id: key,
      title: '',
      backgroundColor: '#ffffff',
      start: value[0].start,
      end: value[0].end,
      events: value,
    });
  }

  return finalEvents;
}

/**
 * Get the Monday of the week containing the given date
 */
export const getMonday = (date: Date): Date => {
  const d = new Date(date);
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust when day is Sunday
  return new Date(d.setDate(diff));
};

export const formatDate = (date: Date): String => {
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0'); // months are 0-based
  const day = date.getDate().toString().padStart(2, '0');
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');
  const seconds = date.getSeconds().toString().padStart(2, '0');

  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
}

/**
 * Format time string (HH:mm) to display format
 */
export const formatTime = (time: string): string => {
  const [hour, minute] = time.split(':').map(Number);
  const period = hour >= 12 ? 'PM' : 'AM';
  const displayHour = hour % 12 || 12;
  return `${displayHour}:${minute.toString().padStart(2, '0')} ${period}`;
};

/**
 * Format time range
 */
export const formatTimeRange = (startTime: string, endTime: string): string => {
  return `${formatTime(startTime)} - ${formatTime(endTime)}`;
};

/**
 * Get day name from day of week number
 */
export const getDayName = (dayOfWeek: number): string => {
  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'];
  return days[dayOfWeek - 1] || 'Unknown';
};

/**
 * Get short day name from day of week number
 */
export const getShortDayName = (dayOfWeek: number): string => {
  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'];
  return days[dayOfWeek - 1] || '?';
};

/**
 * Check if two timeslots overlap
 */
export const timeslotsOverlap = (
  slot1: { dayOfWeek: number; startTime: string; endTime: string },
  slot2: { dayOfWeek: number; startTime: string; endTime: string }
): boolean => {
  if (slot1.dayOfWeek !== slot2.dayOfWeek) {
    return false;
  }

  return slot1.startTime < slot2.endTime && slot1.endTime > slot2.startTime;
};

/**
 * Group sections by day of week
 */
export const groupSectionsByDay = (
  sections: Section[]
): Record<number, Section[]> => {
  const grouped: Record<number, Section[]> = {
    1: [],
    2: [],
    3: [],
    4: [],
    5: [],
  };

  sections.forEach((section) => {
    section.timeslots.forEach((timeslot) => {
      if (!grouped[timeslot.dayOfWeek].includes(section)) {
        grouped[timeslot.dayOfWeek].push(section);
      }
    });
  });

  return grouped;
};

export const semesterList = [
  {
    value: 1,
    label: 'Fall 2021'
  },
  {
    value: 2,
    label: 'Spring 2021'
  },
  {
    value: 3,
    label: 'Fall 2022'
  },
  {
    value: 4,
    label: 'Spring 2022'
  },
  {
    value: 5,
    label: 'Fall 2023'
  },
  {
    value: 6,
    label: 'Spring 2023'
  },
  {
    value: 7,
    label: 'Fall 2024'
  },
  {
    value: 8,
    label: 'Spring 2024'
  },
  {
    value: 9,
    label: 'Fall 2025'
  }
]