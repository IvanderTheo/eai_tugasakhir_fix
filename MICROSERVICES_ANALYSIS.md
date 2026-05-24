# Microservices Architecture Analysis - GrowBusiness

**Date**: May 2026  
**Project**: RSU Sumatera - Hospital Information System  
**Analysis Scope**: Saga Pattern, API Gateway, Service Communication, Error Handling

---

## Executive Summary

| Aspect | Status | Details |
|--------|--------|---------|
| **Saga Pattern** | ⚠️ Partially Implemented | Event-driven choreography with compensation logic |
| **API Gateway** | ✅ Fully Implemented | Spring Cloud Gateway with JWT authentication |
| **Service Communication** | ✅ Event-Driven + REST | Kafka for async, HTTP for testing/queries |
| **Error Handling** | ⚠️ Basic Implementation | Try-catch blocks, limited centralized handling |
| **Compensation Logic** | ✅ Partially Implemented | Stock reversal in pharmacy service on payment failure |

---

## 1. SAGA PATTERN IMPLEMENTATION STATUS

### Current Implementation Level: **PARTIALLY IMPLEMENTED (Event-Driven Choreography)**

#### 1.1 Saga Architecture Type
- **Type**: **Event-Driven Choreography** (NOT Orchestration)
- **Pattern**: Services publish events and listen to events
- **Broker**: Apache Kafka 3.8.0
- **Trigger**: Prescription creation initiates the saga flow

#### 1.2 Saga Flow Diagram

```
Prescription Created (Medical Service)
    ↓
    └─→ [Event] prescription-created (Kafka)
        ↓
        ├─→ Pharmacy Service Listener
        │   ├─ Check stock availability
        │   ├─ If sufficient: Reserve stock, emit STOCK_RESERVED event
        │   └─ If insufficient: Emit STOCK_FAILED event
        │
        └─→ [Event] prescription-received (Kafka)
            ↓
            ├─→ Payment Service Listener
            │   ├─ If STOCK_RESERVED: Create invoice (Tagihan)
            │   └─ If STOCK_FAILED: Stop processing
            │
            └─→ [Event] payment-processed (Kafka)
                ↓
                ├─→ Pharmacy Service: Finalize or Compensate
                └─→ Medical Service: Update prescription status
```

#### 1.3 Saga Participants

| Service | Topics Listened | Topics Published | Role |
|---------|-----------------|------------------|------|
| **Medical** | `prescription-received`, `payment-processed` | `prescription-created` | Initiator + Finalizer |
| **Pharmacy** | `prescription-created`, `payment-processed` | `prescription-received` | Stock Manager |
| **Payment** | `prescription-received` | `payment-processed` | Billing Manager |

#### 1.4 Saga Listeners Implementation

##### Pharmacy Service (`SagaPharmacyListener.java`)
```java
@KafkaListener(topics = "prescription-created")
- Checks if medicine stock >= requested quantity
- If YES: Deducts stock and publishes STOCK_RESERVED event
- If NO: Publishes STOCK_FAILED event

@KafkaListener(topics = "payment-processed")
- If status = "FAILED": COMPENSATION LOGIC EXECUTES
  ✓ Restores stock to original amount
  ✓ Logs compensation event
- If status = "SUCCESS": Finalizes stock reservation
```

**Code Location**: [pharmacy-service/src/main/java/com/example/pharmacyservice/messaging/SagaPharmacyListener.java](pharmacy-service/src/main/java/com/example/pharmacyservice/messaging/SagaPharmacyListener.java)

##### Payment Service (`SagaPaymentListener.java`)
```java
@KafkaListener(topics = "prescription-received")
- If status = "STOCK_RESERVED": Creates invoice (Tagihan)
- Sets invoice status = PENDING
- Waits for payment processing
```

**Code Location**: [payment-service/src/main/java/com/example/paymentservice/messaging/SagaPaymentListener.java](payment-service/src/main/java/com/example/paymentservice/messaging/SagaPaymentListener.java)

##### Medical Service (`SagaMedicalListener.java`)
```java
@KafkaListener(topics = "prescription-received")
- If STOCK_RESERVED: Updates prescription status to RESERVED
- If STOCK_FAILED: Updates prescription status to FAILED_OUT_OF_STOCK

@KafkaListener(topics = "payment-processed")
- If SUCCESS: Updates prescription status to COMPLETED
- If FAILED: Updates prescription status to CANCELLED
```

**Code Location**: [medical-service/src/main/java/com/example/medicalservice/messaging/SagaMedicalListener.java](medical-service/src/main/java/com/example/medicalservice/messaging/SagaMedicalListener.java)

#### 1.5 Saga Transaction Steps

##### Happy Path (Success Flow):
1. **Step 1**: Doctor creates prescription (Medical Service)
   - Persists `Resep` with status = PENDING
   - Publishes `prescription-created` event
   
2. **Step 2**: Pharmacy reserves stock (Pharmacy Service)
   - Checks `Obat.stok >= jumlah`
   - Deducts stock: `stok = stok - jumlah`
   - Publishes `prescription-received` with status = STOCK_RESERVED
   
3. **Step 3**: Invoice generated (Payment Service)
   - Creates `Tagihan` (invoice)
   - Calculates total: consultation fee + medication cost + tax
   - Status = PENDING
   
4. **Step 4**: Medical Service updates prescription
   - Status updated to RESERVED
   
5. **Step 5**: Patient pays (Payment Service - via REST API)
   - Processes transaction
   - Updates `Tagihan.status = PAID`
   - Publishes `payment-processed` with status = SUCCESS
   
6. **Step 6**: Compensation/Finalization
   - Pharmacy: Finalizes stock (no revert)
   - Medical: Updates status to COMPLETED

##### Failure Path (Stock Insufficient):
1. Pharmacy checks stock, finds insufficient
2. Publishes `prescription-received` with status = STOCK_FAILED
3. Medical Service updates `Resep.status = FAILED_OUT_OF_STOCK`
4. No invoice created (transaction stops)
5. No compensation needed (stock never deducted)

##### Compensation Path (Payment Cancelled):
1. Patient/Admin cancels invoice via REST: `POST /api/payment/invoices/{id}/cancel`
2. Payment Service sets `Tagihan.status = CANCELLED`
3. Publishes `payment-processed` with status = FAILED
4. **Pharmacy Service COMPENSATES**: Restores stock
   - `stok = stok + jumlah` (reverses deduction)
   - Logs: "COMPENSATION EXECUTION: Reverted stock..."
5. Medical Service updates `Resep.status = CANCELLED`

#### 1.6 Event Topics Configuration

**Defined in Medical Service** (`medical-service/src/main/java/com/example/medicalservice/config/KafkaConfig.java`):

```
PRESCRIPTION_CREATED_TOPIC = "prescription-created"
PRESCRIPTION_RECEIVED_TOPIC = "prescription-received"
BILLING_CREATED_TOPIC = "billing-created"
PAYMENT_PROCESSED_TOPIC = "payment-processed"
STOCK_UPDATED_TOPIC = "stock-updated" (unused)
STOCK_LOW_ALERT_TOPIC = "stock-low-alert" (unused)
```

---

## 2. API GATEWAY IMPLEMENTATION

### Implementation Level: **✅ FULLY IMPLEMENTED**

#### 2.1 API Gateway Architecture

**Gateway Service**: Spring Cloud Gateway MVC (Port 8000)  
**Location**: [api-gateway/src/main/java/com/example/apigateway/](api-gateway/src/main/java/com/example/apigateway/)

#### 2.2 Gateway Routing Configuration

**File**: [api-gateway/src/main/resources/application.properties](api-gateway/src/main/resources/application.properties)

```
Route 0: /api/auth/** → http://localhost:8001 (Admin Service)
Route 1: /api/admin/** → http://localhost:8001 (Admin Service)
         - RewritePath: /api/admin/(?<segment>.*) → /api/${segment}

Route 2: /api/medical/** → http://localhost:8002 (Medical Service)
         - RewritePath: /api/medical/(?<segment>.*) → /api/medical/${segment}

Route 3: /api/pharmacy/** → http://localhost:8004 (Pharmacy Service)
         - RewritePath: /api/pharmacy/(?<segment>.*) → /api/pharmacy/${segment}

Route 4: /api/payment/** → http://localhost:8003 (Payment Service)
         - RewritePath: /api/payment/(?<segment>.*) → /api/payment/${segment}
```

#### 2.3 Gateway Features

| Feature | Status | Details |
|---------|--------|---------|
| **Path-based Routing** | ✅ Yes | Predicates based on path patterns |
| **Rewrite Paths** | ✅ Yes | Removes gateway prefix before forwarding |
| **JWT Auth** | ✅ Yes | `JwtAuthenticationFilter` validates tokens |
| **Security** | ✅ Yes | Spring Security + JWT roles |
| **Load Balancing** | ❌ No | Single backend per service |
| **Rate Limiting** | ❌ No | Not implemented |
| **Circuit Breaker** | ❌ No | Not implemented |
| **Retry Logic** | ❌ No | Not implemented |

#### 2.4 JWT Authentication Flow

1. **Login**: `POST /api/auth/login` → Admin Service returns JWT token
2. **Token Format**: JWT with payload containing `username` and `roles` (ROLE_ADMIN, ROLE_PHARMACIST, ROLE_STAFF)
3. **Header**: Requests include `Authorization: Bearer {token}`
4. **Validation**: `JwtAuthenticationFilter` intercepts all requests and validates token
5. **Role-based Access**: Services use `@PreAuthorize` to check roles

#### 2.5 Gateway Security Configuration

**File**: [api-gateway/src/main/java/com/example/apigateway/security/SecurityConfig.java](api-gateway/src/main/java/com/example/apigateway/security/SecurityConfig.java)

- Permits `/api/auth/login` and `/api/auth/register` (no auth required)
- All other routes require valid JWT token
- CORS configured for cross-origin requests

#### 2.6 Service Discovery

**Current Approach**: **Hardcoded URLs**
- No service registry (Eureka/Consul)
- Gateway maintains explicit service mappings
- Suitable for development, not production

---

## 3. SERVICE COMMUNICATION PATTERNS

### Communication Methods: **Hybrid (Event-Driven + REST)**

#### 3.1 Event-Driven Communication (Kafka)

**Topics & Flow**:

```
Topic: prescription-created
├─ Published by: Medical Service (ResepService.createResep)
├─ Consumed by: Pharmacy Service (SagaPharmacyListener)
└─ Payload: {id, pasienId, namaObat, jumlah, timestamp}

Topic: prescription-received
├─ Published by: Pharmacy Service (SagaPharmacyListener)
├─ Consumed by: Payment Service (SagaPaymentListener)
│                Medical Service (SagaMedicalListener)
└─ Payload: {prescriptionId, pasienId, namaObat, jumlah, status, hargaObat}

Topic: payment-processed
├─ Published by: Payment Service (TransaksiService.processPayment)
│                             (TagihanService.cancelTagihan)
├─ Consumed by: Pharmacy Service (SagaPharmacyListener - COMPENSATION)
│                Medical Service (SagaMedicalListener)
└─ Payload: {resepId, pasienId, namaObat, jumlah, status, timestamp}
```

**Advantages**:
- ✅ Decoupled services
- ✅ Asynchronous processing
- ✅ Natural for compensation flows
- ✅ Audit trail (Kafka topics)

**Disadvantages**:
- ⚠️ Eventual consistency (not immediate)
- ⚠️ Harder to debug distributed flows
- ⚠️ Requires Kafka infrastructure

#### 3.2 Direct HTTP/REST Communication

**Used in Test Endpoints**: [medical-service/src/main/java/com/example/medicalservice/controller/SagaValidationController.java](medical-service/src/main/java/com/example/medicalservice/controller/SagaValidationController.java)

**Purpose**: Saga simulation and end-to-end testing

```java
- HTTP GET to Pharmacy: /api/pharmacy/medicines/search?nama={name}
- HTTP GET to Payment: /api/payment/invoices/prescription/{resepId}
- HTTP POST to Payment: /api/payment/transactions/process
- HTTP POST to Payment: /api/payment/invoices/{id}/cancel
```

**Characteristics**:
- Synchronous calls for testing/validation
- Uses `java.net.http.HttpClient` for requests
- Mock JWT tokens for testing

#### 3.3 Service Interaction Matrix

| From Service | To Service | Method | Topics | Purpose |
|--------------|-----------|--------|--------|---------|
| Medical | Pharmacy | Kafka | prescription-created | Notify stock reservation |
| Pharmacy | Medical | Kafka | prescription-received | Update reservation status |
| Pharmacy | Payment | Kafka | prescription-received | Trigger billing |
| Payment | Pharmacy | Kafka | payment-processed | Trigger finalization/compensation |
| Payment | Medical | Kafka | payment-processed | Update completion status |
| Medical | Pharmacy | HTTP | Direct | Test/Query operations |
| Medical | Payment | HTTP | Direct | Test/Query operations |

---

## 4. ERROR HANDLING & COMPENSATION LOGIC

### Current Status: **⚠️ BASIC IMPLEMENTATION**

#### 4.1 Error Handling Approach

| Layer | Method | Coverage |
|-------|--------|----------|
| **Controller** | Try-catch blocks | Partial (basic exceptions) |
| **Service** | Try-catch + logging | Partial (transactional consistency) |
| **Kafka Listeners** | Try-catch + ack() | Good (event handling) |
| **Global Exception Handler** | None | ❌ Missing |
| **Circuit Breaker** | Not implemented | ❌ Missing |
| **Timeout Handling** | Not implemented | ❌ Missing |

#### 4.2 Compensation Logic Implementation

##### Pharmacy Service Compensation

**Trigger**: `payment-processed` event with `status = "FAILED"`

**Code Location**: [pharmacy-service/src/main/java/com/example/pharmacyservice/messaging/SagaPharmacyListener.java#L95-L115](pharmacy-service/src/main/java/com/example/pharmacyservice/messaging/SagaPharmacyListener.java#L95-L115)

```java
@KafkaListener(topics = "payment-processed")
@Transactional
public void listenPaymentProcessed(Map<String, Object> event, Acknowledgment ack) {
    String status = (String) event.get("status");
    
    if ("FAILED".equals(status)) {
        // COMPENSATION: Restore stock
        Optional<Obat> optionalObat = obatRepository.findByNamaObat(namaObat);
        if (optionalObat.isPresent()) {
            Obat obat = optionalObat.get();
            obat.setStok(obat.getStok() + jumlah);  // ← ADD BACK TO STOCK
            obatRepository.save(obat);
            log.info("COMPENSATION EXECUTION: Reverted stock for prescriptionId: {}, 
                     medicine: {}, new stock: {}", 
                     prescriptionId, namaObat, obat.getStok());
        }
    }
}
```

**What Gets Compensated**:
- ✅ Stock reservation (reversed to original)
- ✅ Prescription status (set to CANCELLED)
- ✅ Invoice status (set to CANCELLED)

**What's NOT Compensated**:
- ❌ Log entries/audit trail (not rolled back)
- ❌ Ledger entries (no financial ledger in design)
- ❌ Notifications (no rollback of notifications)

#### 4.3 Payment Cancellation Flow

**Endpoint**: `POST /api/payment/invoices/{id}/cancel`

**Implemented in**: [payment-service/src/main/java/com/example/paymentservice/service/TagihanService.java#L100-L120](payment-service/src/main/java/com/example/paymentservice/service/TagihanService.java#L100-L120)

```java
@Transactional
public Tagihan cancelTagihan(Long id) {
    Tagihan tagihan = tagihanRepository.findById(id).get();
    tagihan.setStatus("CANCELLED");
    Tagihan saved = tagihanRepository.save(tagihan);
    
    // Publish compensation event
    Map<String, Object> event = new HashMap<>();
    event.put("status", "FAILED");
    event.put("resepId", saved.getResepId());
    event.put("namaObat", saved.getNamaObat());
    event.put("jumlah", saved.getJumlahObat());
    
    kafkaTemplate.send(KafkaConfig.PAYMENT_PROCESSED_TOPIC, event);
    return saved;
}
```

#### 4.4 Medical Service Status Tracking

**Prescription Status Transitions**:

```
PENDING 
  ├─→ RESERVED (stock confirmed)
  ├─→ FAILED_OUT_OF_STOCK (stock unavailable)
  ├─→ COMPLETED (payment successful)
  └─→ CANCELLED (payment failed/cancelled)
```

**Code Location**: [medical-service/src/main/java/com/example/medicalservice/messaging/SagaMedicalListener.java](medical-service/src/main/java/com/example/medicalservice/messaging/SagaMedicalListener.java)

#### 4.5 Transaction Processing with Validation

**Payment Service** (`TransaksiService.processPayment`):

```java
// Validate payment amount
if (transaksi.getJumlahBayar() < tagihan.getTotalBayar()) {
    throw new RuntimeException("Payment amount is insufficient");
}

// Process payment
transaksi.setStatusPembayaran("COMPLETED");
saved = transaksiRepository.save(transaksi);

// Update invoice status
tagihan.setStatus("PAID");
tagihanRepository.save(tagihan);

// Publish success event (triggers finalization)
kafkaTemplate.send(PAYMENT_PROCESSED_TOPIC, event);
```

---

## 5. DEPENDENCIES & SAGA LIBRARIES

### 5.1 Kafka & Messaging Dependencies

**Spring Kafka** (All services):
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**Status**: ✅ Correctly configured for event-driven architecture

### 5.2 Saga Libraries

| Library | Status | Reason |
|---------|--------|--------|
| **Axon Framework** | ❌ Not Used | Custom choreography implementation |
| **Temporal** | ❌ Not Used | Overkill for current scale |
| **Spring Cloud Config** | ❌ Not Used | Hardcoded URLs sufficient |
| **Spring Cloud Consul** | ❌ Not Used | No service discovery needed yet |
| **Spring Retry** | ❌ Not Used | Manual retry in Kafka listeners |

**Conclusion**: No external saga framework; built with vanilla Spring Kafka + custom business logic.

---

## 6. WHAT'S IMPLEMENTED

### ✅ Fully Implemented

1. **Event-Driven Saga (Choreography)**
   - Prescription creation triggers multi-step flow
   - Stock reservation before billing
   - Compensation on payment failure

2. **API Gateway with JWT**
   - Path-based routing to 4 microservices
   - JWT token validation on all requests
   - Role-based access control

3. **Asynchronous Communication**
   - Kafka topics for events
   - Listener-based consumption
   - Manual acknowledgment handling

4. **Stock Management Saga**
   - Stock deduction on reservation
   - Stock restoration on compensation
   - Transactional consistency at database level

5. **Prescription Lifecycle**
   - Creation → Reservation → Billing → Payment → Completion
   - Or: Creation → Out of Stock → Failure
   - Or: Reservation → Billing → Payment Cancellation → Reversion

6. **Kafka Infrastructure**
   - 3.8.0 version in docker-compose
   - Auto topic creation enabled
   - 3 partitions per topic

---

## 7. WHAT'S MISSING OR NEEDS IMPROVEMENT

### ⚠️ Critical Gaps

1. **Saga Orchestrator / Coordinator**
   - No central orchestrator managing saga state
   - Choreography relies on implicit ordering
   - Hard to visualize overall saga progress
   - **Impact**: Difficult to debug failed sagas

2. **Saga State Tracking**
   - No persisted saga instance store
   - Cannot query "current state" of a saga
   - No compensation history audit
   - **Recommendation**: Add `SagaInstance` entity to track saga progression

3. **Idempotency**
   - Listeners lack idempotency keys
   - Duplicate messages could cause double-deduction
   - No duplicate detection mechanism
   - **Risk**: Race conditions in stock management

4. **Error Recovery**
   - No automatic retry mechanism
   - No dead-letter queue for failed events
   - Failed compensation has no fallback
   - **Problem**: Inconsistent state if compensation fails

5. **Monitoring & Observability**
   - No centralized logging for saga flows
   - No tracing across services
   - No metrics for saga success/failure rates
   - **Impact**: Hard to diagnose issues in production

### ⚠️ Implementation Gaps

6. **Global Exception Handler**
   - Controllers use manual try-catch
   - No `@RestControllerAdvice` for centralized handling
   - Inconsistent error response format

7. **Circuit Breaker Pattern**
   - No Resilience4j or Hystrix
   - Service failures not isolated
   - No cascading failure prevention

8. **Validation**
   - Missing `@Valid` annotations
   - No DTO validation in listeners
   - Could process malformed events

9. **Authorization**
   - Basic role-based access (ROLE_ADMIN, ROLE_PHARMACIST)
   - No fine-grained permissions
   - No service-to-service authentication

10. **Database Transactions**
    - Listeners use `@Transactional` but no explicit transaction boundary
    - Risk of partial compensation if DB fails mid-update

---

## 8. RECOMMENDATIONS

### Priority 1 - CRITICAL (Do First)

1. **Add Saga Instance Tracking**
   ```
   Create SagaInstance entity to track:
   - Saga ID (unique identifier)
   - Status (STARTED, STOCK_RESERVED, BILLED, PAID, COMPLETED, FAILED, COMPENSATED)
   - Current step
   - Started timestamp
   - Completed timestamp
   - Error reason (if failed)
   ```
   This enables querying saga state and manual remediation.

2. **Implement Idempotency Keys**
   - Add `idempotencyKey` to events
   - Store processed keys in database
   - Skip duplicate processing
   - Prevent double stock deductions

3. **Add Dead Letter Queue (DLQ)**
   - Kafka topic: `{original-topic}-dlq`
   - Route failed messages to DLQ
   - Manual intervention process for DLQ

### Priority 2 - HIGH (Do Soon)

4. **Implement Centralized Exception Handling**
   ```java
   @RestControllerAdvice
   public class GlobalExceptionHandler {
       @ExceptionHandler(InvalidPaymentException.class)
       public ResponseEntity<?> handlePaymentError(...) { }
   }
   ```

5. **Add Distributed Tracing**
   - Implement Spring Cloud Sleuth
   - Add correlation IDs to saga events
   - Log saga flow with trace IDs

6. **Circuit Breaker for Kafka**
   - Use Resilience4j for retry/fallback
   - Prevent cascading failures

### Priority 3 - MEDIUM (Nice to Have)

7. **Saga Orchestrator (Future)**
   - If choreography becomes too complex
   - Consider Axon Framework or custom orchestrator
   - Centralize saga logic

8. **Metrics & Monitoring**
   - Prometheus for metrics
   - Grafana dashboards for saga flows
   - Alert on failed compensation

9. **Audit Logging**
   - Immutable audit log for all saga steps
   - Compliance and debugging

10. **Service-to-Service Security**
    - Add mutual TLS (mTLS)
    - OAuth2 between services (if not same trusted domain)

---

## 9. TESTING ENDPOINTS FOR SAGA VALIDATION

**Location**: [medical-service/src/main/java/com/example/medicalservice/controller/SagaValidationController.java](medical-service/src/main/java/com/example/medicalservice/controller/SagaValidationController.java)

**No JWT Required (for testing)**:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/medical/saga-validation/success` | POST | Happy path: Success flow |
| `/api/medical/saga-validation/fail-payment` | POST | Compensation: Payment cancelled |
| `/api/medical/saga-validation/fail-stock` | POST | Failure: Out of stock |

**Example Response** (success flow):
```json
{
  "status": "SUCCESS",
  "auditLogs": [
    "Starting Saga Simulation: Happy Path (Success)",
    "Step 1: Ensured medicine 'SagaParacetamol' exists...",
    "Step 2: Saved prescription with ID...",
    "Waiting for Kafka to process stock reservation...",
    "Final Check: Prescription status: COMPLETED (Expected: COMPLETED)",
    "Final Check: Medicine stock: 8 (Expected: 8)",
    "Final Check: Tagihan status: PAID (Expected: PAID)"
  ]
}
```

---

## 10. ARCHITECTURE DIAGRAM

```
┌─────────────────────────────────────────────────────────────────┐
│                   CLIENT / API CONSUMER                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                    POST /api/auth/login
                    (credentials)
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                 API GATEWAY (Port 8000)                          │
│            Spring Cloud Gateway + JWT Auth                       │
│  Routes: /api/admin → 8001, /api/medical → 8002, etc.          │
└─────────────────────────────────────────────────────────────────┘
        │                    │                    │              │
        │ (+JWT)             │ (+JWT)             │ (+JWT)       │ (+JWT)
        ▼                    ▼                    ▼              ▼
    ┌────────────┐    ┌──────────────┐    ┌──────────────┐  ┌─────────────┐
    │   ADMIN    │    │   MEDICAL    │    │  PHARMACY    │  │  PAYMENT    │
    │  Service   │    │   Service    │    │   Service    │  │  Service    │
    │ (8001)     │    │  (8002)      │    │  (8004)      │  │  (8003)     │
    └────────────┘    └──────────────┘    └──────────────┘  └─────────────┘
         │                   │                   │                │
         ▼                   ▼                   ▼                ▼
    ┌────────────┐    ┌──────────────┐    ┌──────────────┐  ┌─────────────┐
    │  MySQL DB  │    │  MySQL DB    │    │  MySQL DB    │  │  MySQL DB   │
    │  (admin)   │    │  (medical)   │    │  (pharmacy)  │  │  (payment)  │
    └────────────┘    └──────────────┘    └──────────────┘  └─────────────┘
         ▲                   ▲                   ▲                ▲
         │                   │                   │                │
         └───────────────────┴───────────────────┴────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────┐
                    │  Apache Kafka (9092)      │
                    │  Topics:                  │
                    │  - prescription-created   │
                    │  - prescription-received  │
                    │  - payment-processed      │
                    │  - stock-updated          │
                    └───────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
    ┌──────────┐            ┌──────────────┐          ┌─────────────┐
    │ SagaMed  │            │SagaPharmacy  │          │SagaPayment  │
    │Listener  │            │  Listener    │          │  Listener   │
    └──────────┘            └──────────────┘          └─────────────┘
   (update status)         (reserve/revert stock)   (create invoice)
```

---

## 11. SUMMARY TABLE

| Component | Implementation | Completeness | Quality |
|-----------|----------------|--------------|---------|
| Saga Pattern | Event-Driven Choreography | 60% | Medium |
| API Gateway | Spring Cloud Gateway | 100% | High |
| Service Communication | Kafka + REST | 90% | High |
| Error Handling | Try-catch + Listeners | 50% | Low-Medium |
| Compensation Logic | Stock Reversion | 70% | Medium |
| Database Transactions | JPA + @Transactional | 80% | Medium |
| Monitoring | Basic Logging | 40% | Low |
| Security | JWT + Roles | 75% | Medium |
| Idempotency | Not Implemented | 0% | Critical Gap |
| Saga State Tracking | Not Implemented | 0% | Critical Gap |

---

## Conclusion

The GrowBusiness microservices architecture demonstrates a **solid event-driven foundation** with a working saga implementation for prescription processing. The API Gateway and JWT authentication are well-implemented. However, the lack of saga state tracking, idempotency handling, and centralized error recovery creates risks for production deployment. **Addressing the Priority 1 recommendations** (saga tracking, idempotency, DLQ) should be done before scaling to high-transaction volume.

