package com.TheTrio.SATracker.controller;

import com.TheTrio.SATracker.models.Batch;
import com.TheTrio.SATracker.models.Teacher;
import com.TheTrio.SATracker.models.User;
import com.TheTrio.SATracker.repository.BatchRepository;
import com.TheTrio.SATracker.repository.TeacherRepository;
import com.TheTrio.SATracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/api/teachers")
public class AdminTeacherRestController {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminTeacherRestController(TeacherRepository teacherRepository, UserRepository userRepository, BatchRepository batchRepository, PasswordEncoder passwordEncoder) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Utility to parse batchId which may arrive as Number or String from the client
    private Integer parseInteger(Object o){
        if(o==null) return null;
        if(o instanceof Number) return ((Number)o).intValue();
        if(o instanceof String){
            String s = ((String)o).trim();
            if(s.isEmpty()) return null;
            try{ return Integer.parseInt(s); }catch(NumberFormatException ex){ return null; }
        }
        return null;
    }

    @GetMapping
    public List<Teacher> list(){
        return teacherRepository.findAll();
    }

    // DTO expected: { username, password, fullName, email, phone, address, batchId }
    @PostMapping
    public ResponseEntity<?> create(@RequestBody java.util.Map<String,Object> body){
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String fullName = (String) body.get("fullName");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        String address = (String) body.get("address");
    Integer batchId = parseInteger(body.get("batchId"));

        if(username==null || username.trim().isEmpty()) return ResponseEntity.badRequest().body("username required");
        if(password==null || password.trim().isEmpty()) return ResponseEntity.badRequest().body("password required");

        if(userRepository.findByUsername(username).isPresent()) return ResponseEntity.status(HttpStatus.CONFLICT).body("username exists");

        User u = new User();
        u.setUsername(username.trim());
        u.setPassword(passwordEncoder.encode(password));
        u.setFullName(fullName);
        u.setEmail(email);
    u.setRole(User.Role.TEACHER);
        User savedUser = userRepository.save(u);

        Teacher t = new Teacher();
        t.setUser(savedUser);
        t.setPhone(phone);
        t.setAddress(address);
        if(batchId!=null){
            Optional<Batch> b = batchRepository.findById(batchId);
            b.ifPresent(t::setBatch);
        }
        Teacher saved = teacherRepository.save(t);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody java.util.Map<String,Object> body){
        Optional<Teacher> opt = teacherRepository.findById(id);
        if(opt.isEmpty()) return ResponseEntity.notFound().build();
        Teacher t = opt.get();

        // update teacher fields
        if(body.containsKey("phone")) t.setPhone((String) body.get("phone"));
        if(body.containsKey("address")) t.setAddress((String) body.get("address"));
        if(body.containsKey("batchId")){
            Object bi = body.get("batchId");
            Integer batchId = parseInteger(bi);
            if(batchId==null) t.setBatch(null);
            else batchRepository.findById(batchId).ifPresent(t::setBatch);
        }

        // update user fields optionally
        User u = t.getUser();
        if(body.containsKey("fullName")) u.setFullName((String) body.get("fullName"));
        if(body.containsKey("email")) u.setEmail((String) body.get("email"));
        if(body.containsKey("password") && body.get("password")!=null){
            String pw = (String) body.get("password");
            if(!pw.trim().isEmpty()) u.setPassword(passwordEncoder.encode(pw));
        }

        userRepository.save(u);
        Teacher saved = teacherRepository.save(t);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id){
        Optional<Teacher> opt = teacherRepository.findById(id);
        if(opt.isEmpty()) return ResponseEntity.notFound().build();
        Teacher t = opt.get();
        User u = t.getUser();
        teacherRepository.deleteById(id);
        if(u!=null && userRepository.existsById(u.getId())) userRepository.deleteById(u.getId());
        return ResponseEntity.noContent().build();
    }
}
