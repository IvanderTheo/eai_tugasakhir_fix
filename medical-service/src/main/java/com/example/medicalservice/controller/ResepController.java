package com.example.medicalservice.controller;

import com.example.medicalservice.entity.Resep;
import com.example.medicalservice.service.ResepService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medical/prescriptions")
@Slf4j
@Tag(name = "Prescriptions", description = "E-resep (JSON body, triggers Kafka saga)")
public class ResepController {

    @Autowired
    private ResepService resepService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> createResep(@RequestBody Resep resep) {
        try {
            Resep created = resepService.createResep(resep);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Prescription created successfully");
            response.put("data", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating prescription: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getResep(@PathVariable Long id) {
        try {
            Resep resep = resepService.getResepById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("data", resep);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving prescription: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/patient/{pasienId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getByPasienId(@PathVariable Long pasienId) {
        try {
            List<Resep> prescriptions = resepService.getResepByPasienId(pasienId);
            Map<String, Object> response = new HashMap<>();
            response.put("data", prescriptions);
            response.put("count", prescriptions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving patient prescriptions: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPendingPrescriptions() {
        try {
            List<Resep> prescriptions = resepService.getPendingResep();
            Map<String, Object> response = new HashMap<>();
            response.put("data", prescriptions);
            response.put("count", prescriptions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving pending prescriptions: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            Resep updated = resepService.updateResepStatus(id, status);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Prescription status updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating prescription status: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

