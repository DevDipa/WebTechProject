package com.TheTrio.SATracker.repository;

import com.TheTrio.SATracker.models.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface HolidayRepository extends JpaRepository<Holiday, Integer> {
	boolean existsByDate(LocalDate date);
	long count();
}