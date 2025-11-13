package com.TheTrio.SATracker.controller;

import com.TheTrio.SATracker.models.Attendance;
import com.TheTrio.SATracker.models.AttendanceStatus;
import com.TheTrio.SATracker.models.Student;
import com.TheTrio.SATracker.models.Batch;
import com.TheTrio.SATracker.models.Teacher;
import com.TheTrio.SATracker.repository.AttendanceRepository;
import com.TheTrio.SATracker.repository.HolidayRepository;
import com.TheTrio.SATracker.repository.StudentRepository;
import com.TheTrio.SATracker.repository.TeacherRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class TeacherController {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;

    public TeacherController(TeacherRepository teacherRepository, StudentRepository studentRepository, AttendanceRepository attendanceRepository, HolidayRepository holidayRepository) {
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.holidayRepository = holidayRepository;
    }

    @GetMapping("/teacher/dashboard")
    public String dashboard(Authentication auth, Model model){
        String username = auth.getName();
        Optional<Teacher> opt = teacherRepository.findByUser_Username(username);
        if(opt.isEmpty()) return "redirect:/login";
        Teacher t = opt.get();
        model.addAttribute("active","dashboard");
        model.addAttribute("teacherName", t.getUser()!=null? t.getUser().getFullName(): username);
        return "teacher/dashboard";
    }

    @GetMapping("/teacher/attendance")
    public String attendancePage(Authentication auth, Model model){
        String username = auth.getName();
        Optional<Teacher> opt = teacherRepository.findByUser_Username(username);
        if(opt.isEmpty()) return "redirect:/login";
        Teacher t = opt.get();
        model.addAttribute("active","attendance");
        model.addAttribute("teacherName", t.getUser()!=null? t.getUser().getFullName(): username);
        return "teacher/attendance";
    }

    // --- REST endpoints for teacher frontend ---

    @GetMapping("/teacher/api/summary")
    @ResponseBody
    public Map<String, Object> summary(Authentication auth){
        String username = auth.getName();
        Optional<Teacher> opt = teacherRepository.findByUser_Username(username);
        if(opt.isEmpty()) return Map.of();
        Teacher t = opt.get();
        Integer batchId = t.getBatch()!=null? t.getBatch().getId() : null;
        long totalStudents = 0;
        long attendancePresent = 0;
        if(batchId!=null){
            List<Student> students = studentRepository.findByBatch_Id(batchId);
            totalStudents = students.size();
            List<Integer> ids = students.stream().map(Student::getId).collect(Collectors.toList());
            LocalDate today = LocalDate.now();
            attendancePresent = attendanceRepository.findByDateAndStudent_IdIn(today, ids).stream().filter(a->a.getStatus()==AttendanceStatus.PRESENT).count();
        }
        long totalHolidays = holidayRepository.count();
        Map<String,Object> m = new HashMap<>();
        m.put("teacherName", t.getUser()!=null? t.getUser().getFullName(): username);
        m.put("totalStudents", totalStudents);
        m.put("attendanceToday", attendancePresent);
        m.put("totalHolidays", totalHolidays);
        return m;
    }

    @GetMapping("/teacher/api/students")
    @ResponseBody
    public List<Map<String,Object>> studentsForTeacher(Authentication auth){
        String username = auth.getName();
        Optional<Teacher> opt = teacherRepository.findByUser_Username(username);
        if(opt.isEmpty()) return List.of();
        Teacher t = opt.get();
        Integer batchId = t.getBatch()!=null? t.getBatch().getId() : null;
        if(batchId==null) return List.of();
        List<Student> list = studentRepository.findByBatch_Id(batchId);
        List<Map<String,Object>> out = new ArrayList<>();
        for(Student s : list){
            Map<String,Object> m = new HashMap<>();
            m.put("id", s.getId());
            Map<String,Object> u = new HashMap<>();
            if(s.getUser()!=null){
                u.put("fullName", s.getUser().getFullName());
                u.put("username", s.getUser().getUsername());
            }
            m.put("user", u);
            out.add(m);
        }
        return out;
    }

    @GetMapping("/teacher/api/batch")
    @ResponseBody
    public Map<String,Object> batchForTeacher(Authentication auth){
        String username = auth.getName();
        Optional<Teacher> opt = teacherRepository.findByUser_Username(username);
        if(opt.isEmpty()) return Map.of();
        Teacher t = opt.get();
        Batch b = t.getBatch();
        if(b==null) return Map.of();
        Map<String,Object> m = new HashMap<>();
        m.put("id", b.getId());
        m.put("batchName", b.getBatchName());
        m.put("year", b.getYear());
        return m;
    }

    @PostMapping("/teacher/api/attendance")
    @ResponseBody
    public Map<String,Object> saveAttendance(Authentication auth,
            @RequestBody Map<String, Object> payload){
        // payload: { date: '2025-11-13', records: [ { studentId: 1, status: 'PRESENT' }, ... ] }
        String username = auth.getName();
        Optional<Teacher> opt = teacherRepository.findByUser_Username(username);
        if(opt.isEmpty()) return Map.of("error","not authorized");
        Teacher t = opt.get();
        Integer batchId = t.getBatch()!=null? t.getBatch().getId() : null;
        if(batchId==null) return Map.of("error","no batch assigned");

        String dateStr = (String) payload.get("date");
        LocalDate date = LocalDate.parse(dateStr);

        // validations
        if(date.isAfter(LocalDate.now())) return Map.of("error","future dates not allowed");
        if(date.getDayOfWeek()== DayOfWeek.SATURDAY) return Map.of("error","cannot mark attendance on Saturday");
        if(holidayRepository.existsByDate(date)) return Map.of("error","date is a registered holiday");

        List<Map<String,Object>> records = (List<Map<String,Object>>) payload.get("records");
        int saved = 0;
        for(Map<String,Object> r: records){
            Integer studentId = (Integer) (r.get("studentId") instanceof Integer? r.get("studentId") : ((Number)r.get("studentId")).intValue());
            String statusStr = (String) r.get("status");
            AttendanceStatus status = AttendanceStatus.valueOf(statusStr);
            String remarks = r.get("remarks")!=null? String.valueOf(r.get("remarks")) : null;
            Optional<com.TheTrio.SATracker.models.Attendance> existing = attendanceRepository.findByStudent_IdAndDate(studentId, date);
            com.TheTrio.SATracker.models.Attendance a;
                if(existing.isPresent()){
                a = existing.get();
                a.setStatus(status);
                a.setRemarks(remarks);
            } else {
                a = new com.TheTrio.SATracker.models.Attendance();
                Student s = studentRepository.findById(studentId).orElse(null);
                if(s==null) continue;
                a.setStudent(s);
                a.setDate(date);
                a.setStatus(status);
                a.setRemarks(remarks);
            }
            attendanceRepository.save(a);
            saved++;
        }
        return Map.of("saved", saved);
    }

    @GetMapping("/teacher/api/isHoliday")
    @ResponseBody
    public Map<String,Object> isHoliday(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date){
        boolean h = holidayRepository.existsByDate(date);
        boolean saturday = date.getDayOfWeek()== DayOfWeek.SATURDAY;
        boolean future = date.isAfter(LocalDate.now());
        return Map.of("isHoliday", h, "isSaturday", saturday, "isFuture", future);
    }

    // Attendance history aggregation for teacher's batch
    @GetMapping("/teacher/api/records")
    @ResponseBody
    public List<Map<String,Object>> records(Authentication auth){
        String username = auth.getName();
        Optional<Teacher> opt = teacherRepository.findByUser_Username(username);
        if(opt.isEmpty()) return List.of();
        Teacher t = opt.get();
        Integer batchId = t.getBatch()!=null? t.getBatch().getId() : null;
        if(batchId==null) return List.of();
        List<Student> students = studentRepository.findByBatch_Id(batchId);
        List<Integer> ids = students.stream().map(Student::getId).collect(Collectors.toList());
        if(ids.isEmpty()) return List.of();
        List<com.TheTrio.SATracker.models.Attendance> all = attendanceRepository.findByStudent_IdIn(ids);
        // group by date
        Map<LocalDate, List<com.TheTrio.SATracker.models.Attendance>> grouped = all.stream().collect(Collectors.groupingBy(com.TheTrio.SATracker.models.Attendance::getDate));
        List<Map<String,Object>> out = new ArrayList<>();
        grouped.forEach((date, list)->{
            long present = list.stream().filter(a->a.getStatus()==AttendanceStatus.PRESENT).count();
            long late = list.stream().filter(a->a.getStatus()==AttendanceStatus.LATE).count();
            long absent = list.stream().filter(a->a.getStatus()==AttendanceStatus.ABSENT).count();
            Map<String,Object> m = new HashMap<>();
            m.put("date", date.toString());
            m.put("total", list.size());
            m.put("present", present);
            m.put("late", late);
            m.put("absent", absent);
            out.add(m);
        });
        // sort by date desc
        out.sort((a,b)-> ((String)b.get("date")).compareTo((String)a.get("date")));
        return out;
    }

    @GetMapping("/teacher/api/records/{date}")
    @ResponseBody
    public List<Map<String,Object>> recordsForDate(Authentication auth, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date){
        String username = auth.getName();
        Optional<Teacher> opt = teacherRepository.findByUser_Username(username);
        if(opt.isEmpty()) return List.of();
        Teacher t = opt.get();
        Integer batchId = t.getBatch()!=null? t.getBatch().getId() : null;
        if(batchId==null) return List.of();
        List<Student> students = studentRepository.findByBatch_Id(batchId);
        List<Integer> ids = students.stream().map(Student::getId).collect(Collectors.toList());
        if(ids.isEmpty()) return List.of();
        List<com.TheTrio.SATracker.models.Attendance> list = attendanceRepository.findByDateAndStudent_IdIn(date, ids);
        List<Map<String,Object>> out = new ArrayList<>();
        for(com.TheTrio.SATracker.models.Attendance a: list){
            Map<String,Object> m = new HashMap<>();
            Student s = a.getStudent();
            m.put("studentId", s.getId());
            m.put("name", s.getUser()!=null? s.getUser().getFullName() : "");
            m.put("status", a.getStatus().name());
            m.put("remarks", a.getRemarks());
            out.add(m);
        }
        return out;
    }

    @GetMapping("/teacher/records")
    public String recordsPage(Authentication auth, Model model){
        String username = auth.getName();
        Optional<Teacher> opt = teacherRepository.findByUser_Username(username);
        if(opt.isEmpty()) return "redirect:/login";
        Teacher t = opt.get();
        model.addAttribute("active","records");
        model.addAttribute("teacherName", t.getUser()!=null? t.getUser().getFullName(): username);
        return "teacher/records";
    }

    @GetMapping("/teacher/records/{date}")
    public String recordsDetailPage(Authentication auth, @PathVariable String date, Model model){
        String username = auth.getName();
        Optional<Teacher> opt = teacherRepository.findByUser_Username(username);
        if(opt.isEmpty()) return "redirect:/login";
        Teacher t = opt.get();
        model.addAttribute("active","records");
        model.addAttribute("teacherName", t.getUser()!=null? t.getUser().getFullName(): username);
        model.addAttribute("date", date);
        return "teacher/records-detail";
    }
}
