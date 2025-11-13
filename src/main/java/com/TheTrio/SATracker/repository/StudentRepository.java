package com.TheTrio.SATracker.repository;

import com.TheTrio.SATracker.models.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Integer> {
	List<Student> findByBatch_Id(Integer batchId);
}