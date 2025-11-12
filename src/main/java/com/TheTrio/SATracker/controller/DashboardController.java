package com.TheTrio.SATracker.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        // DevTools demo: print a timestamp when this method class is loaded/used
        System.out.println("[DEVTOOLS DEMO] DashboardController loaded at " + System.currentTimeMillis());
        if (authentication == null) {
            return "redirect:/login";
        }

        for (GrantedAuthority a : authentication.getAuthorities()) {
            String role = a.getAuthority();
            if ("ADMIN".equals(role)) {
                return "redirect:/admin/dashboard";
            }
            if ("TEACHER".equals(role)) {
                return "redirect:/teacher/dashboard";
            }
            if ("STUDENT".equals(role)) {
                return "redirect:/student/dashboard";
            }
        }

        // default fallback
        return "redirect:/login";
    }
}
