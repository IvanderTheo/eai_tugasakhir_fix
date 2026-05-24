package com.example.paymentservice.controller;

import com.example.paymentservice.entity.Tagihan;
import com.example.paymentservice.service.TagihanService;
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
@RequestMapping("/api/payment/invoices")
@Slf4j
@Tag(name = "Invoices", description = "Tagihan / billing (JSON body)")
public class TagihanController {

    @Autowired
    private TagihanService tagihanService;

    @PostMapping
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> createTagihan(@RequestBody Tagihan tagihan) {
        try {
            Tagihan created = tagihanService.createTagihan(tagihan);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Invoice created successfully");
            response.put("data", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating invoice: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> getTagihan(@PathVariable Long id) {
        try {
            Tagihan tagihan = tagihanService.getTagihanById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("data", tagihan);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving invoice: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/patient/{pasienId}")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> getByPasienId(@PathVariable Long pasienId) {
        try {
            List<Tagihan> invoices = tagihanService.getTagihanByPasienId(pasienId);
            Map<String, Object> response = new HashMap<>();
            response.put("data", invoices);
            response.put("count", invoices.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving patient invoices: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> getPendingInvoices() {
        try {
            List<Tagihan> invoices = tagihanService.getPendingTagihan();
            Map<String, Object> response = new HashMap<>();
            response.put("data", invoices);
            response.put("count", invoices.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving pending invoices: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/prescription/{resepId}")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> getByResepId(@PathVariable Long resepId) {
        try {
            java.util.Optional<Tagihan> optional = tagihanService.getTagihanByResepId(resepId);
            if (optional.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invoice not found for prescription ID " + resepId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("data", optional.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving prescription invoice: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            Tagihan updated = tagihanService.updateTagihanStatus(id, status);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Invoice status updated successfully");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating invoice status: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> cancelTagihan(@PathVariable Long id) {
        try {
            Tagihan cancelled = tagihanService.cancelTagihan(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Invoice cancelled successfully");
            response.put("data", cancelled);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cancelling invoice: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

