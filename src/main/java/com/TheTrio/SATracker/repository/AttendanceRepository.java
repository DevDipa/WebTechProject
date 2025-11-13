package com.TheTrio.SATracker.repository;

import com.TheTrio.SATracker.models.Attendance;
import com.TheTrio.SATracker.models.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
	long countByDate(LocalDate date);
	long countByDateAndStatus(LocalDate date, AttendanceStatus status);

    List<Attendance> findByDateAndStudent_IdIn(LocalDate date, List<Integer> studentIds);
    Optional<Attendance> findByStudent_IdAndDate(Integer studentId, LocalDate date);
	List<Attendance> findByStudent_IdIn(List<Integer> studentIds);
    List<Attendance> findByStudent_IdAndDateBetween(Integer studentId, LocalDate start, LocalDate end);
}
