package com.TheTrio.SATracker.repository;

import com.TheTrio.SATracker.models.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Integer> {}