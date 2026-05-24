# IMPLEMENTASI MICROSERVICES GROWBUSINESS - SUMMARY

## Overview
Implementasi lengkap microservices architecture untuk sistem informasi rumah sakit RSU Sumatera dengan teknologi modern dan best practices.

## Teknologi yang Digunakan

### Framework & Platform
- **Spring Boot 4.0.5** - Application framework
- **Spring Cloud Gateway** - API Gateway untuk routing requests
- **Spring Data JPA** - Object-relational mapping
- **Spring Security** - Authentication & Authorization
- **Hibernate** - ORM framework

### Database
- **MySQL 8.0+** - Relational database (4 databases)
  - growbusiness_admin
  - growbusiness_medical
  - growbusiness_pharmacy
  - growbusiness_payment

### Message Broker
- **Apache Kafka 3.0+** - Event streaming & messaging
- **Spring Kafka** - Kafka integration dengan Spring

### Security
- **JWT (JSON Web Tokens)** - Token-based authentication
- **JJWT 0.12.3** - JWT library
- **BCrypt** - Password hashing

### Development Tools
- **Java 17** - Programming language
- **Maven 3.8+** - Build tool
- **Lombok** - Code generation utility

## Microservices Architecture

### 1. Admin Service (Port 8001)
**Purpose**: Manajemen data pasien dan user authentication
**Database**: growbusiness_admin

#### Entities:
- `Pasien` - Data pasien/patient
- `User` - Data user sistem

#### Endpoints:
```
POST   /api/auth/register          - Register user baru
POST   /api/auth/login             - Login user
POST   /api/auth/refresh           - Refresh JWT token
POST   /api/patients               - Buat pasien baru
GET    /api/patients               - List semua pasien
GET    /api/patients/{id}          - Get pasien by ID
PUT    /api/patients/{id}          - Update pasien
DELETE /api/patients/{id}          - Delete pasien
GET    /api/patients/search        - Search pasien by nama
```

#### Kafka Events Published:
- `patient-created` - Ketika pasien baru dibuat
- `patient-updated` - Ketika data pasien diupdate
- `user-registered` - Ketika user baru terdaftar
- `schedule-created` - Ketika jadwal dibuat

### 2. Medical Service (Port 8002)
**Purpose**: Manajemen data medis, pemeriksaan, dan resep
**Database**: growbusiness_medical

#### Entities:
- `Pemeriksaan` - Data pemeriksaan medis
- `Resep` - Data resep/prescription

#### Endpoints:
```
POST   /api/medical/examinations              - Buat pemeriksaan baru
GET    /api/medical/examinations/{id}         - Get pemeriksaan by ID
GET    /api/medical/examinations/patient/{id} - List pemeriksaan pasien
PUT    /api/medical/examinations/{id}         - Update pemeriksaan

POST   /api/medical/prescriptions             - Buat resep baru
GET    /api/medical/prescriptions/{id}        - Get resep by ID
GET    /api/medical/prescriptions/pending     - List resep pending
PATCH  /api/medical/prescriptions/{id}/status - Update status resep
```

#### Kafka Events Published:
- `medical-record-created` - Ketika record medis dibuat
- `prescription-created` - Ketika resep dibuat
- `diagnosis-recorded` - Ketika diagnosis dicatat

### 3. Pharmacy Service (Port 8004)
**Purpose**: Manajemen inventory obat dan stok
**Database**: growbusiness_pharmacy

#### Entities:
- `Obat` - Data obat/medicine
- `StokObat` - Transaction history untuk stok

#### Endpoints:
```
POST   /api/pharmacy/medicines              - Tambah obat baru
GET    /api/pharmacy/medicines              - List semua obat
GET    /api/pharmacy/medicines/{id}         - Get obat by ID
GET    /api/pharmacy/medicines/search       - Search obat by nama
GET    /api/pharmacy/medicines/low-stock    - List obat stok rendah
PATCH  /api/pharmacy/medicines/{id}/stock   - Update stok obat
```

#### Kafka Events Published:
- `prescription-received` - Ketika apotek menerima resep
- `stock-updated` - Ketika stok diupdate
- `stock-low-alert` - Alert ketika stok rendah

### 4. Payment Service (Port 8003)
**Purpose**: Manajemen billing, invoice, dan payment processing
**Database**: growbusiness_payment

#### Entities:
- `Tagihan` - Data tagihan/invoice
- `Transaksi` - Data transaksi pembayaran

#### Endpoints:
```
POST   /api/payment/invoices                      - Buat invoice baru
GET    /api/payment/invoices/{id}                 - Get invoice by ID
GET    /api/payment/invoices/patient/{id}         - List invoice pasien
GET    /api/payment/invoices/pending              - List invoice pending
PATCH  /api/payment/invoices/{id}/status          - Update status invoice

POST   /api/payment/transactions/process          - Proses pembayaran
GET    /api/payment/transactions/{id}             - Get transaksi by ID
GET    /api/payment/transactions/invoice/{id}     - List transaksi invoice
```

#### Kafka Events Published:
- `billing-created` - Ketika tagihan dibuat
- `payment-processed` - Ketika pembayaran diproses
- `invoice-generated` - Ketika invoice digenerate

### 5. API Gateway (Port 8000)
**Purpose**: Central routing dan JWT authentication
**Technology**: Spring Cloud Gateway

#### Routes:
```
/api/admin/**      → Admin Service (8001)
/api/medical/**    → Medical Service (8002)
/api/pharmacy/**   → Pharmacy Service (8004)
/api/payment/**    → Payment Service (8003)
```

#### Features:
- Request routing ke service yang tepat
- JWT token validation untuk semua requests
- CORS support
- Request/Response logging
- Circuit breaking (ready for Hystrix)

## Security Implementation

### JWT Authentication Flow
```
1. User -> POST /api/auth/login dengan credentials
2. Admin Service -> Validate password
3. Admin Service -> Generate JWT token
4. Client -> Menerima token
5. Client -> Include token dalam Authorization header
6. Request -> Masuk ke API Gateway
7. API Gateway -> Validate JWT token via JwtAuthenticationFilter
8. API Gateway -> Forward request ke service yang sesuai
9. Service -> Validate token lagi (double validation)
10. Response -> Kembali ke client
```

### Security Configuration
- **Stateless Authentication**: SessionCreationPolicy.STATELESS
- **Password Encoding**: BCryptPasswordEncoder dengan salt 10
- **Token Algorithm**: HS512 (HMAC SHA-512)
- **Token Expiration**: 24 hours (86400000 ms)
- **Refresh Token**: 7 days (604800000 ms)

### Role-Based Access Control (RBAC)
```java
@PreAuthorize("hasRole('ROLE_ADMIN')")
@PreAuthorize("hasRole('ROLE_DOCTOR')")
@PreAuthorize("hasRole('ROLE_PHARMACIST')")
@PreAuthorize("hasRole('ROLE_STAFF')")
@PreAuthorize("hasRole('ROLE_USER')")
```

## Kafka Event-Driven Architecture

### Topics Configuration
Semua topics dikonfigurasi dengan:
- **Partitions**: 3 (untuk scalability & parallelism)
- **Replication Factor**: 1 (production: gunakan 3)
- **Serialization**: JSON

### Event Publishing Pattern
```java
kafkaTemplate.send(TopicName, eventId, eventPayload);
```

### Event Consumption Pattern
```java
@KafkaListener(topics = "topic-name", groupId = "group-id")
public void handleEvent(EventObject event) {
    // Process event
}
```

### Event Flows
1. **Patient Registration**:
   patient-created → Medical Service subscribe → Update patient records
   
2. **Prescription Workflow**:
   prescription-created → Pharmacy Service subscribe → Receive prescription
   prescription-received → Update status
   
3. **Billing Process**:
   medical-record-created → Payment Service subscribe → Create invoice
   payment-processed → Update patient account

4. **Stock Management**:
   stock-updated → Monitor stock levels
   stock-low-alert → Admin notification

## Database Schema

### Admin Service - 2 Tables
```sql
Pasien (PRIMARY)
├── id (PK)
├── nik (UNIQUE)
├── nama
├── noRM (UNIQUE)
├── alamat
├── noTelepon
├── email (UNIQUE)
├── jenisKelamin
├── tanggalLahir
└── timestamps

Users
├── id (PK)
├── username (UNIQUE)
├── password (hashed)
├── nama
├── email (UNIQUE)
├── role
├── noIdentitas
├── is_active
└── timestamps
```

### Medical Service - 2 Tables
```sql
Pemeriksaan (PRIMARY)
├── id (PK)
├── pasienId (FK)
├── tekananDarah
├── beratBadan
├── tinggiBadan
├── suhuTubuh
├── keluhan
├── hasilPemeriksaan
├── dokterId
└── timestamps

Resep
├── id (PK)
├── pemeriksaanId (FK)
├── pasienId (FK)
├── namaObat
├── dosis
├── frekuensi
├── jumlah
├── catatan
├── status (PENDING/COMPLETED)
└── timestamps
```

### Pharmacy Service - 2 Tables
```sql
Obat (PRIMARY)
├── id (PK)
├── kodeObat (UNIQUE)
├── namaObat
├── deskripsi
├── harga
├── satuan
├── stok
├── stokMinimal
├── supplier
└── timestamps

StokObat
├── id (PK)
├── obatId (FK)
├── jumlahMasuk
├── jumlahKeluar
├── tipe (IN/OUT)
├── keterangan
└── timestamps
```

### Payment Service - 2 Tables
```sql
Tagihan (PRIMARY)
├── id (PK)
├── pasienId (FK)
├── biayaKonsultasi
├── hargaObat
├── subtotal
├── diskonAsuransi
├── pajakPPN (calculated 10%)
├── totalBayar
├── status (PENDING/PAID)
├── noInvoice (UNIQUE)
└── timestamps

Transaksi
├── id (PK)
├── tagihanId (FK)
├── jumlahBayar
├── metodePembayaran
├── statusPembayaran
├── referensiTransaksi
├── keterangan
└── timestamps
```

## Key Features Implementation

### 1. Transaction Management
- `@Transactional` pada service layer
- Rollback otomatis jika ada error
- JPA automatic flush

### 2. Error Handling
```java
try {
    // Business logic
} catch (Exception e) {
    log.error("Error: {}", e.getMessage());
    // Return error response
}
```

### 3. Logging
- SLF4J dengan Logback
- DEBUG level untuk application code
- INFO level untuk system events
- ERROR level untuk exceptions

### 4. Data Validation
```java
@Valid @RequestBody Entity entity
@RequestParam @NotNull String value
```

### 5. Response Format
```json
{
  "data": {...},
  "message": "Success message",
  "error": "Error message (if any)"
}
```

## Deployment Structure

```
Project Root/
├── admin-service/
│   ├── pom.xml (updated with Kafka, JWT, JPA, MySQL)
│   ├── src/main/
│   │   ├── java/com/example/GrowBussiness/
│   │   │   ├── security/ (JWT filters, util, config)
│   │   │   ├── entity/ (Pasien, User)
│   │   │   ├── dto/ (PasienDTO)
│   │   │   ├── repository/ (PasienRepository, UserRepository)
│   │   │   ├── service/ (PasienService, UserService)
│   │   │   ├── controller/ (AuthController, PasienController)
│   │   │   ├── config/ (KafkaConfig)
│   │   │   └── GrowBussinessApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── target/
│
├── medical-service/
│   ├── pom.xml
│   ├── src/main/
│   │   ├── java/com/example/GrowBussiness/
│   │   │   ├── security/
│   │   │   ├── entity/ (Pemeriksaan, Resep)
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── controller/
│   │   │   ├── config/ (KafkaConfig)
│   │   │   └── GrowBussinessApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── target/
│
├── pharmacy-service/
│   └── GrowBussiness/
│       ├── pom.xml
│       ├── src/main/
│       │   ├── java/com/example/GrowBussiness/
│       │   │   ├── security/
│       │   │   ├── entity/ (Obat, StokObat)
│       │   │   ├── repository/
│       │   │   ├── service/
│       │   │   ├── controller/
│       │   │   ├── config/ (KafkaConfig)
│       │   │   └── GrowBussinessApplication.java
│       │   └── resources/
│       │       └── application.properties
│       └── target/
│
├── payment-service/
│   ├── pom.xml
│   ├── src/main/
│   │   ├── java/com/example/GrowBussiness/
│   │   │   ├── security/
│   │   │   ├── entity/ (Tagihan, Transaksi)
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── controller/
│   │   │   ├── config/ (KafkaConfig)
│   │   │   └── GrowBussinessApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── target/
│
├── api-gateway/
│   ├── pom.xml
│   ├── src/main/
│   │   ├── java/com/example/GrowBussiness/
│   │   │   ├── security/ (JWT filters, util, config)
│   │   │   └── GrowBussinessApplication.java
│   │   └── resources/
│   │       └── application.properties (gateway routes)
│   └── target/
│
├── MICROSERVICES_SETUP.md (Dokumentasi lengkap)
├── startup-services.bat (Windows startup script)
├── startup-services.sh (Linux/Mac startup script)
└── Service-RSUSumatera.md (Original requirements)
```

## Build & Run

### Build All Services
```bash
mvn clean install
```

### Run Individual Service
```bash
cd admin-service
mvn spring-boot:run

# atau
java -jar target/GrowBussiness-0.0.1-SNAPSHOT.jar
```

### Automated Startup
```bash
# Windows
startup-services.bat

# Linux/Mac
chmod +x startup-services.sh
./startup-services.sh
```

## Testing API

### Postman Collection
Create collection dengan folders:
- Authentication
  - Register
  - Login
  - Refresh Token
- Admin
  - Create Patient
  - Get Patient
  - Search Patient
  - Update Patient
- Medical
  - Create Examination
  - Get Prescription
  - List Pending Prescriptions
- Pharmacy
  - Get Medicines
  - Update Stock
  - Get Low Stock
- Payment
  - Create Invoice
  - Process Payment
  - Get Invoice

### cURL Examples
Lihat file MICROSERVICES_SETUP.md untuk contoh lengkap

## Performance Considerations

### Database
- Connection pooling: HikariCP
- Batch size: 20
- Order inserts/updates enabled
- Index pada foreign keys dan search columns

### Kafka
- Partitions: 3
- Consumer group per service
- Manual acknowledge for reliability

### JWT
- Stateless authentication (no session storage)
- Fast token validation
- Short-lived tokens (24 hours)

## Future Enhancements

1. **API Documentation**: Swagger/SpringDoc OpenAPI
2. **Monitoring**: Prometheus + Grafana
3. **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
4. **Tracing**: Sleuth + Zipkin
5. **Circuit Breaking**: Spring Cloud Hystrix
6. **Caching**: Redis
7. **Containerization**: Docker
8. **Orchestration**: Kubernetes
9. **Service Discovery**: Eureka
10. **Config Server**: Spring Cloud Config

## Files Modified/Created

### Modified
- admin-service/pom.xml
- medical-service/pom.xml
- pharmacy-service/GrowBussiness/pom.xml
- payment-service/pom.xml
- api-gateway/pom.xml
- All application.properties files

### Created (Total: 50+ files)
- Security classes (JWT util, filters, config)
- Entity classes (Pasien, User, Pemeriksaan, etc.)
- Repository classes
- Service classes
- Controller classes
- Kafka configuration
- Startup scripts
- Documentation

## Summary Statistics

| Component | Count |
|-----------|-------|
| Microservices | 5 |
| Entities | 6 |
| Repositories | 6 |
| Services | 8 |
| Controllers | 6 |
| Kafka Topics | 12 |
| Database Tables | 8 |
| API Endpoints | 25+ |
| Security Classes | 12 |
| Total Classes | 50+ |

## Kesimpulan

Implementasi ini menyediakan:
✅ Microservices architecture yang scalable
✅ JWT-based authentication & authorization
✅ Event-driven design dengan Kafka
✅ RESTful API endpoints
✅ Database schema yang normalized
✅ Transaction management
✅ Error handling & logging
✅ RBAC dengan Spring Security
✅ Automated deployment scripts
✅ Comprehensive documentation

Sistem siap untuk production dengan minor improvements:
- Add Docker containerization
- Setup Kubernetes orchestration
- Add API documentation (Swagger)
- Implement monitoring & alerting
- Add distributed tracing

---

**Last Updated**: May 2026
**Status**: ✅ COMPLETE & READY FOR TESTING
**Author**: GrowBusiness Development Team
