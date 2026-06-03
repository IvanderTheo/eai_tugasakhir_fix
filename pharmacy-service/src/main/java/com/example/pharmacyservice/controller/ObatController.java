package com.example.pharmacyservice.controller;

import com.example.pharmacyservice.entity.Obat;
import com.example.pharmacyservice.service.ObatService;
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
@RequestMapping("/api/pharmacy/medicines")
@Slf4j
@Tag(name = "Medicines", description = "Inventory & stock (JSON body)")
public class ObatController {

    @Autowired
    private ObatService obatService;

    @PostMapping
    @PreAuthorize("hasRole('PHARMACIST') or hasRole('ADMIN')")
    public ResponseEntity<?> createObat(@RequestBody Obat obat) {
        try {
            Obat created = obatService.createObat(obat);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Medicine created successfully");
            response.put("data", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating medicine: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getObat(@PathVariable Long id) {
        try {
            Obat obat = obatService.getObatById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("data", obat);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving medicine: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllObat() {
        try {
            List<Obat> medicines = obatService.getAllObat();
            Map<String, Object> response = new HashMap<>();
            response.put("data", medicines);
            response.put("count", medicines.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving medicines: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchObat(@RequestParam String nama) {
        try {
            List<Obat> medicines = obatService.searchObat(nama);
            Map<String, Object> response = new HashMap<>();
            response.put("data", medicines);
            response.put("count", medicines.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching medicines: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/low-stock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getLowStock() {
        try {
            List<Obat> medicines = obatService.getLowStockMedicines();
            Map<String, Object> response = new HashMap<>();
            response.put("data", medicines);
            response.put("count", medicines.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving low stock medicines: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestParam Integer stok) {
        try {
            Obat updated = obatService.updateStok(id, stok);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Stock updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating stock: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

