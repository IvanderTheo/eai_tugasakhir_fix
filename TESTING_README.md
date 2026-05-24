# 🧪 API GATEWAY SAGA TESTING README

## 📋 Overview

Ini adalah comprehensive testing guide untuk **Saga Pattern Implementation** melalui **API Gateway**. Semua requests akan melalui API Gateway (Port 8000) yang kemudian di-route ke services yang tepat.

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                      CLIENT/POSTMAN                          │
└────────────────────────┬─────────────────────────────────────┘
                         │ HTTP Requests
                         ↓
┌──────────────────────────────────────────────────────────────┐
│              API GATEWAY (Port 8000)                          │
│  - JWT Authentication                                        │
│  - Path routing                                              │
│  - Request/Response transformation                           │
└────┬────────────┬────────────────┬────────────────┬──────────┘
     │            │                │                │
  /api/medical  /api/pharmacy    /api/payment    /api/admin
     │            │                │                │
     ↓            ↓                ↓                ↓
┌─────────┐  ┌─────────┐    ┌─────────┐    ┌─────────┐
│Medical  │  │Pharmacy │    │Payment  │    │ Admin   │
│Service  │  │Service  │    │Service  │    │ Service │
│(8002)   │  │(8004)   │    │(8003)   │    │ (8001)  │
└─────────┘  └─────────┘    └─────────┘    └─────────┘
     │            │                │
     └────────────┼────────────────┘
                  │ Kafka Events
                  ↓
        ┌──────────────────┐
        │  Kafka Broker    │
        │ (Event Hub)      │
        └──────────────────┘
                  │
      ┌───────────┼───────────┐
      ↓           ↓           ↓
  Pharmacy    Medical    Payment
  Listener    Listener   Listener
  
  ├─ SagaPharmacyListener
  ├─ SagaMedicalListener
  └─ SagaPaymentListener
  
  + Idempotency Check
  + State Tracking (SagaInstance)
  + DLQ Error Handling
```

---

## ✅ Pre-Testing Setup

### 1. Verify Services Running

```bash
# Check Medical Service
curl -s http://localhost:8002/actuator/health | jq .

# Check Pharmacy Service
curl -s http://localhost:8004/actuator/health | jq .

# Check Payment Service
curl -s http://localhost:8003/actuator/health | jq .

# Check Admin Service
curl -s http://localhost:8001/actuator/health | jq .

# Check API Gateway
curl -s http://localhost:8000/actuator/health | jq .
```

**Expected Output**: `{"status":"UP"}`

### 2. Verify Databases

```bash
# Check saga_instances tables exist
mysql -u root -p pharmacy_db -e "SHOW TABLES LIKE 'saga%';"
mysql -u root -p medical_db -e "SHOW TABLES LIKE 'saga%';"
mysql -u root -p payment_db -e "SHOW TABLES LIKE 'saga%';"

# Expected: saga_instances table should be listed
```

### 3. Get Authentication Token

```bash
# Register user first (if needed)
curl -X POST http://localhost:8000/api/admin/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "role": "DOCTOR"
  }'

# Login & get token
TOKEN=$(curl -s -X POST http://localhost:8000/api/admin/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }' | jq -r '.token')

echo "Token: $TOKEN"

# Save for later use
export AUTH_TOKEN=$TOKEN
```

---

## 🧪 Test Scenario 1: Normal Flow (Happy Path)

### Objective
Test complete saga flow: Prescription → Stock Reserve → Invoice → Payment → Completion

### Setup Data

```bash
# Create patient (via admin API)
PATIENT_ID=$(curl -s -X POST http://localhost:8000/api/admin/patients \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nama": "John Doe",
    "usia": 30,
    "alamat": "123 Main St",
    "telp": "08123456789"
  }' | jq -r '.id')

echo "Created patient: $PATIENT_ID"

# Create doctor/examination first
EXAMINATION_ID=$(curl -s -X POST http://localhost:8000/api/medical/pemeriksaan \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "pasienId": '$PATIENT_ID',
    "dokterNama": "Dr. Smith",
    "keluhan": "Headache",
    "diagnosis": "Migraine",
    "status": "COMPLETED"
  }' | jq -r '.id')

echo "Created examination: $EXAMINATION_ID"

# Ensure medicine exists
mysql -u root -p pharmacy_db -e \
  "INSERT INTO obat (nama_obat, harga, stok) VALUES ('Aspirin', 5000, 100);"
```

### Test Steps

**Step 1: Send Prescription via API Gateway**

```bash
# Using API Gateway endpoint
PRESCRIPTION=$(curl -s -X POST http://localhost:8000/api/medical/resep \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "X-Idempotency-Key: TEST-HAPPY-PATH-001" \
  -H "Content-Type: application/json" \
  -d '{
    "pemeriksaanId": '$EXAMINATION_ID',
    "pasienId": '$PATIENT_ID',
    "namaObat": "Aspirin",
    "dosis": "500mg",
    "frekuensi": "2x sehari",
    "jumlah": 10,
    "catatan": "For headache"
  }')

PRESCRIPTION_ID=$(echo $PRESCRIPTION | jq -r '.id')
echo "Created prescription: $PRESCRIPTION_ID"
echo "Response:"
echo $PRESCRIPTION | jq .
```

**Expected Response**:
```json
{
  "id": 1,
  "pemeriksaanId": 1,
  "pasienId": 1,
  "namaObat": "Aspirin",
  "status": "CREATED",
  "message": "Prescription created successfully"
}
```

**Step 2: Wait for Event Processing**

```bash
sleep 3
```

**Step 3: Verify Saga Created in Pharmacy DB**

```bash
mysql -u root -p pharmacy_db <<EOF
SELECT 
  saga_id,
  message_id,
  saga_status,
  saga_topic,
  created_at
FROM saga_instances 
WHERE message_id = 'TEST-HAPPY-PATH-001'
ORDER BY created_at DESC;
EOF
```

**Expected Output**:
```
saga_id: 550e8400-e29b-41d4-a716-446655440000
message_id: TEST-HAPPY-PATH-001
saga_status: IN_PROGRESS
saga_topic: prescription-created
created_at: 2026-05-24 10:30:45
```

**Step 4: Verify Stock Reserved**

```bash
mysql -u root -p pharmacy_db -e \
  "SELECT nama_obat, stok FROM obat WHERE nama_obat='Aspirin';"
```

**Expected**: Stok reduced dari 100 menjadi 90 (reserved 10)

**Step 5: Verify Invoice Created in Payment DB**

```bash
mysql -u root -p payment_db -e \
  "SELECT 
    id,
    resep_id,
    pasien_id,
    harga_obat,
    biaya_konsultasi,
    status
   FROM tagihan 
   WHERE resep_id = $PRESCRIPTION_ID;"
```

**Expected**:
```
id: 1
resep_id: 1
pasien_id: 1
harga_obat: 50000
biaya_konsultasi: 50000
status: PENDING
```

**Step 6: Get Saga ID from Any Service**

```bash
# Get from pharmacy
SAGA_ID=$(mysql -u root -p pharmacy_db -s -N -e \
  "SELECT saga_id FROM saga_instances 
   WHERE message_id='TEST-HAPPY-PATH-001' LIMIT 1;")

echo "Saga ID: $SAGA_ID"
```

**Step 7: Simulate Payment Success (Admin action)**

```bash
# Mark tagihan as completed
mysql -u root -p payment_db -e \
  "UPDATE tagihan SET status = 'COMPLETED' 
   WHERE resep_id = $PRESCRIPTION_ID;"

# In real scenario, payment gateway would publish this
# For testing, we'll manually trigger the payment-processed event
# via a test endpoint or direct database update
```

**Step 8: Wait for Final Processing**

```bash
sleep 3
```

**Step 9: Verify Final Saga Status = COMPLETED**

```bash
mysql -u root -p pharmacy_db -e \
  "SELECT 
    saga_id,
    saga_status,
    completed_at,
    compensation_status
   FROM saga_instances 
   WHERE saga_id = '$SAGA_ID';"
```

**Expected**:
```
saga_status: COMPLETED
completed_at: 2026-05-24 10:30:48
compensation_status: NULL
```

**Step 10: Verify Prescription Status Updated in Medical**

```bash
mysql -u root -p medical_db -e \
  "SELECT id, status FROM resep WHERE id = $PRESCRIPTION_ID;"
```

**Expected**: status = COMPLETED

✅ **Test Passed**: If all steps completed successfully!

---

## 🔄 Test Scenario 2: Idempotency via API Gateway

### Objective
Send same prescription request twice (same idempotency key) via API Gateway. Should only process once.

### Test Steps

**Step 1: Send Request with Idempotency Key**

```bash
IDEMPOTENT_KEY="IDEMPOTENCY-TEST-$(date +%s)"

curl -X POST http://localhost:8000/api/medical/resep \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "X-Idempotency-Key: $IDEMPOTENT_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "pemeriksaanId": '$EXAMINATION_ID',
    "pasienId": '$PATIENT_ID',
    "namaObat": "Paracetamol",
    "dosis": "500mg",
    "frekuensi": "3x sehari",
    "jumlah": 5,
    "catatan": "For fever"
  }' | jq .

sleep 1
```

**Step 2: Send EXACT SAME Request Again**

```bash
curl -X POST http://localhost:8000/api/medical/resep \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "X-Idempotency-Key: $IDEMPOTENT_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "pemeriksaanId": '$EXAMINATION_ID',
    "pasienId": '$PATIENT_ID',
    "namaObat": "Paracetamol",
    "dosis": "500mg",
    "frekuensi": "3x sehari",
    "jumlah": 5,
    "catatan": "For fever"
  }' | jq .
```

**Expected**: Same response both times (API Gateway + Services handle idempotency)

**Step 3: Verify Only One Saga Created**

```bash
mysql -u root -p pharmacy_db -e \
  "SELECT COUNT(*) as saga_count FROM saga_instances 
   WHERE message_id='$IDEMPOTENT_KEY';"
```

**Expected Output**:
```
saga_count: 1
```

**Step 4: Verify Stock Reduced Only Once**

```bash
mysql -u root -p pharmacy_db -e \
  "SELECT stok FROM obat WHERE nama_obat='Paracetamol';"
```

**Expected**: Stok reduced by 5 (not 10)

**Step 5: Check Logs**

```bash
# Check pharmacy service logs
grep "IDEMPOTENCY" pharmacy-service.log

# Expected to see:
# "⚠️ IDEMPOTENCY: Message XXX already processed, skipping"
```

✅ **Test Passed**: If saga_count = 1 and stock reduced only once!

---

## 💔 Test Scenario 3: Compensation (Payment Failure)

### Objective
Trigger payment failure and verify stock is restored (compensation)

### Test Steps

**Step 1: Create Prescription**

```bash
COMP_PRESCRIPTION=$(curl -s -X POST http://localhost:8000/api/medical/resep \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "X-Idempotency-Key: COMPENSATION-TEST-001" \
  -H "Content-Type: application/json" \
  -d '{
    "pemeriksaanId": '$EXAMINATION_ID',
    "pasienId": '$PATIENT_ID',
    "namaObat": "Ibuprofen",
    "dosis": "400mg",
    "frekuensi": "2x sehari",
    "jumlah": 20,
    "catatan": "For pain"
  }' | jq -r '.id')

echo "Created prescription for compensation test: $COMP_PRESCRIPTION"

sleep 2
```

**Step 2: Record Initial Stock**

```bash
INITIAL_STOCK=$(mysql -u root -p pharmacy_db -s -N -e \
  "SELECT stok FROM obat WHERE nama_obat='Ibuprofen';")

echo "Initial stock: $INITIAL_STOCK"
```

**Step 3: Verify Stock Reserved**

```bash
RESERVED_STOCK=$(mysql -u root -p pharmacy_db -s -N -e \
  "SELECT stok FROM obat WHERE nama_obat='Ibuprofen';")

echo "Stock after reservation: $RESERVED_STOCK"
# Should be INITIAL_STOCK - 20
```

**Step 4: Fail the Payment**

```bash
# Get invoice ID
INVOICE_ID=$(mysql -u root -p payment_db -s -N -e \
  "SELECT id FROM tagihan WHERE resep_id=$COMP_PRESCRIPTION LIMIT 1;")

echo "Invoice ID: $INVOICE_ID"

# Mark as FAILED
mysql -u root -p payment_db -e \
  "UPDATE tagihan SET status = 'FAILED' 
   WHERE id = $INVOICE_ID;"

sleep 2
```

**Step 5: Verify Compensation Executed**

```bash
FINAL_STOCK=$(mysql -u root -p pharmacy_db -s -N -e \
  "SELECT stok FROM obat WHERE nama_obat='Ibuprofen';")

echo "Stock after compensation: $FINAL_STOCK"
# Should be back to INITIAL_STOCK (20 restored!)
```

**Step 6: Verify Saga Status = COMPENSATED**

```bash
mysql -u root -p pharmacy_db -e \
  "SELECT 
    saga_status,
    compensation_status,
    completed_at
   FROM saga_instances 
   WHERE message_id = 'COMPENSATION-TEST-001';"
```

**Expected**:
```
saga_status: COMPENSATED
compensation_status: EXECUTED
completed_at: NOT NULL
```

**Step 7: Verify Prescription Status = CANCELLED**

```bash
mysql -u root -p medical_db -e \
  "SELECT status FROM resep WHERE id = $COMP_PRESCRIPTION;"
```

**Expected**: status = CANCELLED

**Step 8: Check Compensation Logs**

```bash
grep "COMPENSATION EXECUTED" pharmacy-service.log

# Expected:
# "✅ COMPENSATION EXECUTED: Reverted stock for prescriptionId: ..."
```

✅ **Test Passed**: If stock restored to initial value and saga = COMPENSATED!

---

## 🚨 Test Scenario 4: Error Handling & DLQ

### Objective
Trigger an error in event processing and verify it's sent to DLQ

### Prerequisites

Option 1: Temporarily modify listener to throw exception
```java
// In SagaPharmacyListener.java
@KafkaListener(...)
public void listenPrescriptionCreated(...) {
    try {
        throw new RuntimeException("Simulated error for DLQ test");
        // ... rest of code
    }
}
```

Option 2: Kill pharmacy service mid-processing and restart

### Test Steps

**Step 1: Send Prescription**

```bash
curl -X POST http://localhost:8000/api/medical/resep \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "X-Idempotency-Key: DLQ-TEST-001" \
  -H "Content-Type: application/json" \
  -d '{
    "pemeriksaanId": '$EXAMINATION_ID',
    "pasienId": '$PATIENT_ID',
    "namaObat": "Vitamin C",
    "dosis": "1000mg",
    "frekuensi": "1x sehari",
    "jumlah": 30,
    "catatan": "For immunity"
  }' | jq .

sleep 3
```

**Step 2: Check DLQ Topic**

```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic prescription-received.dlq \
  --from-beginning \
  --max-messages 5
```

**Expected**: See error message with dlqReason

**Step 3: Verify Saga Marked as FAILED**

```bash
mysql -u root -p pharmacy_db -e \
  "SELECT 
    saga_status,
    error_message,
    retry_count
   FROM saga_instances 
   WHERE message_id = 'DLQ-TEST-001';"
```

**Expected**:
```
saga_status: FAILED
error_message: (contains error details)
retry_count: 1
```

**Step 4: Check DLQ Handler Logs**

```bash
grep "DLQ Handler" pharmacy-service.log

# Expected:
# "DLQ Handler - Processing failed message"
# "ALERT: DLQ Message - Topic: prescription-received"
```

**Step 5: Remove Simulated Error & Restart Service**

```bash
# Revert code changes in listener
# Restart pharmacy service
cd pharmacy-service
mvn spring-boot:run
```

✅ **Test Passed**: If event found in DLQ and saga_status = FAILED!

---

## 🔗 Test Scenario 5: Multi-Service Saga Flow

### Objective
Test complete saga involving all 3 services through API Gateway

### Test Steps

**Step 1: Create Test Data**

```bash
mysql -u root -p medical_db -e \
  "INSERT INTO pemeriksaan (pasien_id, dokter_id, status) 
   VALUES (10, 1, 'COMPLETED');"

mysql -u root -p pharmacy_db -e \
  "INSERT INTO obat (nama_obat, harga, stok) 
   VALUES ('Test Medicine', 100000, 50);"
```

**Step 2: Send Prescription via API Gateway**

```bash
MULTI_PRESCRIPTION=$(curl -s -X POST http://localhost:8000/api/medical/resep \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -H "X-Idempotency-Key: MULTI-SERVICE-001" \
  -H "Content-Type: application/json" \
  -d '{
    "pemeriksaanId": 10,
    "pasienId": 10,
    "namaObat": "Test Medicine",
    "dosis": "1g",
    "frekuensi": "2x sehari",
    "jumlah": 15,
    "catatan": "Multi-service test"
  }' | jq -r '.id')

echo "Prescription ID: $MULTI_PRESCRIPTION"

sleep 3
```

**Step 3: Trace Saga Across Services**

```bash
# Get Saga ID from pharmacy
SAGA=$(mysql -u root -p pharmacy_db -s -N -e \
  "SELECT saga_id FROM saga_instances 
   WHERE message_id = 'MULTI-SERVICE-001' LIMIT 1;")

echo "Saga ID: $SAGA"
```

**Step 4: Check Pharmacy Status**

```bash
mysql -u root -p pharmacy_db -e \
  "SELECT saga_id, saga_status FROM saga_instances 
   WHERE saga_id = '$SAGA';"
```

**Expected**: saga_status = IN_PROGRESS or COMPLETED

**Step 5: Check Medical Status**

```bash
mysql -u root -p medical_db -e \
  "SELECT saga_id, saga_status FROM saga_instances 
   WHERE saga_id = '$SAGA';"
```

**Expected**: saga_status = IN_PROGRESS or COMPLETED

**Step 6: Check Payment Status**

```bash
mysql -u root -p payment_db -e \
  "SELECT saga_id, saga_status FROM saga_instances 
   WHERE saga_id = '$SAGA';"
```

**Expected**: saga_status = IN_PROGRESS

**Step 7: Verify Cross-Service Data Exchange**

```bash
# Invoice created in payment
mysql -u root -p payment_db -e \
  "SELECT COUNT(*) FROM tagihan WHERE resep_id = $MULTI_PRESCRIPTION;"

# Stock reserved in pharmacy
mysql -u root -p pharmacy_db -e \
  "SELECT stok FROM obat WHERE nama_obat = 'Test Medicine';"

# Prescription status updated in medical
mysql -u root -p medical_db -e \
  "SELECT status FROM resep WHERE id = $MULTI_PRESCRIPTION;"
```

✅ **Test Passed**: If all services have saga instance with same saga_id!

---

## 📊 API Endpoints Reference

### Via API Gateway (Port 8000)

```bash
# Medical Service endpoints
POST   /api/medical/resep              # Create prescription
GET    /api/medical/resep/{id}         # Get prescription
GET    /api/medical/pemeriksaan        # List examinations
POST   /api/medical/pemeriksaan        # Create examination

# Pharmacy Service endpoints
GET    /api/pharmacy/obat              # List medicines
POST   /api/pharmacy/obat              # Add medicine
GET    /api/pharmacy/obat/{id}         # Get medicine details

# Payment Service endpoints
GET    /api/payment/tagihan           # List invoices
GET    /api/payment/tagihan/{id}      # Get invoice details

# Admin Service endpoints
POST   /api/admin/users/register      # Register user
POST   /api/admin/users/login         # Login user
POST   /api/admin/patients            # Create patient
GET    /api/admin/patients            # List patients
```

### Direct Service Endpoints (for debugging)

```bash
# Medical Service (8002)
curl http://localhost:8002/api/resep

# Pharmacy Service (8004)
curl http://localhost:8004/api/obat

# Payment Service (8003)
curl http://localhost:8003/api/tagihan

# Admin Service (8001)
curl http://localhost:8001/api/users
```

---

## 🔍 Monitoring & Debugging

### View Saga Status

```bash
# All active sagas
mysql -u root -p pharmacy_db -e \
  "SELECT 
    saga_id, 
    saga_status, 
    created_at 
   FROM saga_instances 
   WHERE saga_status IN ('INITIATED', 'IN_PROGRESS') 
   ORDER BY created_at DESC;"

# Failed sagas
mysql -u root -p pharmacy_db -e \
  "SELECT 
    saga_id, 
    error_message, 
    retry_count 
   FROM saga_instances 
   WHERE saga_status = 'FAILED';"

# Compensated sagas
mysql -u root -p pharmacy_db -e \
  "SELECT 
    saga_id, 
    compensation_status, 
    completed_at 
   FROM saga_instances 
   WHERE saga_status = 'COMPENSATED';"
```

### View Kafka Events

```bash
# List all topics
docker-compose exec kafka kafka-topics --list \
  --bootstrap-server localhost:9092

# View prescription-created events
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic prescription-created \
  --from-beginning \
  --max-messages 5

# View DLQ events
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic prescription-received.dlq \
  --from-beginning \
  --max-messages 5
```

### View Service Logs

```bash
# Pharmacy service logs
tail -f pharmacy-service.log | grep -E "SAGA|IDEMPOTENCY|COMPENSATION|DLQ"

# Medical service logs
tail -f medical-service.log | grep -E "SAGA|IDEMPOTENCY|DLQ"

# Payment service logs
tail -f payment-service.log | grep -E "SAGA|IDEMPOTENCY|DLQ"

# API Gateway logs
tail -f api-gateway.log | grep -E "routing|request"
```

---

## ✅ Testing Checklist

After running all 5 test scenarios:

- [ ] Scenario 1: Normal flow completes successfully
- [ ] Scenario 2: Idempotency prevents duplicate processing
- [ ] Scenario 3: Compensation restores stock on failure
- [ ] Scenario 4: DLQ captures errors and marks saga as FAILED
- [ ] Scenario 5: Multi-service saga tracks across all services
- [ ] API Gateway routing works correctly
- [ ] All requests return appropriate status codes
- [ ] Saga instances created in all service databases
- [ ] No duplicate saga_ids in database
- [ ] Error messages properly logged
- [ ] Token-based authentication working
- [ ] Stock levels accurate after operations

---

## 🎓 Expected Success Metrics

| Metric | Good | Bad |
|--------|------|-----|
| API Gateway Response Time | <500ms | >1000ms |
| Saga Completion Rate | >95% | <90% |
| Idempotency Hit Rate | 0-5% | >20% |
| DLQ Message Rate | <1% | >5% |
| Stock Accuracy | 100% | <95% |
| Compensation Success | 100% | <95% |

---

## 🆘 Troubleshooting

### Issue: 401 Unauthorized on API Gateway
**Solution**: Get valid token first
```bash
TOKEN=$(curl -s -X POST http://localhost:8000/api/admin/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' \
  | jq -r '.token')

export AUTH_TOKEN=$TOKEN
```

### Issue: Saga not created
**Solution**: Check Kafka connection
```bash
# Verify Kafka running
docker-compose ps | grep kafka

# Check service logs for Kafka errors
grep "Kafka" pharmacy-service.log
```

### Issue: Stock not reserved
**Solution**: Check medicine exists
```bash
mysql -u root -p pharmacy_db -e \
  "SELECT * FROM obat WHERE nama_obat='Aspirin';"
```

### Issue: DLQ messages not appearing
**Solution**: Verify DLQ topics created
```bash
docker-compose exec kafka kafka-topics --list \
  --bootstrap-server localhost:9092 | grep dlq
```

---

## 📝 Notes

- All timestamps in database are in UTC
- Message IDs in saga_instances should match Kafka message keys
- Payload JSON contains full event for audit trail
- Compensation is automatic on payment FAILED status
- DLQ handler runs independently for each message

---

**Ready to test? Start with Scenario 1 (Happy Path)!** 🚀
