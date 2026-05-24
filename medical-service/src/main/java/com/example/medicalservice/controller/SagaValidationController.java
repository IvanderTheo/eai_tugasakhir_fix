package com.example.medicalservice.controller;

import com.example.medicalservice.entity.Resep;
import com.example.medicalservice.service.ResepService;
import com.example.medicalservice.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/medical/saga-validation")
@Slf4j
@Tag(name = "Saga Validation", description = "End-to-end saga test scenarios (no JWT required)")
public class SagaValidationController {

    @Autowired
    private ResepService resepService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String PHARMACY_URL = "http://localhost:8004/api/pharmacy";
    private static final String PAYMENT_URL = "http://localhost:8003/api/payment";

    private String getMockToken() {
        return jwtUtil.generateTokenFromUsernameAndRoles("admin", List.of("ROLE_ADMIN", "ROLE_PHARMACIST", "ROLE_STAFF"));
    }

    private void ensureMedicineExists(String name, Integer stock, Double price) throws Exception {
        String token = getMockToken();
        
        // Search if exists
        HttpRequest searchReq = HttpRequest.newBuilder()
                .uri(URI.create(PHARMACY_URL + "/medicines/search?nama=" + name))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        
        HttpResponse<String> searchRes = httpClient.send(searchReq, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> body = objectMapper.readValue(searchRes.body(), Map.class);
        List<?> data = (List<?>) body.get("data");
        
        if (data != null && !data.isEmpty()) {
            // Already exists, update its stock directly
            Map<?, ?> obatMap = (Map<?, ?>) data.get(0);
            Long id = Long.valueOf(obatMap.get("id").toString());
            
            HttpRequest updateStockReq = HttpRequest.newBuilder()
                    .uri(URI.create(PHARMACY_URL + "/medicines/" + id + "/stock?stok=" + stock))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.send(updateStockReq, HttpResponse.BodyHandlers.ofString());
            log.info("Updated existing medicine: {} stock to: {}", name, stock);
        } else {
            // Create new medicine
            Map<String, Object> obat = new HashMap<>();
            obat.put("kodeObat", "KODE-" + name.toUpperCase().substring(0, Math.min(name.length(), 5)) + "-" + new Random().nextInt(1000));
            obat.put("namaObat", name);
            obat.put("harga", price);
            obat.put("satuan", "Tablet");
            obat.put("stok", stock);
            obat.put("stokMinimal", 2);
            obat.put("supplier", "Saga Test Lab");
            
            HttpRequest createReq = HttpRequest.newBuilder()
                    .uri(URI.create(PHARMACY_URL + "/medicines"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(obat)))
                    .build();
            httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());
            log.info("Created new medicine: {} with stock: {}", name, stock);
        }
    }

    private Map<String, Object> queryMedicine(String name) throws Exception {
        String token = getMockToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(PHARMACY_URL + "/medicines/search?nama=" + name))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> body = objectMapper.readValue(res.body(), Map.class);
        List<?> data = (List<?>) body.get("data");
        if (data != null && !data.isEmpty()) {
            return (Map<String, Object>) data.get(0);
        }
        return null;
    }

    private Map<String, Object> queryInvoiceByResepId(Long resepId) throws Exception {
        String token = getMockToken();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(PAYMENT_URL + "/invoices/prescription/" + resepId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 200) {
            Map<String, Object> body = objectMapper.readValue(res.body(), Map.class);
            return (Map<String, Object>) body.get("data");
        }
        return null;
    }

    @PostMapping("/success")
    public ResponseEntity<?> simulateSuccess() {
        List<String> auditLogs = new ArrayList<>();
        auditLogs.add("Starting Saga Simulation: Happy Path (Success)");
        try {
            // 1. Prepare Medicine with stock = 10
            ensureMedicineExists("SagaParacetamol", 10, 5000.0);
            auditLogs.add("Step 1: Ensured medicine 'SagaParacetamol' exists with stock=10, price=5000");

            // 2. Create prescription
            Resep resep = new Resep();
            resep.setPemeriksaanId(101L);
            resep.setPasienId(202L);
            resep.setNamaObat("SagaParacetamol");
            resep.setDosis("3x1");
            resep.setFrekuensi("Sehari 3 kali");
            resep.setJumlah(2); // needs 2, remaining should be 8
            resep.setCatatan("Take after meal");
            resep.setDokterNama("Dr. Saga Specialist");
            
            Resep saved = resepService.createResep(resep);
            auditLogs.add("Step 2: Saved prescription with ID: " + saved.getId() + " and status: PENDING. Event published.");

            // 3. Sleep to let Kafka process stock reservation and billing generation
            auditLogs.add("Waiting for Kafka to process stock reservation & billing creation...");
            Thread.sleep(2000);

            // Check stock and prescription status
            Map<String, Object> medState = queryMedicine("SagaParacetamol");
            Resep resepState = resepService.getResepById(saved.getId());
            Map<String, Object> invoiceState = queryInvoiceByResepId(saved.getId());

            auditLogs.add("Check 1: Prescription status in DB: " + resepState.getStatus() + " (Expected: RESERVED)");
            if (medState != null) {
                auditLogs.add("Check 2: Medicine stock in DB: " + medState.get("stok") + " (Expected: 8)");
            }
            if (invoiceState != null) {
                auditLogs.add("Check 3: Tagihan status in DB: " + invoiceState.get("status") + " (Expected: PENDING)");
            } else {
                throw new RuntimeException("Tagihan invoice was not generated!");
            }

            // 4. Process payment
            Long invoiceId = Long.valueOf(invoiceState.get("id").toString());
            String token = getMockToken();
            Map<String, Object> tx = new HashMap<>();
            tx.put("tagihanId", invoiceId);
            tx.put("jumlahBayar", 60000.0); // 50k consultation + 10k medicine
            tx.put("metodePembayaran", "E-Wallet");
            tx.put("keterangan", "Paid Saga Test");

            auditLogs.add("Step 3: Triggering payment processing for invoice ID: " + invoiceId);
            HttpRequest payReq = HttpRequest.newBuilder()
                    .uri(URI.create(PAYMENT_URL + "/transactions/process"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(tx)))
                    .build();
            HttpResponse<String> payRes = httpClient.send(payReq, HttpResponse.BodyHandlers.ofString());
            auditLogs.add("Payment API response code: " + payRes.statusCode());

            // 5. Sleep to let payment event propagate
            auditLogs.add("Waiting for Kafka to propagate payment completion...");
            Thread.sleep(2000);

            // 6. Verify final completed state
            medState = queryMedicine("SagaParacetamol");
            resepState = resepService.getResepById(saved.getId());
            invoiceState = queryInvoiceByResepId(saved.getId());

            auditLogs.add("Final Check: Prescription status: " + resepState.getStatus() + " (Expected: COMPLETED)");
            if (invoiceState != null) {
                auditLogs.add("Final Check: Tagihan status: " + invoiceState.get("status") + " (Expected: PAID)");
            }
            if (medState != null) {
                auditLogs.add("Final Check: Medicine stock: " + medState.get("stok") + " (Expected: 8)");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("auditLogs", auditLogs);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Simulation failed", e);
            auditLogs.add("ERROR: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "FAILED", "auditLogs", auditLogs));
        }
    }

    @PostMapping("/fail-payment")
    public ResponseEntity<?> simulateFailPayment() {
        List<String> auditLogs = new ArrayList<>();
        auditLogs.add("Starting Saga Simulation: Compensation Flow (Payment Cancelled)");
        try {
            // 1. Prepare Medicine with stock = 10
            ensureMedicineExists("SagaAmoxicillin", 10, 8000.0);
            auditLogs.add("Step 1: Ensured medicine 'SagaAmoxicillin' exists with stock=10, price=8000");

            // 2. Create prescription
            Resep resep = new Resep();
            resep.setPemeriksaanId(102L);
            resep.setPasienId(203L);
            resep.setNamaObat("SagaAmoxicillin");
            resep.setDosis("2x1");
            resep.setFrekuensi("Sehari 2 kali");
            resep.setJumlah(3); // needs 3, remaining should temporarily be 7
            resep.setCatatan("Take after meal");
            resep.setDokterNama("Dr. Saga Specialist");
            
            Resep saved = resepService.createResep(resep);
            auditLogs.add("Step 2: Saved prescription with ID: " + saved.getId() + " and status: PENDING. Event published.");

            // 3. Sleep to let Kafka process stock reservation and billing generation
            auditLogs.add("Waiting for Kafka to process stock reservation & billing creation...");
            Thread.sleep(2000);

            // Check stock and prescription status
            Map<String, Object> medState = queryMedicine("SagaAmoxicillin");
            Resep resepState = resepService.getResepById(saved.getId());
            Map<String, Object> invoiceState = queryInvoiceByResepId(saved.getId());

            auditLogs.add("Check 1: Prescription status in DB: " + resepState.getStatus() + " (Expected: RESERVED)");
            if (medState != null) {
                auditLogs.add("Check 2: Medicine stock in DB: " + medState.get("stok") + " (Expected: 7)");
            }
            if (invoiceState != null) {
                auditLogs.add("Check 3: Tagihan status in DB: " + invoiceState.get("status") + " (Expected: PENDING)");
            } else {
                throw new RuntimeException("Tagihan invoice was not generated!");
            }

            // 4. Cancel the invoice (reverts stock!)
            Long invoiceId = Long.valueOf(invoiceState.get("id").toString());
            String token = getMockToken();

            auditLogs.add("Step 3: Triggering cancellation (compensation) for invoice ID: " + invoiceId);
            HttpRequest cancelReq = HttpRequest.newBuilder()
                    .uri(URI.create(PAYMENT_URL + "/invoices/" + invoiceId + "/cancel"))
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> cancelRes = httpClient.send(cancelReq, HttpResponse.BodyHandlers.ofString());
            auditLogs.add("Cancel API response code: " + cancelRes.statusCode());

            // 5. Sleep to let compensation event propagate
            auditLogs.add("Waiting for Kafka to propagate compensation...");
            Thread.sleep(2000);

            // 6. Verify final compensated state
            medState = queryMedicine("SagaAmoxicillin");
            resepState = resepService.getResepById(saved.getId());
            invoiceState = queryInvoiceByResepId(saved.getId());

            auditLogs.add("Final Check: Prescription status: " + resepState.getStatus() + " (Expected: CANCELLED)");
            if (invoiceState != null) {
                auditLogs.add("Final Check: Tagihan status: " + invoiceState.get("status") + " (Expected: CANCELLED)");
            }
            if (medState != null) {
                auditLogs.add("Final Check: Medicine stock: " + medState.get("stok") + " (Expected: 10 - Reverted!)");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("auditLogs", auditLogs);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Simulation failed", e);
            auditLogs.add("ERROR: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "FAILED", "auditLogs", auditLogs));
        }
    }

    @PostMapping("/fail-stock")
    public ResponseEntity<?> simulateFailStock() {
        List<String> auditLogs = new ArrayList<>();
        auditLogs.add("Starting Saga Simulation: Stock Failure Flow (Out of Stock)");
        try {
            // 1. Prepare Medicine with stock = 0
            ensureMedicineExists("SagaEmptyDrug", 0, 10000.0);
            auditLogs.add("Step 1: Ensured medicine 'SagaEmptyDrug' exists with stock=0, price=10000");

            // 2. Create prescription
            Resep resep = new Resep();
            resep.setPemeriksaanId(103L);
            resep.setPasienId(204L);
            resep.setNamaObat("SagaEmptyDrug");
            resep.setDosis("1x1");
            resep.setFrekuensi("Sehari 1 kali");
            resep.setJumlah(1); // needs 1, but stock is 0
            resep.setCatatan("Take after meal");
            resep.setDokterNama("Dr. Saga Specialist");
            
            Resep saved = resepService.createResep(resep);
            auditLogs.add("Step 2: Saved prescription with ID: " + saved.getId() + " and status: PENDING. Event published.");

            // 3. Sleep to let Kafka process stock reservation
            auditLogs.add("Waiting for Kafka to process stock check...");
            Thread.sleep(2000);

            // Check stock and prescription status
            Map<String, Object> medState = queryMedicine("SagaEmptyDrug");
            Resep resepState = resepService.getResepById(saved.getId());
            Map<String, Object> invoiceState = queryInvoiceByResepId(saved.getId());

            auditLogs.add("Final Check: Prescription status: " + resepState.getStatus() + " (Expected: FAILED_OUT_OF_STOCK)");
            if (medState != null) {
                auditLogs.add("Final Check: Medicine stock: " + medState.get("stok") + " (Expected: 0)");
            }
            auditLogs.add("Final Check: Tagihan invoice exists: " + (invoiceState != null ? "YES (Error!)" : "NO (Expected)"));

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("auditLogs", auditLogs);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Simulation failed", e);
            auditLogs.add("ERROR: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("status", "FAILED", "auditLogs", auditLogs));
        }
    }
}
