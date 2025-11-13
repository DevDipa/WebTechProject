package com.TheTrio.SATracker.controller;

import com.TheTrio.SATracker.models.Holiday;
import com.TheTrio.SATracker.repository.HolidayRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/api/holidays")
public class AdminHolidayRestController {

    private final HolidayRepository holidayRepository;

    public AdminHolidayRestController(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @GetMapping
    public List<Holiday> list() {
        return holidayRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Holiday input){
        if(input.getHolidayName()==null || input.getHolidayName().trim().isEmpty()){
            return ResponseEntity.badRequest().body("holidayName is required");
        }
        if(input.getDate()==null){
            return ResponseEntity.badRequest().body("date is required");
        }
        Holiday h = new Holiday();
        h.setHolidayName(input.getHolidayName().trim());
        h.setDate(input.getDate());
        Holiday saved = holidayRepository.save(h);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Holiday input){
        Optional<Holiday> opt = holidayRepository.findById(id);
        if(opt.isEmpty()) return ResponseEntity.notFound().build();
        Holiday h = opt.get();
        if(input.getHolidayName()!=null && !input.getHolidayName().trim().isEmpty()) h.setHolidayName(input.getHolidayName().trim());
        if(input.getDate()!=null) h.setDate(input.getDate());
        Holiday saved = holidayRepository.save(h);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id){
        if(!holidayRepository.existsById(id)) return ResponseEntity.notFound().build();
        holidayRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
