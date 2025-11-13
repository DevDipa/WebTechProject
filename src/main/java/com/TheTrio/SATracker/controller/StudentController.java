package com.TheTrio.SATracker.controller;

import com.TheTrio.SATracker.models.Attendance;
import com.TheTrio.SATracker.models.AttendanceStatus;
import com.TheTrio.SATracker.models.Student;
import com.TheTrio.SATracker.repository.AttendanceRepository;
import com.TheTrio.SATracker.repository.StudentRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Controller
public class StudentController {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;

    public StudentController(StudentRepository studentRepository, AttendanceRepository attendanceRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping("/student/dashboard")
    public String dashboard(Authentication auth, Model model){
        if(auth==null) return "redirect:/login";
        String username = auth.getName();
        Optional<Student> opt = studentRepository.findByUser_Username(username);
        if(opt.isEmpty()) return "redirect:/login";
        Student s = opt.get();
        model.addAttribute("active", "dashboard");
        model.addAttribute("studentName", s.getUser()!=null? s.getUser().getFullName(): username);
        return "student/dashboard";
    }

    @GetMapping("/student/performance")
    public String performance(Authentication auth, Model model){
        if(auth==null) return "redirect:/login";
        String username = auth.getName();
        Optional<Student> opt = studentRepository.findByUser_Username(username);
        if(opt.isEmpty()) return "redirect:/login";
        Student s = opt.get();
        model.addAttribute("active", "performance");
        model.addAttribute("studentName", s.getUser()!=null? s.getUser().getFullName(): username);
        return "student/performance";
    }

    // ===== REST for current month summary =====
    @GetMapping("/student/api/summary")
    @ResponseBody
    public Map<String,Object> monthlySummary(Authentication auth,
                                             @RequestParam(value = "month", required = false)
                                             @DateTimeFormat(pattern = "yyyy-MM") YearMonth month){
        String username = auth.getName();
        Optional<Student> opt = studentRepository.findByUser_Username(username);
        if(opt.isEmpty()) return Map.of();
        Student s = opt.get();
        YearMonth ym = month!=null? month : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Attendance> list = attendanceRepository.findByStudent_IdAndDateBetween(s.getId(), start, end);
        long present = list.stream().filter(a -> a.getStatus()== AttendanceStatus.PRESENT).count();
        long absent = list.stream().filter(a -> a.getStatus()== AttendanceStatus.ABSENT).count();
        long late = list.stream().filter(a -> a.getStatus()== AttendanceStatus.LATE).count();
        long total = list.size();
        double avg = total==0? 0 : (present*100.0/total);
        Map<String,Object> m = new HashMap<>();
        m.put("month", ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        m.put("year", ym.getYear());
        m.put("present", present);
        m.put("absent", absent);
        m.put("late", late);
        m.put("average", Math.round(avg));
        return m;
    }

    // Daily series for chart for a given month (current if null)
    @GetMapping("/student/api/series")
    @ResponseBody
    public Map<String,Object> monthlySeries(Authentication auth,
                                            @RequestParam(value = "month", required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month){
        String username = auth.getName();
        Optional<Student> opt = studentRepository.findByUser_Username(username);
        if(opt.isEmpty()) return Map.of();
        Student s = opt.get();
        YearMonth ym = month!=null? month : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Attendance> list = attendanceRepository.findByStudent_IdAndDateBetween(s.getId(), start, end);
        Map<LocalDate, AttendanceStatus> byDate = new HashMap<>();
        for(Attendance a: list){
            byDate.put(a.getDate(), a.getStatus());
        }
        List<String> labels = new ArrayList<>();
        List<Integer> present = new ArrayList<>();
        List<Integer> absent = new ArrayList<>();
        List<Integer> late = new ArrayList<>();
        for(int d=1; d<=ym.lengthOfMonth(); d++){
            LocalDate day = ym.atDay(d);
            labels.add(String.valueOf(d));
            AttendanceStatus st = byDate.get(day);
            present.add(st==AttendanceStatus.PRESENT?1:0);
            absent.add(st==AttendanceStatus.ABSENT?1:0);
            late.add(st==AttendanceStatus.LATE?1:0);
        }
        return Map.of(
                "labels", labels,
                "present", present,
                "absent", absent,
                "late", late
        );
    }

    // Generate simple monthly PDF report
    @GetMapping("/student/report.pdf")
    public ResponseEntity<byte[]> report(Authentication auth,
                                         @RequestParam(value = "month", required = false)
                                         @DateTimeFormat(pattern = "yyyy-MM") YearMonth month){
        String username = auth.getName();
        Optional<Student> opt = studentRepository.findByUser_Username(username);
        if(opt.isEmpty()) return ResponseEntity.status(302).header("Location","/login").build();
        Student s = opt.get();
        YearMonth ym = month!=null? month : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<Attendance> list = attendanceRepository.findByStudent_IdAndDateBetween(s.getId(), start, end);
        long present = list.stream().filter(a -> a.getStatus()== AttendanceStatus.PRESENT).count();
        long absent = list.stream().filter(a -> a.getStatus()== AttendanceStatus.ABSENT).count();
        long late = list.stream().filter(a -> a.getStatus()== AttendanceStatus.LATE).count();
        long total = list.size();
        double avg = total==0?0:(present*100.0/total);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4);
        try{
            PdfWriter.getInstance(doc, baos);
            doc.open();
            Font h1 = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font text = new Font(Font.HELVETICA, 12, Font.NORMAL);

            Paragraph title = new Paragraph("Attendance Report", h1);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Student: " + (s.getUser()!=null? s.getUser().getFullName(): username), text));
            doc.add(new Paragraph("Username: " + username, text));
            doc.add(new Paragraph("Month: " + ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear(), text));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(60);
            table.setSpacingBefore(10);
            table.addCell(cell("Present"));
            table.addCell(cell(String.valueOf(present)));
            table.addCell(cell("Absent"));
            table.addCell(cell(String.valueOf(absent)));
            table.addCell(cell("Late"));
            table.addCell(cell(String.valueOf(late)));
            table.addCell(cell("Average"));
            table.addCell(cell(String.format(Locale.ENGLISH, "%.0f%%", avg)));
            doc.add(table);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Generated on: " + LocalDate.now(), new Font(Font.HELVETICA, 10)));
        }catch(Exception ex){
            // swallow for now
        } finally {
            doc.close();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-"+ym+".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(baos.toByteArray());
    }

    private PdfPCell cell(String text){
        PdfPCell c = new PdfPCell(new Phrase(text));
        c.setPadding(6);
        return c;
    }
}
