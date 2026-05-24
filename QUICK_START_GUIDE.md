# 🚀 Quick Start Guide - Critical Gaps Implementation

## 📦 Files Created Summary

### 1. Pharmacy Service
```
✅ entity/SagaInstance.java
✅ repository/SagaInstanceRepository.java
✅ service/SagaInstanceService.java
✅ messaging/SagaDeadLetterQueueHandler.java
✅ config/KafkaConfig.java (updated with DLQ)
✅ messaging/SagaPharmacyListener.java (updated with idempotency)
✅ src/main/resources/db/migration/V001__create_saga_instances_table.sql
```

### 2. Medical Service
```
✅ entity/SagaInstance.java
✅ repository/SagaInstanceRepository.java
✅ service/SagaInstanceService.java
✅ messaging/SagaDeadLetterQueueHandler.java
✅ config/KafkaConfig.java (updated with DLQ)
✅ messaging/SagaMedicalListener.java (updated with idempotency)
✅ src/main/resources/db/migration/V001__create_saga_instances_table.sql
```

### 3. Payment Service
```
✅ entity/SagaInstance.java
✅ repository/SagaInstanceRepository.java
✅ service/SagaInstanceService.java
✅ messaging/SagaDeadLetterQueueHandler.java
✅ config/KafkaConfig.java (updated with DLQ)
✅ messaging/SagaPaymentListener.java (updated with idempotency)
✅ src/main/resources/db/migration/V001__create_saga_instances_table.sql
```

### 4. Documentation
```
✅ SAGA_IMPLEMENTATION_GUIDE.md (comprehensive guide)
✅ QUICK_START_GUIDE.md (this file)
```

---

## 🔧 Step 1: Build All Services

```bash
cd c:\Users\ivand\OneDrive\Documents\eai_fix_tugas_akhir

# Build each service
cd pharmacy-service && mvn clean install -DskipTests
cd ../medical-service && mvn clean install -DskipTests
cd ../payment-service && mvn clean install -DskipTests
```

---

## 🗄️ Step 2: Start Database & Kafka

```bash
# Start Kafka (if not already running)
docker-compose up -d kafka

# Wait untuk Kafka siap
docker-compose logs kafka | grep "started"
```

---

## 🚀 Step 3: Start Services

```bash
# Open 3 terminals or use tmux

# Terminal 1: Pharmacy Service
cd pharmacy-service
mvn spring-boot:run

# Terminal 2: Medical Service
cd medical-service
mvn spring-boot:run

# Terminal 3: Payment Service
cd payment-service
mvn spring-boot:run
```

**Expected Startup Logs**:
```
✅ "Flyway initialized with 1 location: file:/.../db/migration"
✅ "Created new table saga_instances"
✅ "Listened on port 8004 (pharmacy), 8002 (medical), 8003 (payment)"
```

---

## ✅ Step 4: Verify Tables Created

```bash
# Connect to database
mysql -u root -p

# Check saga_instances table exists
USE pharmacy_db;
SHOW TABLES LIKE 'saga%';

# Should show:
# | saga_instances |

# Check columns
DESC saga_instances;
```

---

## 🧪 Step 5: Test Normal Flow (Idempotency Test)

### Scenario: Send same prescription twice with same idempotency key

```bash
# Request 1: Create prescription
curl -X POST http://localhost:8000/api/medical/resep \
  -H "X-Idempotency-Key: TEST-KEY-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "pemeriksaanId": 1,
    "pasienId": 1,
    "namaObat": "Aspirin",
    "dosis": "500mg",
    "frekuensi": "2x sehari",
    "jumlah": 10,
    "catatan": "Ambil dengan air"
  }'

# Wait 2 seconds for processing...
# sleep 2

# Request 2: Send SAME request again (idempotent)
curl -X POST http://localhost:8000/api/medical/resep \
  -H "X-Idempotency-Key: TEST-KEY-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "pemeriksaanId": 1,
    "pasienId": 1,
    "namaObat": "Aspirin",
    "dosis": "500mg",
    "frekuensi": "2x sehari",
    "jumlah": 10,
    "catatan": "Ambil dengan air"
  }'

# ✅ Expected: Same response, no duplicate processing
```

### Verify Idempotency:
```bash
mysql -u root -p pharmacy_db

SELECT COUNT(*) as total_sagas 
FROM saga_instances 
WHERE message_id = 'TEST-KEY-001';

# Expected: 1 (not 2!)
```

---

## ⚡ Step 6: Test Compensation (Failure Scenario)

### Scenario: Payment fails → Stock restored

```bash
# 1. Check initial stock
mysql -u root -p pharmacy_db
SELECT * FROM obat WHERE nama_obat = 'Aspirin';
# Note: stok = X

# 2. Send prescription request
curl -X POST http://localhost:8000/api/medical/resep \
  -H "X-Idempotency-Key: TEST-KEY-002" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "pemeriksaanId": 1,
    "pasienId": 1,
    "namaObat": "Aspirin",
    "dosis": "500mg",
    "frekuensi": "2x sehari",
    "jumlah": 5,
    "catatan": "Test payment failure"
  }'

# 3. Wait for stock reservation
# sleep 3

# 4. Check stock (should be RESERVED = reduced)
SELECT stok FROM obat WHERE nama_obat = 'Aspirin';
# Expected: X - 5

# 5. Trigger payment failure (admin action or simulation)
# UPDATE tagihan SET status = 'FAILED' WHERE resep_id = <resep_id>;

# 6. Wait for compensation
# sleep 3

# 7. Check stock (should be RESTORED)
SELECT stok FROM obat WHERE nama_obat = 'Aspirin';
# Expected: X (restored!)

# 8. Verify SagaInstance status
SELECT saga_status, compensation_status FROM saga_instances 
WHERE message_id = 'TEST-KEY-002';
# Expected: COMPENSATED | EXECUTED
```

---

## 🚨 Step 7: Test DLQ (Error Scenario)

### Scenario: Process event with error → DLQ

```bash
# 1. Kill payment service temporarily
# Ctrl+C on Terminal 3

# 2. Send prescription request
curl -X POST http://localhost:8000/api/medical/resep \
  -H "X-Idempotency-Key: TEST-KEY-003" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{...}'

# 3. Restart payment service
cd payment-service
mvn spring-boot:run

# 4. Wait 5 seconds for DLQ processing
# sleep 5

# 5. Check DLQ messages
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-processed.dlq \
  --from-beginning \
  --max-messages 5

# Expected: Event message with dlqReason

# 6. Check SagaInstance marked FAILED
mysql -u root -p payment_db
SELECT * FROM saga_instances WHERE saga_status = 'FAILED';
```

---

## 📊 Monitoring Queries

### Get Saga Flow Status:
```sql
-- All active sagas
SELECT saga_id, saga_status, created_at, message_id
FROM saga_instances 
WHERE saga_status IN ('INITIATED', 'IN_PROGRESS')
ORDER BY created_at DESC;

-- Failed sagas
SELECT saga_id, error_message, retry_count
FROM saga_instances 
WHERE saga_status = 'FAILED'
ORDER BY created_at DESC;

-- Completed sagas
SELECT saga_id, completed_at
FROM saga_instances 
WHERE saga_status = 'COMPLETED'
ORDER BY completed_at DESC
LIMIT 10;

-- Compensation tracking
SELECT saga_id, saga_status, compensation_status
FROM saga_instances 
WHERE compensation_status IS NOT NULL;
```

### Check Message Idempotency:
```sql
-- Find duplicates (should be 0!)
SELECT message_id, COUNT(*) as cnt
FROM saga_instances 
GROUP BY message_id
HAVING cnt > 1;

-- Expected: Empty result
```

---

## 🔍 Logs to Watch

### Success Indicators ✅
```
"Created saga instance: 550e8400-e29b-41d4-a716-446655440000 for prescriptionId: 1"
"✅ Stock reserved for prescriptionId: 1"
"⚠️ IDEMPOTENCY: Message XXX already processed, skipping"
"✅ Saga completed successfully for prescriptionId: 1"
```

### Error Indicators ❌
```
"❌ IDEMPOTENCY VIOLATION: Duplicate message detected"
"❌ Insufficient stock for medicine: Aspirin"
"❌ Error processing prescription-created event"
"Sent event to DLQ: prescription-received.dlq"
```

---

## 📈 Key Metrics to Track

| Metric | Good | Bad |
|--------|------|-----|
| Duplicate saga_ids | 0 | >0 |
| Failed sagas | <5% | >10% |
| DLQ messages | <1% | >5% |
| Compensation success | 100% | <95% |
| Idempotency hits | 0-10% | >50% (indicates duplicate sending) |

---

## 🆘 Troubleshooting

### Issue: Table not created
```bash
# Check Flyway logs
grep "Flyway" application.log

# Manually create table
mysql -u root -p < V001__create_saga_instances_table.sql
```

### Issue: Duplicate saga entries
```bash
# Check for messageId duplicates
SELECT message_id, COUNT(*) FROM saga_instances 
GROUP BY message_id HAVING COUNT(*) > 1;

# Fix: Delete duplicates (keep only first)
# Restart service
```

### Issue: DLQ messages not being processed
```bash
# Check DLQ topic exists
docker-compose exec kafka kafka-topics --list \
  --bootstrap-server localhost:9092 | grep dlq

# Check consumer group
docker-compose exec kafka kafka-consumer-groups --list \
  --bootstrap-server localhost:9092 | grep dlq
```

### Issue: SagaInstance service not injected
```bash
# Check that @Autowired is working
# Verify SagaInstanceService has @Service annotation
# Check package scanning in Application class
```

---

## ✨ Success Checklist

- [ ] Build all 3 services successfully
- [ ] Databases created with saga_instances tables
- [ ] Services started without errors
- [ ] Idempotency test: duplicate request returns same response
- [ ] Compensation test: stock restored after payment failure
- [ ] DLQ test: failed events routed to DLQ
- [ ] Saga status queryable from database
- [ ] No duplicate saga_ids in database
- [ ] Error messages properly logged

---

## 🎓 What's Implemented

### Gap 1: Saga Instance Tracking ✅
- Full state visibility from database
- Can query saga status anytime
- Audit trail via payload_json

### Gap 2: Idempotency Keys ✅
- Duplicate message detection
- Exactly-once processing semantics
- Prevents double-charging, double-stock-deduction

### Gap 3: Dead-Letter Queue ✅
- Failed events captured
- Saga marked as FAILED
- Manual intervention possible

---

**Ready to test? Start with Step 1!** 🚀

Need help? Check `SAGA_IMPLEMENTATION_GUIDE.md` for detailed explanation.
