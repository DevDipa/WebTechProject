package com.TheTrio.SATracker.controller;

import com.TheTrio.SATracker.models.Batch;
import com.TheTrio.SATracker.repository.BatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/api/batches")
public class AdminBatchRestController {

    private final BatchRepository batchRepository;

    public AdminBatchRestController(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @GetMapping
    public List<Batch> list() {
        return batchRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Batch input) {
        if (input.getBatchName() == null || input.getBatchName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("batchName is required");
        }
        Batch b = new Batch();
        b.setBatchName(input.getBatchName().trim());
        b.setYear(input.getYear());
        Batch saved = batchRepository.save(b);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Batch input) {
        Optional<Batch> opt = batchRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Batch b = opt.get();
        if (input.getBatchName() != null && !input.getBatchName().trim().isEmpty()) {
            b.setBatchName(input.getBatchName().trim());
        }
        b.setYear(input.getYear());
        Batch saved = batchRepository.save(b);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        if (!batchRepository.existsById(id)) return ResponseEntity.notFound().build();
        batchRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
