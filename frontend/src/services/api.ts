/**
 * API service for communicating with Trinity College Time Table backend
 */

import axios from 'axios';
import type {
  ScheduleResponse,
  Section,
  EnrollmentRequest,
  EnrollmentResponse,
  ValidationResponse,
  StudentSchedule,
  AcademicProgress,
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const API_ENDPOINTS = {
   GENERATE_SEMESTER_SCHEDULE: '/api/semesters/{semesterId}/schedule',
   GET_SEMESTER_SCHEDULE: '/api/semesters/{semesterId}',
   GET_COURSE_SECTIONS: '/api/sections',
   VALIDATE_STUDENT_ENROLLMENT: '/api/students/{studentId}/enroll/validate?sectionId={sectionId}',
   ENROLL_STUDENT: '/api/students/{studentId}/enroll',
   GET_STUDENT_SCHEDULE: '/api/students/{studentId}/schedule',
   GET_STUDENT_ACADEMIC_PROGRESS: '/api/students/{studentId}/progress',
};

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor for logging
api.interceptors.request.use(
  (config) => {
    console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// ============================================================================
// Schedule Generation APIs
// ============================================================================

export const generateSchedule = async (
  semesterId: number
): Promise<ScheduleResponse> => {
  const response = await api.post<ScheduleResponse>(
    API_ENDPOINTS.GENERATE_SEMESTER_SCHEDULE.replace('{semesterId}', semesterId.toString()),
    null
  );
  return response.data;
};

// ============================================================================
// Common Query APIs
// ============================================================================

export const getSchedule = async (semesterId: number): Promise<ScheduleResponse> => {
  const response = await api.get<ScheduleResponse>(
    API_ENDPOINTS.GET_SEMESTER_SCHEDULE.replace('{semesterId}', semesterId.toString())
  );
  return response.data;
};

export const getSections = async (
  semesterId: number,
  status?: string,
  courseId?: number
): Promise<Section[]> => {
  const response = await api.get<Section[]>(
    API_ENDPOINTS.GET_COURSE_SECTIONS,
    {
        params: { semesterId, status, courseId },
    }
  );
  return response.data;
};

// ============================================================================
// Enrollment APIs
// ============================================================================

export const validateEnrollment = async (
  studentId: number,
  sectionId: number
): Promise<ValidationResponse> => {
  const response = await api.get<ValidationResponse>(
    API_ENDPOINTS.VALIDATE_STUDENT_ENROLLMENT
        .replace('{studentId}', studentId.toString())
        .replace('{sectionId}', sectionId.toString())
  );
  return response.data;
};

export const enrollStudent = async (
  request: EnrollmentRequest
): Promise<EnrollmentResponse> => {
  const response = await api.post<EnrollmentResponse>(
    API_ENDPOINTS.ENROLL_STUDENT.replace('{studentId}', request.studentId.toString()),
    request
  );
  return response.data;
};

export const getStudentSchedule = async (
  studentId: number,
  semesterId: number
): Promise<StudentSchedule> => {
  const response = await api.get<StudentSchedule>(
    API_ENDPOINTS.GET_STUDENT_SCHEDULE.replace('{studentId}', studentId.toString()),
    {
      params: { semesterId },
    }
  );
  return response.data;
};

export const getAcademicProgress = async (
  studentId: number
): Promise<AcademicProgress> => {
  const response = await api.get<AcademicProgress>(
    API_ENDPOINTS.GET_STUDENT_ACADEMIC_PROGRESS.replace('{studentId}', studentId.toString())
  );
  return response.data;
};

export default api;
