# GrowBusiness Microservices Architecture

Implementasi microservices lengkap untuk sistem informasi rumah sakit RSU Sumatera dengan Kafka, MySQL, JWT, dan Spring Cloud Gateway.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway (Port 8000)                   │
│                      Spring Cloud Gateway                         │
│                    + JWT Authentication                           │
└─────────────────────────────────────────────────────────────────┘
       │                    │                    │                │
       ▼                    ▼                    ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│Admin Service │  │Medical Service│  │Pharmacy Svc │  │Payment Service│
│  (Port 8001) │  │  (Port 8002)  │  │ (Port 8004) │  │ (Port 8003)  │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
│  MySQL DB    │  │  MySQL DB    │  │  MySQL DB    │  │  MySQL DB    │
│ admin schema │  │ medical sema │  │pharmacy schem│  │payment schema│
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘

                    ┌──────────────────────────┐
                    │   Kafka Message Broker   │
                    │   (localhost:9092)       │
                    └──────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
   Topic Groups:      
   - patient-created
   - patient-updated
   - medical-record-created
   - prescription-created
   - billing-created
   - payment-processed
   - stock-updated
   - stock-low-alert
```

## Services Details

### 1. Admin Service (Port 8001)
**Database**: growbusiness_admin
**Endpoints**:
- `/api/auth/register` - Register user
- `/api/auth/login` - User login
- `/api/patients` - CRUD operations for patients
- `/api/patients/search` - Search patients

### 2. Medical Service (Port 8002)
**Database**: growbusiness_medical
**Endpoints**:
- `/api/medical/examinations` - Medical examination records
- `/api/medical/prescriptions` - E-Prescription management
- `/api/medical/prescriptions/pending` - Get pending prescriptions

### 3. Pharmacy Service (Port 8004)
**Database**: growbusiness_pharmacy
**Endpoints**:
- `/api/pharmacy/medicines` - Medicine inventory management
- `/api/pharmacy/medicines/low-stock` - Monitor low stock items
- `/api/pharmacy/medicines/{id}/stock` - Update stock

### 4. Payment Service (Port 8003)
**Database**: growbusiness_payment
**Endpoints**:
- `/api/payment/invoices` - Invoice/Billing management
- `/api/payment/transactions/process` - Payment processing
- `/api/payment/invoices/pending` - Get pending invoices

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.8+
- MySQL 8.0+
- Kafka 3.0+
- Windows/Linux/Mac

### 1. Database Setup

```sql
-- Create databases
CREATE DATABASE IF NOT EXISTS growbusiness_admin;
CREATE DATABASE IF NOT EXISTS growbusiness_medical;
CREATE DATABASE IF NOT EXISTS growbusiness_pharmacy;
CREATE DATABASE IF NOT EXISTS growbusiness_payment;
```

### 2. Install Kafka (Windows)

```powershell
# Download and extract Kafka
# Set KAFKA_HOME environment variable

# Start Zookeeper (in one terminal)
.\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties

# Start Kafka Server (in another terminal)
.\bin\windows\kafka-server-start.bat .\config\server.properties
```

### 3. Build and Run Services

```bash
# From workspace root
cd admin-service
mvn clean install
mvn spring-boot:run

# In new terminal
cd medical-service
mvn clean install
mvn spring-boot:run

# In new terminal
cd pharmacy-service\GrowBussiness
mvn clean install
mvn spring-boot:run

# In new terminal
cd payment-service
mvn clean install
mvn spring-boot:run

# In new terminal
cd api-gateway
mvn clean install
mvn spring-boot:run
```

## API Usage Examples

### 1. Register User

```bash
curl -X POST "http://localhost:8000/api/auth/register?username=dokter1&password=pass123&nama=Dr.Budi&email=budi@test.com&role=DOCTOR"
```

### 2. Login

```bash
curl -X POST "http://localhost:8000/api/auth/login?username=dokter1&password=pass123"
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "username": "dokter1",
  "message": "Login successful"
}
```

### 3. Create Patient

```bash
curl -X POST "http://localhost:8000/api/admin/patients" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "nik": "1234567890123456",
    "nama": "Budi Santoso",
    "noRM": "RM001",
    "alamat": "Jln Merdeka No 1",
    "noTelepon": "081234567890",
    "email": "budi@example.com",
    "jenisKelamin": "LAKI-LAKI",
    "tanggalLahir": "1990-01-15"
  }'
```

### 4. Create Medical Examination

```bash
curl -X POST "http://localhost:8000/api/medical/examinations" \
  -H "Authorization: Bearer <TOKEN>" \
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

### 5. Create Prescription

```bash
curl -X POST "http://localhost:8000/api/medical/prescriptions" \
  -H "Authorization: Bearer <TOKEN>" \
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

### 6. Update Stock

```bash
curl -X PATCH "http://localhost:8000/api/pharmacy/medicines/1/stock?stok=100" \
  -H "Authorization: Bearer <TOKEN>"
```

### 7. Create Invoice

```bash
curl -X POST "http://localhost:8000/api/payment/invoices" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "pasienId": 1,
    "biayaKonsultasi": 150000,
    "hargaObat": 50000,
    "diskonAsuransi": 20000
  }'
```

### 8. Process Payment

```bash
curl -X POST "http://localhost:8000/api/payment/transactions/process" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "tagihanId": 1,
    "jumlahBayar": 180000,
    "metodePembayaran": "TUNAI",
    "referensiTransaksi": "TRX001"
  }'
```

## Security

### JWT Token Configuration
- **Secret Key**: GrowBussinessSecretKeyForJWTTokenGenerationAndValidation1234567890
- **Expiration**: 24 hours (86400000 ms)
- **Refresh Token**: 7 days (604800000 ms)
- **Algorithm**: HS512

### Role-Based Access Control
- `ROLE_ADMIN` - Full access
- `ROLE_DOCTOR` - Medical operations
- `ROLE_PHARMACIST` - Pharmacy operations
- `ROLE_STAFF` - General staff operations
- `ROLE_USER` - Basic user operations

## Kafka Topics & Events

### Patient Events
- `patient-created` - Event saat pasien baru terdaftar
- `patient-updated` - Event saat data pasien diupdate

### Medical Events
- `medical-record-created` - Event saat record medis dibuat
- `prescription-created` - Event saat resep dibuat
- `diagnosis-recorded` - Event saat diagnosis dicatat

### Pharmacy Events
- `prescription-received` - Event saat apotek menerima resep
- `stock-updated` - Event saat stok diupdate
- `stock-low-alert` - Alert ketika stok rendah

### Payment Events
- `billing-created` - Event saat tagihan dibuat
- `payment-processed` - Event saat pembayaran diproses
- `invoice-generated` - Event saat invoice digenerate

## Database Schemas

### Admin Service Tables
```sql
-- Patients
CREATE TABLE pasien (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nik VARCHAR(20) UNIQUE NOT NULL,
  nama VARCHAR(100) NOT NULL,
  noRM VARCHAR(20) UNIQUE NOT NULL,
  alamat TEXT,
  noTelepon VARCHAR(20),
  email VARCHAR(100) UNIQUE,
  jenisKelamin VARCHAR(20),
  tanggalLahir VARCHAR(20),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Users
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  nama VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE,
  role VARCHAR(50),
  noIdentitas VARCHAR(20),
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Medical Service Tables
```sql
-- Examinations
CREATE TABLE pemeriksaan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pasienId BIGINT NOT NULL,
  tekananDarah VARCHAR(20),
  beratBadan DOUBLE,
  tinggiBadan DOUBLE,
  suhuTubuh DOUBLE,
  keluhan TEXT,
  hasilPemeriksaan TEXT,
  dokterId VARCHAR(50),
  tanggal_pemeriksaan TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Prescriptions
CREATE TABLE resep (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pemeriksaanId BIGINT NOT NULL,
  pasienId BIGINT NOT NULL,
  namaObat VARCHAR(100),
  dosis VARCHAR(50),
  frekuensi VARCHAR(50),
  jumlah INT,
  catatan TEXT,
  status VARCHAR(20) DEFAULT 'PENDING',
  dokterNama VARCHAR(100),
  tanggal_resep TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Pharmacy Service Tables
```sql
-- Medicines
CREATE TABLE obat (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  kodeObat VARCHAR(50) UNIQUE NOT NULL,
  namaObat VARCHAR(100) NOT NULL,
  deskripsi TEXT,
  harga DOUBLE,
  satuan VARCHAR(20),
  stok INT,
  stokMinimal INT,
  supplier VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Stock Transactions
CREATE TABLE stok_obat (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  obatId BIGINT NOT NULL,
  jumlahMasuk INT,
  jumlahKeluar INT,
  tipe VARCHAR(20),
  keterangan TEXT,
  tanggal_transaksi TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Payment Service Tables
```sql
-- Invoices
CREATE TABLE tagihan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pasienId BIGINT NOT NULL,
  biayaKonsultasi DOUBLE,
  hargaObat DOUBLE,
  subtotal DOUBLE,
  diskonAsuransi DOUBLE,
  pajakPPN DOUBLE,
  totalBayar DOUBLE,
  status VARCHAR(20) DEFAULT 'PENDING',
  noInvoice VARCHAR(50),
  tanggal_tagihan TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Transactions
CREATE TABLE transaksi (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tagihanId BIGINT NOT NULL,
  jumlahBayar DOUBLE,
  metodePembayaran VARCHAR(50),
  statusPembayaran VARCHAR(20) DEFAULT 'PENDING',
  referensiTransaksi VARCHAR(100),
  keterangan TEXT,
  tanggal_transaksi TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Troubleshooting

### Port Already in Use
```powershell
# Find process using port (e.g., 9092 for Kafka)
netstat -ano | findstr :9092
taskkill /PID <PID> /F
```

### Kafka Connection Issues
- Ensure Kafka and Zookeeper are running
- Check Kafka configuration in application.properties
- Verify bootstrap servers address

### JWT Token Expired
- Request new token using `/api/auth/refresh` endpoint
- Token expires after 24 hours

### Database Connection Failed
- Check MySQL is running
- Verify database credentials in application.properties
- Ensure databases exist

## Dependencies

### Core
- Spring Boot 4.0.5
- Spring Data JPA
- Spring Security
- Spring Cloud Gateway

### Database
- MySQL Connector J 8.3.0
- Hibernate

### Message Queue
- Spring Kafka
- Apache Kafka

### Security
- JJWT 0.12.3
- BCrypt

### Utilities
- Lombok
- Jackson

## Performance Optimization Tips

1. **Token Caching**: JWT tokens are stateless, no session storage needed
2. **Database Indexing**: Create indexes on frequently queried columns (NIK, email, status)
3. **Kafka Partitioning**: 3 partitions per topic for scalability
4. **Batch Processing**: Hibernate batch_size=20 for bulk operations
5. **Connection Pooling**: HikariCP used by default

## Monitoring & Logging

- All services log to console at DEBUG level
- JWT authentication logged in security module
- Kafka message production/consumption logged
- Database queries logged when enabled

## Next Steps

1. Configure Elasticsearch for centralized logging
2. Add API documentation with Swagger/SpringDoc
3. Implement distributed tracing with Sleuth + Zipkin
4. Add circuit breaker pattern with Spring Cloud Hystrix
5. Implement caching with Redis
6. Add containerization with Docker
7. Setup Kubernetes for orchestration

---

**Version**: 1.0.0
**Last Updated**: May 2026
**Author**: GrowBusiness Development Team
