# 🧪 Comprehensive Testing Guide - Saga Implementation

## 📋 Pre-Testing Checklist

- [ ] All services compiled successfully
- [ ] Kafka running and healthy
- [ ] Databases migrated (saga_instances tables created)
- [ ] All 3 services started without errors
- [ ] Logs showing "Listened on port 8002, 8003, 8004"

---

## Test Scenario 1: Normal Flow (Happy Path) ✅

### Objective
Test that a complete saga flows successfully from prescription → stock → payment → completion

### Setup
```bash
# Ensure data exists
mysql -u root -p medical_db
INSERT INTO pemeriksaan (pasien_id, dokter_id, status) 
VALUES (1, 1, 'COMPLETED');

mysql -u root -p pharmacy_db
INSERT INTO obat (nama_obat, harga, stok) 
VALUES ('Aspirin', 5000, 100);
```

### Test Steps

**Step 1: Send Prescription**
```bash
PRESCRIPTION_ID=$(curl -s -X POST http://localhost:8000/api/medical/resep \
  -H "X-Idempotency-Key: TEST-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer token" \
  -d '{
    "pemeriksaanId": 1,
    "pasienId": 1,
    "namaObat": "Aspirin",
    "dosis": "500mg",
    "frekuensi": "2x sehari",
    "jumlah": 10,
    "catatan": "For testing"
  }' | jq -r '.id')

echo "Created prescription: $PRESCRIPTION_ID"
```

**Step 2: Wait for Event Processing**
```bash
sleep 3
```

**Step 3: Verify Saga Creation**
```bash
mysql -u root -p pharmacy_db <<EOF
SELECT 
  saga_id,
  saga_status,
  message_id,
  compensation_status
FROM saga_instances 
WHERE saga_topic = 'prescription-created' 
ORDER BY created_at DESC LIMIT 3;
EOF
```

**Expected Output**:
```
saga_id: 550e8400-e29b-41d4-a716-446655440000
saga_status: IN_PROGRESS
message_id: TEST-001
compensation_status: NULL
```

**Step 4: Verify Stock Reservation**
```bash
mysql -u root -p pharmacy_db -e \
  "SELECT nama_obat, stok FROM obat WHERE nama_obat='Aspirin';"
```

**Expected**: Stok reduced by 10

**Step 5: Verify Invoice Creation**
```bash
mysql -u root -p payment_db -e \
  "SELECT * FROM tagihan WHERE resep_id=$PRESCRIPTION_ID;"
```

**Expected**: Tagihan status = PENDING

**Step 6: Simulate Payment Success**
```bash
mysql -u root -p payment_db <<EOF
UPDATE tagihan SET status = 'COMPLETED' 
WHERE resep_id = $PRESCRIPTION_ID;

-- Publish payment-processed event
-- (In real scenario, payment gateway would do this)
EOF
```

**Step 7: Verify Final State**
```bash
mysql -u root -p pharmacy_db <<EOF
SELECT 
  saga_id,
  saga_status,
  completed_at
FROM saga_instances 
WHERE saga_topic = 'prescription-created' 
ORDER BY created_at DESC LIMIT 1;
EOF
```

**Expected**: saga_status = COMPLETED, completed_at = NOT NULL

---

## Test Scenario 2: Idempotency (Duplicate Message) ✅

### Objective
Verify that sending the same message twice doesn't create duplicate saga instances

### Test Steps

**Step 1: Send Prescription - First Time**
```bash
curl -X POST http://localhost:8000/api/medical/resep \
  -H "X-Idempotency-Key: IDEMPOTENT-TEST-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer token" \
  -d '{
    "pemeriksaanId": 2,
    "pasienId": 2,
    "namaObat": "Paracetamol",
    "dosis": "500mg",
    "frekuensi": "3x sehari",
    "jumlah": 5,
    "catatan": "Idempotency test"
  }'
```

**Step 2: Wait**
```bash
sleep 1
```

**Step 3: Send EXACT SAME Request Again**
```bash
curl -X POST http://localhost:8000/api/medical/resep \
  -H "X-Idempotency-Key: IDEMPOTENT-TEST-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer token" \
  -d '{
    "pemeriksaanId": 2,
    "pasienId": 2,
    "namaObat": "Paracetamol",
    "dosis": "500mg",
    "frekuensi": "3x sehari",
    "jumlah": 5,
    "catatan": "Idempotency test"
  }'

# Should return SAME response as first request
```

**Step 4: Check Database**
```bash
mysql -u root -p pharmacy_db -e \
  "SELECT COUNT(*) as saga_count FROM saga_instances 
   WHERE message_id='IDEMPOTENT-TEST-001';"
```

**Expected**: 
```
saga_count: 1
```

**Step 5: Verify Stock Changed Only Once**
```bash
mysql -u root -p pharmacy_db -e \
  "SELECT nama_obat, stok FROM obat WHERE nama_obat='Paracetamol';"
```

**Expected**: Stock reduced by 5 (only once, not 10)

**Step 6: Check Logs**
```bash
# Look for IDEMPOTENCY messages in pharmacy-service logs
grep "IDEMPOTENCY" pharmacy-service.log

# Expected:
# "⚠️ IDEMPOTENCY: Message IDEMPOTENT-TEST-001 already processed, skipping"
```

✅ **Test Passed**: If saga_count = 1 and stock reduced exactly once!

---

## Test Scenario 3: Compensation (Payment Failure) ✅

### Objective
Verify that when payment fails, stock is restored (compensation executed)

### Test Steps

**Step 1: Create Prescription**
```bash
PRESCRIPTION_ID=$(curl -s -X POST http://localhost:8000/api/medical/resep \
  -H "X-Idempotency-Key: COMPENSATION-TEST-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer token" \
  -d '{
    "pemeriksaanId": 3,
    "pasienId": 3,
    "namaObat": "Ibuprofen",
    "dosis": "400mg",
    "frekuensi": "2x sehari",
    "jumlah": 20,
    "catatan": "Compensation test"
  }' | jq -r '.id')

echo "Created prescription: $PRESCRIPTION_ID"
```

**Step 2: Record Initial Stock**
```bash
mysql -u root -p pharmacy_db -e \
  "SELECT stok FROM obat WHERE nama_obat='Ibuprofen';" > /tmp/initial_stock.txt
```

**Step 3: Wait for Reservation**
```bash
sleep 3
```

**Step 4: Verify Stock Reserved**
```bash
mysql -u root -p pharmacy_db -e \
  "SELECT stok FROM obat WHERE nama_obat='Ibuprofen';"
# Should be less than initial
```

**Step 5: Get Saga ID**
```bash
SAGA_ID=$(mysql -u root -p pharmacy_db -s -N -e \
  "SELECT saga_id FROM saga_instances 
   WHERE message_id='COMPENSATION-TEST-001' LIMIT 1;")

echo "Saga ID: $SAGA_ID"
```

**Step 6: Simulate Payment Failure**
```bash
# Get invoice
INVOICE_ID=$(mysql -u root -p payment_db -s -N -e \
  "SELECT id FROM tagihan WHERE resep_id=$PRESCRIPTION_ID LIMIT 1;")

# Fail the payment
mysql -u root -p payment_db <<EOF
UPDATE tagihan SET status = 'FAILED' WHERE id = $INVOICE_ID;

-- Trigger payment-processed event with FAILED status
-- (In real scenario, payment gateway would publish this)
EOF
```

**Step 7: Wait for Compensation**
```bash
sleep 3
```

**Step 8: Verify Compensation Executed**
```bash
mysql -u root -p pharmacy_db -e \
  "SELECT saga_status, compensation_status, completed_at 
   FROM saga_instances 
   WHERE saga_id='$SAGA_ID';"
```

**Expected**:
```
saga_status: COMPENSATED
compensation_status: EXECUTED
completed_at: NOT NULL
```

**Step 9: Verify Stock Restored**
```bash
mysql -u root -p pharmacy_db -e \
  "SELECT stok FROM obat WHERE nama_obat='Ibuprofen';"
# Should be back to initial or close
```

**Step 10: Check Logs**
```bash
grep "COMPENSATION" pharmacy-service.log

# Expected:
# "✅ COMPENSATION EXECUTED: Reverted stock for prescriptionId: ..."
```

✅ **Test Passed**: If compensation_status = EXECUTED and stock restored!

---

## Test Scenario 4: DLQ (Error Handling) 🚨

### Objective
Verify that failed events are sent to DLQ and saga marked as FAILED

### Test Steps

**Step 1: Add Break in Code (Simulate Error)**
```
Edit pharmacy-service listener to throw exception:

@KafkaListener(...)
public void listenPrescriptionCreated(...) {
    try {
        throw new RuntimeException("Simulated error for DLQ test");
        // ... rest of code
    } catch (Exception e) {
        // ... error handling will send to DLQ
    }
}
```

OR use database constraint violation to trigger error

**Step 2: Restart Service**
```bash
cd pharmacy-service
mvn spring-boot:run
```

**Step 3: Send Prescription**
```bash
curl -X POST http://localhost:8000/api/medical/resep \
  -H "X-Idempotency-Key: DLQ-TEST-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer token" \
  -d '{
    "pemeriksaanId": 4,
    "pasienId": 4,
    "namaObat": "Vitamin",
    "dosis": "1 tablet",
    "frekuensi": "1x sehari",
    "jumlah": 30,
    "catatan": "DLQ test"
  }'
```

**Step 4: Wait for Error Processing**
```bash
sleep 3
```

**Step 5: Check DLQ Topic**
```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic prescription-received.dlq \
  --from-beginning \
  --max-messages 5
```

**Expected**: See error event with dlqReason

**Step 6: Verify Saga Marked Failed**
```bash
mysql -u root -p pharmacy_db -e \
  "SELECT saga_id, saga_status, error_message, retry_count 
   FROM saga_instances 
   WHERE message_id='DLQ-TEST-001';"
```

**Expected**:
```
saga_status: FAILED
error_message: (contains error details)
retry_count: 1
```

**Step 7: Check DLQ Handler Logs**
```bash
grep "DLQ Handler" pharmacy-service.log

# Expected:
# "DLQ Handler - Processing failed message from prescription-received DLQ"
```

**Step 8: Revert Code Changes**
```
Remove the simulated error from listener
Restart service
```

✅ **Test Passed**: If event found in DLQ and saga_status = FAILED!

---

## Test Scenario 5: Multi-Service Saga Flow 🔄

### Objective
Test the complete end-to-end saga involving all 3 services

### Test Steps

**Step 1: Prepare Data**
```bash
mysql -u root -p medical_db -e \
  "INSERT INTO pemeriksaan (pasien_id, dokter_id, status) 
   VALUES (5, 1, 'COMPLETED');"

mysql -u root -p pharmacy_db -e \
  "INSERT INTO obat (nama_obat, harga, stok) 
   VALUES ('MultiTest Drug', 100000, 50);"
```

**Step 2: Send Prescription**
```bash
PRESCRIPTION_ID=$(curl -s -X POST http://localhost:8000/api/medical/resep \
  -H "X-Idempotency-Key: MULTI-SERVICE-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer token" \
  -d '{
    "pemeriksaanId": 5,
    "pasienId": 5,
    "namaObat": "MultiTest Drug",
    "dosis": "1g",
    "frekuensi": "2x sehari",
    "jumlah": 15,
    "catatan": "Multi-service test"
  }' | jq -r '.id')
```

**Step 3: Trace Through All Services**

**In Pharmacy**:
```bash
sleep 1
mysql -u root -p pharmacy_db -e \
  "SELECT saga_id, saga_status FROM saga_instances 
   WHERE message_id='MULTI-SERVICE-001';"
# Should be: IN_PROGRESS
```

**In Medical**:
```bash
mysql -u root -p medical_db -e \
  "SELECT saga_id, saga_status FROM saga_instances 
   WHERE message_id='MULTI-SERVICE-001';"
# Should be: IN_PROGRESS or COMPLETED
```

**In Payment**:
```bash
mysql -u root -p payment_db -e \
  "SELECT saga_id, saga_status FROM saga_instances 
   WHERE message_id='MULTI-SERVICE-001';"
# Should be: IN_PROGRESS
```

**Step 4: Check Cross-Service Data**
```bash
# Verify invoice created in payment
mysql -u root -p payment_db -e \
  "SELECT * FROM tagihan WHERE resep_id=$PRESCRIPTION_ID;"

# Verify stock reserved in pharmacy
mysql -u root -p pharmacy_db -e \
  "SELECT stok FROM obat WHERE nama_obat='MultiTest Drug';"

# Verify prescription status updated in medical
mysql -u root -p medical_db -e \
  "SELECT status FROM resep WHERE id=$PRESCRIPTION_ID;"
```

**Step 5: Simulate Payment Success**
```bash
mysql -u root -p payment_db -e \
  "UPDATE tagihan SET status = 'COMPLETED' 
   WHERE resep_id = $PRESCRIPTION_ID;"

sleep 2
```

**Step 6: Verify Final State Across Services**
```bash
# All should show COMPLETED
mysql -u root -p pharmacy_db -e \
  "SELECT saga_status FROM saga_instances 
   WHERE message_id='MULTI-SERVICE-001';"

mysql -u root -p medical_db -e \
  "SELECT saga_status FROM saga_instances 
   WHERE message_id='MULTI-SERVICE-001';"

mysql -u root -p payment_db -e \
  "SELECT saga_status FROM saga_instances 
   WHERE message_id='MULTI-SERVICE-001';"
```

✅ **Test Passed**: If all services show saga_status = COMPLETED!

---

## Performance Metrics to Check

### After Testing
```bash
mysql -u root -p pharmacy_db -e \
  "SELECT 
    saga_status, 
    COUNT(*) as count,
    AVG(TIMESTAMPDIFF(SECOND, created_at, COMPLETED_AT)) as avg_duration_sec
   FROM saga_instances
   GROUP BY saga_status;"
```

**Expected** (for normal flow):
- COMPLETED: ~90%+ of sagas
- FAILED: <5%
- avg_duration: 1-3 seconds

---

## Troubleshooting During Testing

| Issue | Diagnosis | Solution |
|-------|-----------|----------|
| Saga not creating | Check listener logs | Verify Kafka connection |
| Duplicate saga entries | messageId not unique | Restart service + clear DLQ |
| Stock not restored | Compensation not triggered | Check payment event published |
| DLQ message not received | Handler not registered | Check @KafkaListener annotation |
| Long processing time | Event queuing | Check Kafka consumer lag |

---

## Success Criteria Summary

- ✅ Saga instance created for each event
- ✅ Idempotency: duplicate messages skipped
- ✅ Compensation: stock restored on payment failure
- ✅ DLQ: errors captured and tracked
- ✅ All 3 services participate in saga
- ✅ No duplicate data in database
- ✅ Error messages logged
- ✅ Timestamps recorded correctly

---

**Ready to test? Start with Scenario 1!** 🚀
