package com.example.adminservice.controller;

import com.example.adminservice.dto.PasienDTO;
import com.example.adminservice.service.PasienService;
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
@RequestMapping("/api/patients")
@Slf4j
@Tag(name = "Patients", description = "Patient CRUD (JSON body, requires JWT)")
public class PasienController {

    @Autowired
    private PasienService pasienService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('STAFF')")
    public ResponseEntity<?> createPasien(@RequestBody PasienDTO pasienDTO) {
        try {
            PasienDTO created = pasienService.createPasien(pasienDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Patient created successfully");
            response.put("data", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating patient: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('STAFF')")
    public ResponseEntity<?> getPasienById(@PathVariable Long id) {
        try {
            PasienDTO pasien = pasienService.getPasienById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("data", pasien);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving patient: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('STAFF')")
    public ResponseEntity<?> getAllPasien() {
        try {
            List<PasienDTO> patients = pasienService.getAllPasien();
            Map<String, Object> response = new HashMap<>();
            response.put("data", patients);
            response.put("count", patients.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving patients: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('STAFF')")
    public ResponseEntity<?> searchPasien(@RequestParam String nama) {
        try {
            List<PasienDTO> patients = pasienService.searchPasienByNama(nama);
            Map<String, Object> response = new HashMap<>();
            response.put("data", patients);
            response.put("count", patients.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching patients: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('STAFF')")
    public ResponseEntity<?> updatePasien(@PathVariable Long id, @RequestBody PasienDTO pasienDTO) {
        try {
            PasienDTO updated = pasienService.updatePasien(id, pasienDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Patient updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating patient: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePasien(@PathVariable Long id) {
        try {
            pasienService.deletePasien(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Patient deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting patient: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

