package com.trinity.scheduler.service.student;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.trinity.scheduler.entity.Student;
import com.trinity.scheduler.entity.StudentCourseHistory;
import com.trinity.scheduler.repository.StudentCourseHistoryRepository;
import com.trinity.scheduler.repository.StudentRepository;

import java.util.List;

/**
 * Service class for Student.
 */
@Service
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private final StudentRepository studentRepository;
    private final StudentCourseHistoryRepository studentCourseHistoryRepository;

    public StudentService(StudentRepository studentRepository,
                          StudentCourseHistoryRepository studentCourseHistoryRepository) {
        this.studentRepository = studentRepository;
        this.studentCourseHistoryRepository = studentCourseHistoryRepository;
    }

    /**
     * Retrieves a Student by its ID.
     *
     * @param studentId the ID of the student
     * @return the Student entity
     * @throws IllegalArgumentException if the student is not found
     */
    public Student getStudentById(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
    }

    /**
     * Retrieves a Student's course history.
     *
     * @param studentId the ID of the student
     * @return the list of course history of the student
     */
    public List<StudentCourseHistory> getStudentCourseHistory(Long studentId) {
        return studentCourseHistoryRepository.findByStudentId(studentId);
    }
}
