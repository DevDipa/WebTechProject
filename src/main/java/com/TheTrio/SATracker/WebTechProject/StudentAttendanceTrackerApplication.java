package com.TheTrio.SATracker.WebTechProject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Ensure Spring scans the top-level package so it finds repositories and services
@SpringBootApplication(scanBasePackages = "com.TheTrio.SATracker")
@EnableJpaRepositories(basePackages = "com.TheTrio.SATracker.repository")
@EntityScan(basePackages = "com.TheTrio.SATracker.models")
public class StudentAttendanceTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudentAttendanceTrackerApplication.class, args);
	}

}
