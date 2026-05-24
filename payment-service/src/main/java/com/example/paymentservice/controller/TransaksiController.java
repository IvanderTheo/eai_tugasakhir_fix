package com.example.paymentservice.controller;

import com.example.paymentservice.entity.Transaksi;
import com.example.paymentservice.service.TransaksiService;
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
@RequestMapping("/api/payment/transactions")
@Slf4j
@Tag(name = "Transactions", description = "Payment processing (JSON body)")
public class TransaksiController {

    @Autowired
    private TransaksiService transaksiService;

    @PostMapping("/process")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> processPayment(@RequestBody Transaksi transaksi) {
        try {
            Transaksi created = transaksiService.processPayment(transaksi);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Payment processed successfully");
            response.put("data", created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> getTransaksi(@PathVariable Long id) {
        try {
            Transaksi transaksi = transaksiService.getTransaksiById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("data", transaksi);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving transaction: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/invoice/{tagihanId}")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> getByTagihanId(@PathVariable Long tagihanId) {
        try {
            List<Transaksi> transactions = transaksiService.getTransaksiByTagihanId(tagihanId);
            Map<String, Object> response = new HashMap<>();
            response.put("data", transactions);
            response.put("count", transactions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving invoice transactions: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

