# 🔐 Admin Service - Authentication & Testing Guide

## 📌 Informasi Penting

**Status**: ✅ Admin Service memerlukan JWT token untuk akses ke endpoint yang protected
- ✅ `POST /api/auth/**` → **PUBLIC** (Tidak perlu token)
- ✅ `GET /api/patients/**`, `POST /api/patients/**`, etc → **PROTECTED** (Harus ada token)
- ✅ Error 401 yang Anda dapat adalah **expected behavior**

---

## 🔑 Public Endpoints (Tidak Perlu Token)

### 1. **Register User**
```bash
curl -X POST http://localhost:8001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin1",
    "password": "password123",
    "nama": "Admin User",
    "email": "admin@example.com",
    "role": "ADMIN"
  }'
```

**Response (201 Created)**:
```json
{
  "message": "User registered successfully",
  "username": "admin1"
}
```

---

### 2. **Login & Get JWT Token**
```bash
curl -X POST http://localhost:8001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin1",
    "password": "password123"
  }'
```

**Response (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin1",
  "message": "Login successful"
}
```

**💾 Simpan token ini untuk step selanjutnya!**

---

### 3. **Refresh JWT Token**
```bash
curl -X POST http://localhost:8001/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin1"
  }'
```

---

## 🛡️ Protected Endpoints (Perlu JWT Token)

### Using the Token in Requests

Gunakan token dari login response dalam `Authorization` header dengan format `Bearer <token>`:

```bash
curl -X GET http://localhost:8001/api/patients \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 1. **Get All Patients**
```bash
curl -X GET http://localhost:8001/api/patients \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Expected Response (200 OK)**:
```json
{
  "data": [
    {
      "id": 1,
      "nama": "John Doe",
      "nomorIdentitas": "1234567890",
      "alamat": "Jl. Example",
      "noTelepon": "081234567890",
      "email": "john@example.com"
    }
  ],
  "count": 1
}
```

---

### 2. **Get Patient by ID**
```bash
curl -X GET http://localhost:8001/api/patients/1 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

### 3. **Create New Patient** (Requires ADMIN/DOCTOR/STAFF role)
```bash
curl -X POST http://localhost:8001/api/patients \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "nama": "Jane Smith",
    "nomorIdentitas": "9876543210",
    "alamat": "Jl. Another Street",
    "noTelepon": "089876543210",
    "email": "jane@example.com"
  }'
```

**Expected Response (201 Created)**:
```json
{
  "message": "Patient created successfully",
  "data": {
    "id": 2,
    "nama": "Jane Smith",
    ...
  }
}
```

---

### 4. **Update Patient** (Requires ADMIN/DOCTOR/STAFF role)
```bash
curl -X PUT http://localhost:8001/api/patients/2 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "nama": "Jane Smith Updated",
    "nomorIdentitas": "9876543210",
    "alamat": "Jl. Updated Street",
    "noTelepon": "089876543210",
    "email": "jane.updated@example.com"
  }'
```

---

### 5. **Search Patient by Name**
```bash
curl -X GET "http://localhost:8001/api/patients/search?nama=Jane" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

### 6. **Delete Patient** (Requires ADMIN/DOCTOR/STAFF role)
```bash
curl -X DELETE http://localhost:8001/api/patients/2 \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## 🧪 Complete Testing Workflow

### Step 1: Register & Login
```bash
# Register new user
curl -X POST http://localhost:8001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testadmin",
    "password": "test123456",
    "nama": "Test Admin",
    "email": "testadmin@example.com",
    "role": "ADMIN"
  }'

# Login to get token (SIMPAN TOKEN!)
TOKEN=$(curl -s -X POST http://localhost:8001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testadmin",
    "password": "test123456"
  }' | jq -r '.token')

echo "Token: $TOKEN"
```

### Step 2: Create Patient
```bash
curl -X POST http://localhost:8001/api/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nama": "Pasien Test",
    "nomorIdentitas": "1111111111",
    "alamat": "Jl. Testing",
    "noTelepon": "082222222222",
    "email": "pasien@test.com"
  }'
```

### Step 3: Get All Patients
```bash
curl -X GET http://localhost:8001/api/patients \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🔴 Error Codes & Solutions

### ❌ 401 Unauthorized
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/error"
}
```

**Penyebab & Solusi**:
1. ❌ Tidak ada token → Gunakan `/api/auth/login` untuk login terlebih dahulu
2. ❌ Token expired → Gunakan `/api/auth/refresh` untuk refresh token
3. ❌ Token tidak valid → Pastikan format header: `Authorization: Bearer TOKEN`
4. ❌ Token invalid/corrupt → Login lagi dan dapatkan token baru

### ❌ 403 Forbidden
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied"
}
```

**Penyebab & Solusi**:
- User tidak memiliki role yang diperlukan (contoh: bukan ADMIN, DOCTOR, atau STAFF)
- Gunakan user dengan role ADMIN untuk testing

### ❌ 404 Not Found
- Resource tidak ditemukan (contoh: patient dengan ID yang tidak ada)

### ❌ 400 Bad Request
- Data input tidak valid (format email salah, field wajib kosong, dll)

---

## 📝 Swagger UI Testing

Untuk testing yang lebih mudah dengan UI, buka Swagger:

**URL**: http://localhost:8001/swagger-ui.html

### Langkah-langkah:
1. Buka Swagger UI di browser
2. Cari endpoint `/api/auth/login`
3. Klik "Try it out"
4. Input username & password
5. Klik "Execute"
6. Copy token dari response
7. Klik tombol "Authorize" di atas
8. Paste token dengan format: `Bearer YOUR_TOKEN`
9. Sekarang semua endpoint protected bisa di-test

---

## 🔧 Konfigurasi Security

File: `admin-service/src/main/java/com/example/adminservice/security/SecurityConfig.java`

**Public Endpoints** (`.permitAll()`):
```java
.requestMatchers("/api/auth/**").permitAll()           // Login, Register, Refresh
.requestMatchers("/health", "/actuator/**").permitAll() // Health check
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // Documentation
```

**Protected Endpoints** (`.authenticated()`):
```java
.anyRequest().authenticated()  // Semua endpoint lain butuh token
```

---

## 💡 Tips

- 🔑 Simpan token yang didapat dari login untuk testing endpoint protected
- ⏰ Token berlaku 24 jam (configurable di `jwt.expiration`)
- 🔄 Gunakan `/api/auth/refresh` untuk mendapatkan token baru tanpa login ulang
- 📋 Dalam Postman, gunakan "Bearer" auth type dan paste token
- 🛡️ Jangan share token ke orang lain, token itu seperti password

---

## 📚 Database Schema

### User Table
```sql
-- users table
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  nama VARCHAR(100),
  email VARCHAR(100),
  role VARCHAR(20) DEFAULT 'USER',
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Example test user (password: password123, encrypted)
INSERT INTO users (username, password, nama, email, role, is_active)
VALUES ('admin', '$2a$10$...encrypted_password...', 'Admin User', 'admin@example.com', 'ADMIN', true);
```

---

## 🚀 Troubleshooting Checklist

- [ ] Admin Service running di port 8001
- [ ] Database terkoneksi (check logs)
- [ ] `/api/auth/login` return token (bukan 401)
- [ ] Token di-copy dengan benar
- [ ] Header format: `Authorization: Bearer TOKEN` (ada spasi!)
- [ ] Token belum expired (login ulang jika perlu)
- [ ] User punya role yang diperlukan untuk endpoint tertentu

---

## 📞 Support

Jika masih error 401 setelah mengikuti langkah ini:
1. Periksa console logs (ada error apa?)
2. Pastikan database terkoneksi
3. Coba register user baru dan login lagi
4. Restart admin-service

