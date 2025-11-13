package com.TheTrio.SATracker.controller;

import com.TheTrio.SATracker.models.AttendanceStatus;
import com.TheTrio.SATracker.repository.AttendanceRepository;
import com.TheTrio.SATracker.repository.BatchRepository;
import com.TheTrio.SATracker.repository.HolidayRepository;
import com.TheTrio.SATracker.repository.StudentRepository;
import com.TheTrio.SATracker.repository.TeacherRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AdminStatsController {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final BatchRepository batchRepository;
    private final HolidayRepository holidayRepository;
    private final AttendanceRepository attendanceRepository;

    public AdminStatsController(StudentRepository studentRepository,
                                TeacherRepository teacherRepository,
                                BatchRepository batchRepository,
                                HolidayRepository holidayRepository,
                                AttendanceRepository attendanceRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.batchRepository = batchRepository;
        this.holidayRepository = holidayRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping("/admin/api/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new HashMap<>();
        m.put("studentsCount", studentRepository.count());
        m.put("teachersCount", teacherRepository.count());
        m.put("batchesCount", batchRepository.count());
        m.put("holidaysCount", holidayRepository.count());

        LocalDate today = LocalDate.now();
        long totalToday = attendanceRepository.countByDate(today);
        long present = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.PRESENT);
        long absent = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.ABSENT);

        m.put("attendanceTodayTotal", totalToday);
        m.put("attendanceTodayPresent", present);
        m.put("attendanceTodayAbsent", absent);

        return m;
    }
}
