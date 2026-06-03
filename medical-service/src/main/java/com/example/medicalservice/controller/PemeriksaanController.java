package com.example.medicalservice.controller;

import com.example.medicalservice.entity.Pemeriksaan;
import com.example.medicalservice.service.PemeriksaanService;
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
@RequestMapping("/api/medical/examinations")
@Slf4j
@Tag(name = "Medical Examinations", description = "Pemeriksaan pasien (JSON body)")
public class PemeriksaanController {

    @Autowired
    private PemeriksaanService pemeriksaanService;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> createPemeriksaan(@RequestBody Pemeriksaan pemeriksaan) {
        try {
            Pemeriksaan created = pemeriksaanService.createPemeriksaan(pemeriksaan);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Medical examination created successfully");
            response.put("data", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating examination: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPemeriksaan(@PathVariable Long id) {
        try {
            Pemeriksaan pemeriksaan = pemeriksaanService.getPemeriksaanById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("data", pemeriksaan);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving examination: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/patient/{pasienId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getByPasienId(@PathVariable Long pasienId) {
        try {
            List<Pemeriksaan> examinations = pemeriksaanService.getPemeriksaanByPasienId(pasienId);
            Map<String, Object> response = new HashMap<>();
            response.put("data", examinations);
            response.put("count", examinations.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving patient examinations: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updatePemeriksaan(@PathVariable Long id, @RequestBody Pemeriksaan pemeriksaan) {
        try {
            Pemeriksaan updated = pemeriksaanService.updatePemeriksaan(id, pemeriksaan);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Medical examination updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating examination: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

