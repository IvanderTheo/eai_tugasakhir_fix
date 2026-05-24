# API Testing Guide - GrowBusiness Microservices

Panduan lengkap untuk testing semua API endpoints menggunakan curl commands.

## Prerequisites

- Semua services berjalan (lihat startup-services.bat atau startup-services.sh)
- Kafka dan MySQL berjalan
- API Gateway accessible di http://localhost:8000

## Variables

```bash
BASE_URL=http://localhost:8000
TOKEN=<YOUR_JWT_TOKEN_HERE>
```

## 1. AUTHENTICATION ENDPOINTS

### 1.1 Register User
```bash
curl -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin123&nama=Admin User&email=admin@test.com&role=ADMIN"
```

Response Success:
```json
{
  "message": "User registered successfully",
  "user": {
    "id": 1,
    "username": "admin",
    "nama": "Admin User",
    "email": "admin@test.com",
    "role": "ADMIN"
  }
}
```

### 1.2 Register Doctor
```bash
curl -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=dokter1&password=pass123&nama=Dr. Budi&email=budi@test.com&role=DOCTOR"
```

### 1.3 Register Pharmacist
```bash
curl -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=apt1&password=pass123&nama=Apt. Siti&email=apt@test.com&role=PHARMACIST"
```

### 1.4 Register Staff
```bash
curl -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=staff1&password=pass123&nama=Staff&email=staff@test.com&role=STAFF"
```

### 1.5 Login User
```bash
curl -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin123"
```

Response Success:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcxNjM0NTYwMH0...",
  "username": "admin",
  "message": "Login successful"
}
```

### 1.6 Refresh Token
```bash
# Gunakan token yang didapat dari login
curl -X POST "${BASE_URL}/api/auth/refresh" \
  -H "Authorization: Bearer ${TOKEN}"
```

## 2. ADMIN SERVICE - PATIENT MANAGEMENT

### 2.1 Create Patient
```bash
curl -X POST "${BASE_URL}/api/admin/patients" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "nik": "3273010101900001",
    "nama": "Budi Santoso",
    "noRM": "RM001",
    "alamat": "Jln Merdeka No 1, Medan",
    "noTelepon": "081234567890",
    "email": "budi@example.com",
    "jenisKelamin": "LAKI-LAKI",
    "tanggalLahir": "1990-01-15"
  }'
```

### 2.2 Get All Patients
```bash
curl -X GET "${BASE_URL}/api/admin/patients" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 2.3 Get Patient by ID
```bash
curl -X GET "${BASE_URL}/api/admin/patients/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 2.4 Search Patients by Name
```bash
curl -X GET "${BASE_URL}/api/admin/patients/search?nama=Budi" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 2.5 Update Patient
```bash
curl -X PUT "${BASE_URL}/api/admin/patients/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "nik": "3273010101900001",
    "nama": "Budi Santoso Updated",
    "noRM": "RM001",
    "alamat": "Jln Merdeka No 2, Medan",
    "noTelepon": "082234567890",
    "email": "budi.updated@example.com",
    "jenisKelamin": "LAKI-LAKI",
    "tanggalLahir": "1990-01-15"
  }'
```

### 2.6 Delete Patient
```bash
curl -X DELETE "${BASE_URL}/api/admin/patients/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

## 3. MEDICAL SERVICE - EXAMINATIONS

### 3.1 Create Medical Examination
```bash
curl -X POST "${BASE_URL}/api/medical/examinations" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "pasienId": 1,
    "tekananDarah": "120/80",
    "beratBadan": 70.5,
    "tinggiBadan": 175.0,
    "suhuTubuh": 36.5,
    "keluhan": "Sakit kepala ringan",
    "hasilPemeriksaan": "Hasil pemeriksaan normal",
    "dokterId": "DOK001"
  }'
```

### 3.2 Get Examination by ID
```bash
curl -X GET "${BASE_URL}/api/medical/examinations/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 3.3 Get Examinations for Patient
```bash
curl -X GET "${BASE_URL}/api/medical/examinations/patient/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 3.4 Update Examination
```bash
curl -X PUT "${BASE_URL}/api/medical/examinations/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "pasienId": 1,
    "tekananDarah": "125/85",
    "beratBadan": 71.0,
    "tinggiBadan": 175.0,
    "suhuTubuh": 36.8,
    "keluhan": "Sakit kepala ringan, agak membaik",
    "hasilPemeriksaan": "Hasil pemeriksaan membaik",
    "dokterId": "DOK001"
  }'
```

## 4. MEDICAL SERVICE - PRESCRIPTIONS

### 4.1 Create Prescription
```bash
curl -X POST "${BASE_URL}/api/medical/prescriptions" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "pemeriksaanId": 1,
    "pasienId": 1,
    "namaObat": "Paracetamol",
    "dosis": "500mg",
    "frekuensi": "3x sehari",
    "jumlah": 10,
    "catatan": "Minum setelah makan",
    "dokterNama": "Dr. Budi"
  }'
```

### 4.2 Get Prescription by ID
```bash
curl -X GET "${BASE_URL}/api/medical/prescriptions/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 4.3 Get Pending Prescriptions
```bash
curl -X GET "${BASE_URL}/api/medical/prescriptions/pending" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 4.4 Update Prescription Status
```bash
curl -X PATCH "${BASE_URL}/api/medical/prescriptions/1/status?status=COMPLETED" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

## 5. PHARMACY SERVICE - MEDICINES

### 5.1 Create Medicine
```bash
curl -X POST "${BASE_URL}/api/pharmacy/medicines" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "kodeObat": "OB001",
    "namaObat": "Paracetamol 500mg",
    "deskripsi": "Analgesik dan antipiretik",
    "harga": 5000,
    "satuan": "tablet",
    "stok": 100,
    "stokMinimal": 20,
    "supplier": "PT Kimia Farma"
  }'
```

### 5.2 Get All Medicines
```bash
curl -X GET "${BASE_URL}/api/pharmacy/medicines" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 5.3 Get Medicine by ID
```bash
curl -X GET "${BASE_URL}/api/pharmacy/medicines/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 5.4 Search Medicines by Name
```bash
curl -X GET "${BASE_URL}/api/pharmacy/medicines/search?nama=Paracetamol" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 5.5 Get Low Stock Medicines
```bash
curl -X GET "${BASE_URL}/api/pharmacy/medicines/low-stock" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 5.6 Update Medicine Stock
```bash
curl -X PATCH "${BASE_URL}/api/pharmacy/medicines/1/stock?stok=150" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

## 6. PAYMENT SERVICE - INVOICES

### 6.1 Create Invoice
```bash
curl -X POST "${BASE_URL}/api/payment/invoices" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "pasienId": 1,
    "biayaKonsultasi": 150000,
    "hargaObat": 50000,
    "diskonAsuransi": 20000
  }'
```

Response:
```json
{
  "message": "Invoice created successfully",
  "data": {
    "id": 1,
    "noInvoice": "INV-ABC123DE",
    "pasienId": 1,
    "biayaKonsultasi": 150000,
    "hargaObat": 50000,
    "subtotal": 200000,
    "diskonAsuransi": 20000,
    "pajakPPN": 18000,
    "totalBayar": 198000,
    "status": "PENDING"
  }
}
```

### 6.2 Get Invoice by ID
```bash
curl -X GET "${BASE_URL}/api/payment/invoices/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 6.3 Get Invoices for Patient
```bash
curl -X GET "${BASE_URL}/api/payment/invoices/patient/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 6.4 Get Pending Invoices
```bash
curl -X GET "${BASE_URL}/api/payment/invoices/pending" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 6.5 Update Invoice Status
```bash
curl -X PATCH "${BASE_URL}/api/payment/invoices/1/status?status=PAID" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

## 7. PAYMENT SERVICE - TRANSACTIONS

### 7.1 Process Payment
```bash
curl -X POST "${BASE_URL}/api/payment/transactions/process" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "tagihanId": 1,
    "jumlahBayar": 198000,
    "metodePembayaran": "TUNAI",
    "referensiTransaksi": "TRX20260515001"
  }'
```

Response:
```json
{
  "message": "Payment processed successfully",
  "data": {
    "id": 1,
    "tagihanId": 1,
    "jumlahBayar": 198000,
    "metodePembayaran": "TUNAI",
    "statusPembayaran": "COMPLETED",
    "referensiTransaksi": "TRX20260515001"
  }
}
```

### 7.2 Get Transaction by ID
```bash
curl -X GET "${BASE_URL}/api/payment/transactions/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

### 7.3 Get Transactions for Invoice
```bash
curl -X GET "${BASE_URL}/api/payment/transactions/invoice/1" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

## 8. COMPLETE WORKFLOW EXAMPLE

### Skenario: Patient Check-up dengan Resep dan Pembayaran

#### Step 1: Register Admin User
```bash
ADMIN_TOKEN=$(curl -s -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin123&nama=Admin&email=admin@test.com&role=ADMIN" | jq -r '.token')
```

#### Step 2: Register Doctor
```bash
DOCTOR_TOKEN=$(curl -s -X POST "${BASE_URL}/api/auth/register" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=dokter1&password=pass123&nama=Dr. Budi&email=budi@test.com&role=DOCTOR" | jq -r '.token')
```

#### Step 3: Login Admin
```bash
ADMIN_TOKEN=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin123" | jq -r '.token')
```

#### Step 4: Create Patient
```bash
PATIENT_ID=$(curl -s -X POST "${BASE_URL}/api/admin/patients" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "nik": "3273010101900001",
    "nama": "Budi Santoso",
    "noRM": "RM001",
    "alamat": "Jln Merdeka No 1",
    "noTelepon": "081234567890",
    "email": "budi@example.com",
    "jenisKelamin": "LAKI-LAKI",
    "tanggalLahir": "1990-01-15"
  }' | jq -r '.data.id')
```

#### Step 5: Doctor Creates Examination
```bash
EXAM_ID=$(curl -s -X POST "${BASE_URL}/api/medical/examinations" \
  -H "Authorization: Bearer ${DOCTOR_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"pasienId\": ${PATIENT_ID},
    \"tekananDarah\": \"120/80\",
    \"beratBadan\": 70.5,
    \"tinggiBadan\": 175.0,
    \"suhuTubuh\": 36.5,
    \"keluhan\": \"Sakit kepala\",
    \"hasilPemeriksaan\": \"Pemeriksaan normal\",
    \"dokterId\": \"DOK001\"
  }" | jq -r '.data.id')
```

#### Step 6: Doctor Creates Prescription
```bash
PRESCRIPTION_ID=$(curl -s -X POST "${BASE_URL}/api/medical/prescriptions" \
  -H "Authorization: Bearer ${DOCTOR_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"pemeriksaanId\": ${EXAM_ID},
    \"pasienId\": ${PATIENT_ID},
    \"namaObat\": \"Paracetamol 500mg\",
    \"dosis\": \"500mg\",
    \"frekuensi\": \"3x sehari\",
    \"jumlah\": 10,
    \"catatan\": \"Setelah makan\",
    \"dokterNama\": \"Dr. Budi\"
  }" | jq -r '.data.id')
```

#### Step 7: Staff Creates Invoice
```bash
INVOICE_ID=$(curl -s -X POST "${BASE_URL}/api/payment/invoices" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"pasienId\": ${PATIENT_ID},
    \"biayaKonsultasi\": 150000,
    \"hargaObat\": 50000,
    \"diskonAsuransi\": 20000
  }" | jq -r '.data.id')
```

#### Step 8: Staff Processes Payment
```bash
curl -s -X POST "${BASE_URL}/api/payment/transactions/process" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"tagihanId\": ${INVOICE_ID},
    \"jumlahBayar\": 198000,
    \"metodePembayaran\": \"TUNAI\",
    \"referensiTransaksi\": \"TRX20260515001\"
  }"
```

## Tips & Tricks

### 1. Save Token ke Variable
```bash
TOKEN=$(curl -s -X POST "http://localhost:8000/api/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin123" | jq -r '.token')
```

### 2. Pretty Print JSON Response
```bash
curl ... | jq .
```

### 3. Extract Specific Field
```bash
curl ... | jq '.data.id'
```

### 4. Check HTTP Status Code
```bash
curl -s -o /dev/null -w "%{http_code}" -X GET "http://localhost:8000/api/admin/patients" \
  -H "Authorization: Bearer ${TOKEN}"
```

### 5. Save Response to File
```bash
curl ... > response.json
```

## Common Errors & Solutions

### 401 Unauthorized
- Token expired: request refresh token
- Token invalid: login again
- Insufficient privileges: check role

### 404 Not Found
- Check if resource ID exists
- Verify endpoint path
- Check typos in URL

### 400 Bad Request
- Invalid request body
- Missing required fields
- Wrong data types

### 500 Internal Server Error
- Check service logs
- Verify database connection
- Ensure Kafka is running

## Testing Tools

### Postman
1. Import collection dari API documentation
2. Set environment variables
3. Run requests dengan built-in testing

### Thunder Client (VS Code Extension)
1. Install extension
2. Create requests directly in VS Code
3. Share .json files with team

### Insomnia
1. Free alternative to Postman
2. Request chaining
3. Environment variables

---

**Happy Testing!** 🚀
