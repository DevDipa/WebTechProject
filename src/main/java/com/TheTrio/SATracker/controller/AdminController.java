package com.TheTrio.SATracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        // DevTools restart demo: harmless log to confirm class change detection
        System.out.println("[DEVTOOLS DEMO] adminDashboard method loaded — " + System.currentTimeMillis());
        model.addAttribute("active", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/admin/batches")
    public String batches(Model model) {
        model.addAttribute("active", "batches");
        return "admin/batches";
    }

    @GetMapping("/admin/teachers")
    public String teachers(Model model) {
        model.addAttribute("active", "teachers");
        return "admin/teachers";
    }

    @GetMapping("/admin/students")
    public String students(Model model) {
        model.addAttribute("active", "students");
        return "admin/students";
    }

    @GetMapping("/admin/holidays")
    public String holidays(Model model) {
        model.addAttribute("active", "holidays");
        return "admin/holidays";
    }
}
