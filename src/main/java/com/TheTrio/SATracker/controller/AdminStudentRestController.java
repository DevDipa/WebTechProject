package com.TheTrio.SATracker.controller;

import com.TheTrio.SATracker.models.Batch;
import com.TheTrio.SATracker.models.Student;
import com.TheTrio.SATracker.models.User;
import com.TheTrio.SATracker.repository.BatchRepository;
import com.TheTrio.SATracker.repository.StudentRepository;
import com.TheTrio.SATracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/api/students")
public class AdminStudentRestController {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminStudentRestController(StudentRepository studentRepository, UserRepository userRepository, BatchRepository batchRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
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
    public List<java.util.Map<String,Object>> list(){
        List<Student> all = studentRepository.findAll();
        List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
        for(Student s : all){
            java.util.Map<String,Object> m = new java.util.HashMap<>();
            m.put("id", s.getId());
            // user summary
            java.util.Map<String,Object> u = new java.util.HashMap<>();
            if(s.getUser()!=null){
                u.put("username", s.getUser().getUsername());
                u.put("fullName", s.getUser().getFullName());
                u.put("email", s.getUser().getEmail());
            }
            m.put("user", u);
            // batch summary
            java.util.Map<String,Object> b = new java.util.HashMap<>();
            if(s.getBatch()!=null){
                b.put("id", s.getBatch().getId());
                b.put("batchName", s.getBatch().getBatchName());
                b.put("year", s.getBatch().getYear());
            }
            m.put("batch", b);
            // other fields
            m.put("dob", s.getDob()!=null? s.getDob().toString(): null);
            m.put("phone", s.getPhone());
            m.put("address", s.getAddress());
            m.put("bloodGroup", s.getBloodGroup());
            out.add(m);
        }
        return out;
    }

    // DTO expected: { username, password, fullName, email, dob, phone, address, bloodGroup, batchId }
    @PostMapping
    public ResponseEntity<?> create(@RequestBody java.util.Map<String,Object> body){
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String fullName = (String) body.get("fullName");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        String address = (String) body.get("address");
        String bloodGroup = (String) body.get("bloodGroup");
        String dobStr = (String) body.get("dob");
        Integer batchId = parseInteger(body.get("batchId"));

        if(username==null || username.trim().isEmpty()) return ResponseEntity.badRequest().body("username required");
        if(password==null || password.trim().isEmpty()) return ResponseEntity.badRequest().body("password required");

        if(userRepository.findByUsername(username).isPresent()) return ResponseEntity.status(HttpStatus.CONFLICT).body("username exists");

        User u = new User();
        u.setUsername(username.trim());
        u.setPassword(passwordEncoder.encode(password));
        u.setFullName(fullName);
        u.setEmail(email);
        u.setRole(User.Role.STUDENT);
        User savedUser = userRepository.save(u);

        Student s = new Student();
        s.setUser(savedUser);
        s.setPhone(phone);
        s.setAddress(address);
        s.setBloodGroup(bloodGroup);
        if(dobStr!=null && !dobStr.trim().isEmpty()){
            try{ s.setDob(LocalDate.parse(dobStr)); }catch(Exception ex){ /* ignore invalid date */ }
        }
        if(batchId!=null){
            Optional<Batch> b = batchRepository.findById(batchId);
            b.ifPresent(s::setBatch);
        }
        Student saved = studentRepository.save(s);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody java.util.Map<String,Object> body){
        Optional<Student> opt = studentRepository.findById(id);
        if(opt.isEmpty()) return ResponseEntity.notFound().build();
        Student s = opt.get();

        if(body.containsKey("phone")) s.setPhone((String) body.get("phone"));
        if(body.containsKey("address")) s.setAddress((String) body.get("address"));
        if(body.containsKey("bloodGroup")) s.setBloodGroup((String) body.get("bloodGroup"));
        if(body.containsKey("dob")){
            String ds = (String) body.get("dob");
            if(ds==null || ds.trim().isEmpty()) s.setDob(null);
            else {
                try{ s.setDob(LocalDate.parse(ds)); }catch(Exception ex){ /* ignore parse errors */ }
            }
        }
        if(body.containsKey("batchId")){
            Object bi = body.get("batchId");
            Integer batchId = parseInteger(bi);
            if(batchId==null) s.setBatch(null);
            else batchRepository.findById(batchId).ifPresent(s::setBatch);
        }

        User u = s.getUser();
        if(body.containsKey("fullName")) u.setFullName((String) body.get("fullName"));
        if(body.containsKey("email")) u.setEmail((String) body.get("email"));
        if(body.containsKey("password") && body.get("password")!=null){
            String pw = (String) body.get("password");
            if(!pw.trim().isEmpty()) u.setPassword(passwordEncoder.encode(pw));
        }

        userRepository.save(u);
        Student saved = studentRepository.save(s);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id){
        Optional<Student> opt = studentRepository.findById(id);
        if(opt.isEmpty()) return ResponseEntity.notFound().build();
        Student s = opt.get();
        User u = s.getUser();
        studentRepository.deleteById(id);
        if(u!=null && userRepository.existsById(u.getId())) userRepository.deleteById(u.getId());
        return ResponseEntity.noContent().build();
    }
}
