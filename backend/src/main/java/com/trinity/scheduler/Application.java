package com.trinity.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Trinity College Scheduling System.
 * This application provides automated scheduling capabilities for a college
 * with 400 students, 50 teachers, and 60 classrooms.
 *
 * @author Atul Kumar
 * @version 1.0.0
 */
@SpringBootApplication
public class Application {

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

