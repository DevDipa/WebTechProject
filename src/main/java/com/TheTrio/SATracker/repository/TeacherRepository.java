package com.TheTrio.SATracker.repository;

import com.TheTrio.SATracker.models.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
	Optional<Teacher> findByUser_Username(String username);
}


