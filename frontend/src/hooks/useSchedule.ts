/**
 * React Query hooks for schedule-related API calls
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '../services/api';
import type { EnrollmentRequest } from '../types';

// ============================================================================
// Schedule Generation
// ============================================================================

export const useGenerateScheduleV1 = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ semesterId }: { semesterId: number; }) =>
      api.generateSchedule(semesterId),
    onSuccess: (data) => {
      // Invalidate schedule queries to re-fetch
      queryClient.invalidateQueries({ queryKey: ['schedule', data.semesterId] });
      queryClient.invalidateQueries({ queryKey: ['sections', data.semesterId] });
    },
  });
};

// ============================================================================
// Schedule Queries
// ============================================================================

export const useSchedule = (semesterId: number) => {
  return useQuery({
    queryKey: ['schedule', semesterId],
    queryFn: () => api.getSchedule(semesterId),
    enabled: semesterId > 0,
  });
};

export const useSections = (
  semesterId: number,
  status?: string,
  courseId?: number
) => {
  return useQuery({
    queryKey: ['sections', semesterId, status, courseId],
    queryFn: () => api.getSections(semesterId, status, courseId),
    enabled: semesterId > 0,
  });
};

// ============================================================================
// Enrollment
// ============================================================================

export const useValidateEnrollment = (studentId: number, sectionId: number) => {
  return useQuery({
    queryKey: ['enrollment-validation', studentId, sectionId],
    queryFn: () => api.validateEnrollment(studentId, sectionId),
    enabled: studentId > 0 && sectionId > 0,
  });
};

export const useEnrollStudent = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: EnrollmentRequest) => api.enrollStudent(request),
    onSuccess: (_data, variables) => {
      // Invalidate relevant queries
      queryClient.invalidateQueries({
        queryKey: ['student-schedule', variables.studentId],
      });
      queryClient.invalidateQueries({
        queryKey: ['sections'],
      });
      queryClient.invalidateQueries({
        queryKey: ['academic-progress', variables.studentId],
      });
    },
  });
};

export const useStudentSchedule = (studentId: number, semesterId: number) => {
  return useQuery({
    queryKey: ['student-schedule', studentId, semesterId],
    queryFn: () => api.getStudentSchedule(studentId, semesterId),
    enabled: studentId > 0 && semesterId > 0,
  });
};

export const useAcademicProgress = (studentId: number) => {
  return useQuery({
    queryKey: ['academic-progress', studentId],
    queryFn: () => api.getAcademicProgress(studentId),
    enabled: studentId > 0,
  });
};
