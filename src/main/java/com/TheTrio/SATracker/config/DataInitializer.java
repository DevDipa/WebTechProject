package com.TheTrio.SATracker.config;

import com.TheTrio.SATracker.models.Batch;
import com.TheTrio.SATracker.models.Student;
import com.TheTrio.SATracker.models.User;
import com.TheTrio.SATracker.repository.BatchRepository;
import com.TheTrio.SATracker.repository.StudentRepository;
import com.TheTrio.SATracker.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer {

    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(BatchRepository batchRepository, UserRepository userRepository, StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try{
            // create a default admin user for development if missing
            if(userRepository.findByUsername("admin").isEmpty()){
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("Administrator");
                admin.setEmail("admin@example.com");
                admin.setRole(User.Role.ADMIN);
                userRepository.save(admin);
                System.out.println("[DataInitializer] Created admin user 'admin' with password 'admin123'");
            }
            if(batchRepository.count()==0){
                Batch b = new Batch(); b.setBatchName("Sample Batch"); b.setYear(LocalDate.now().getYear());
                batchRepository.save(b);
                System.out.println("[DataInitializer] Created sample batch");
            }

            if(studentRepository.count()==0){
                Batch any = batchRepository.findAll().stream().findFirst().orElse(null);
                User u = new User();
                u.setUsername("student1");
                u.setPassword(passwordEncoder.encode("student123"));
                u.setFullName("Sample Student");
                u.setEmail("student1@example.com");
                u.setRole(User.Role.STUDENT);
                User saved = userRepository.save(u);

                Student s = new Student();
                s.setUser(saved);
                s.setPhone("9999999999");
                s.setAddress("123 Main St");
                s.setDob(LocalDate.of(2005,1,1));
                s.setBloodGroup("O+");
                s.setBatch(any);
                studentRepository.save(s);
                System.out.println("[DataInitializer] Created sample student");
            }
        }catch(Exception ex){ System.out.println("[DataInitializer] init failed: " + ex.getMessage()); }
    }
}
