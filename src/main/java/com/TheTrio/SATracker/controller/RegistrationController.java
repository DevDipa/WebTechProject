package com.TheTrio.SATracker.controller;

import com.TheTrio.SATracker.models.User;
import com.TheTrio.SATracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class RegistrationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "register";
    }

    @GetMapping("/")
    public String showRootRegistration(Model model) {
        // Serve the registration page at the site root as requested
        model.addAttribute("userForm", new UserForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("userForm") UserForm form,
                               Model model) {

    Optional<User> existing = userRepository.findByUsername(form.getUsername());
        if (existing.isPresent()) {
            model.addAttribute("errorMessage", "Username already taken");
            return "register";
        }

        User user = new User();
        user.setUsername(form.getUsername());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        try {
            user.setRole(User.Role.valueOf(form.getRole()));
        } catch (Exception e) {
            user.setRole(User.Role.STUDENT);
        }
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());

        userRepository.save(user);

        model.addAttribute("successMessage", "Registration successful. You can now log in.");
        // Return the login view so the user can sign in after registering.
        // Note: we also expose a GET /login mapping below so Spring will render the
        // Thymeleaf login template instead of triggering a redirect loop.
        return "login";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model, @org.springframework.web.bind.annotation.RequestParam(value = "error", required = false) String error,
                                @org.springframework.web.bind.annotation.RequestParam(value = "logout", required = false) String logout) {
        if (error != null) {
            model.addAttribute("error", true);
        }
        if (logout != null) {
            model.addAttribute("logout", true);
        }
        return "login";
    }

    // Simple DTO for the form
    public static class UserForm {
        private String username;
        private String password;
        private String fullName;
        private String email;
        private String role = "STUDENT";

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
