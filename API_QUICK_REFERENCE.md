# Admin Service - Quick Reference for API Testing

## 🚀 3-Step Quick Start

### Step 1: Register User (if not exists)
```bash
curl -X POST http://localhost:8001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "nama": "Admin User",
    "email": "admin@example.com",
    "role": "ADMIN"
  }'
```

### Step 2: Login & Get Token
```bash
curl -X POST http://localhost:8001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6W3siYXV0aG9yaXR5IjoiUk9MRV9BRE1JTiJ9XSwic3ViIjoiYWRtaW4iLCJpYXQiOjE3MTYwMDAwMDAsImV4cCI6MTcxNjA4NjQwMH0.xxx",
  "username": "admin",
  "message": "Login successful"
}
```

### Step 3: Use Token in Requests
```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X GET http://localhost:8001/api/patients \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📍 All Endpoints

### Authentication (Public - No Token)

| Method | Endpoint | Body | Notes |
|--------|----------|------|-------|
| POST | `/api/auth/register` | `{username, password, nama, email, role}` | Register new user |
| POST | `/api/auth/login` | `{username, password}` | Get JWT token |
| POST | `/api/auth/refresh` | `{username}` | Refresh token (no password) |

### Patients (Protected - Token Required)

| Method | Endpoint | Notes | Roles |
|--------|----------|-------|-------|
| GET | `/api/patients` | Get all patients | ADMIN, DOCTOR, STAFF |
| GET | `/api/patients/{id}` | Get patient by ID | ADMIN, DOCTOR, STAFF |
| GET | `/api/patients/search?nama=NAME` | Search by name | ADMIN, DOCTOR, STAFF |
| POST | `/api/patients` | Create patient | ADMIN, DOCTOR, STAFF |
| PUT | `/api/patients/{id}` | Update patient | ADMIN, DOCTOR, STAFF |
| DELETE | `/api/patients/{id}` | Delete patient | ADMIN, DOCTOR, STAFF |

---

## 🧪 Common Test Scenarios

### Scenario 1: Get All Patients
```bash
TOKEN="your_token_here"

curl -X GET http://localhost:8001/api/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | jq
```

### Scenario 2: Create Patient
```bash
TOKEN="your_token_here"

curl -X POST http://localhost:8001/api/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nama": "John Doe",
    "nomorIdentitas": "1234567890",
    "alamat": "Jl. Main Street No. 1",
    "noTelepon": "081234567890",
    "email": "john@example.com"
  }' | jq
```

### Scenario 3: Search Patient
```bash
TOKEN="your_token_here"

curl -X GET "http://localhost:8001/api/patients/search?nama=John" \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Scenario 4: Update Patient
```bash
TOKEN="your_token_here"

curl -X PUT http://localhost:8001/api/patients/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nama": "Jane Doe",
    "nomorIdentitas": "1234567890",
    "alamat": "Jl. Updated Street",
    "noTelepon": "089876543210",
    "email": "jane@example.com"
  }' | jq
```

### Scenario 5: Delete Patient
```bash
TOKEN="your_token_here"

curl -X DELETE http://localhost:8001/api/patients/1 \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## ❌ Troubleshooting

### Error: 401 Unauthorized
**Cause**: Missing or invalid token
```bash
# ✗ Wrong - no token
curl -X GET http://localhost:8001/api/patients

# ✓ Correct - with token
curl -X GET http://localhost:8001/api/patients \
  -H "Authorization: Bearer $TOKEN"
```

### Error: Token Expired
**Solution**: Get new token
```bash
curl -X POST http://localhost:8001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

### Error: 403 Forbidden
**Cause**: User doesn't have required role
**Solution**: Register user with ADMIN role or use different user

### Error: 400 Bad Request
**Cause**: Invalid input data
**Check**:
- Email format
- Required fields present
- Data types correct

---

## 🔐 Token Format

Token should be included in header as:
```
Authorization: Bearer <token_value>
```

**Common mistakes**:
- ❌ `Authorization: <token>` (missing "Bearer")
- ❌ `Authorization: Bearer<token>` (missing space)
- ✓ `Authorization: Bearer <token>` (correct)

---

## 📊 Roles & Permissions

| Role | Endpoints | Notes |
|------|-----------|-------|
| ADMIN | All | Full access |
| DOCTOR | Patients | Can CRUD patients |
| STAFF | Patients | Can CRUD patients |
| USER | (None) | Limited/read-only access |

---

## 🛠️ Using Postman

1. **Register & Login**: Use `/api/auth/login` endpoint, copy token
2. **Set Authorization**: 
   - Auth Type → Bearer Token
   - Token → Paste your token
3. **Test Endpoints**: All subsequent requests will auto-include token

---

## 🛠️ Using PowerShell
```powershell
$token = "your_token_here"
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

Invoke-RestMethod -Uri "http://localhost:8001/api/patients" `
  -Method GET `
  -Headers $headers | ConvertTo-Json
```

---

## 🛠️ Using JavaScript/Fetch
```javascript
const token = "your_token_here";

fetch('http://localhost:8001/api/patients', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
.then(res => res.json())
.then(data => console.log(data))
.catch(err => console.error(err));
```

---

## 📝 Sample Database Data

For testing, you can insert sample data:

```sql
-- Register admin user first via API
-- Or insert directly:
INSERT INTO users (username, password, nama, email, role, is_active)
VALUES ('doctor1', '$2a$10$...', 'Dr. Smith', 'doctor@example.com', 'DOCTOR', true);

-- Insert sample patients
INSERT INTO pasiens (nama, nomor_identitas, alamat, no_telepon, email)
VALUES 
  ('John Doe', '1234567890', 'Jl. Main St', '081234567890', 'john@example.com'),
  ('Jane Smith', '9876543210', 'Jl. Oak Ave', '089876543210', 'jane@example.com');
```

---

## 🔗 Useful Links

- **Swagger UI**: http://localhost:8001/swagger-ui.html
- **OpenAPI Docs**: http://localhost:8001/v3/api-docs
- **Health Check**: http://localhost:8001/health
- **Actuator**: http://localhost:8001/actuator

---

## 📚 Related Files

- Main guide: [ADMIN_SERVICE_AUTH_GUIDE.md](ADMIN_SERVICE_AUTH_GUIDE.md)
- Testing scripts:
  - PowerShell: `test-admin-service.ps1`
  - Batch: `test-admin-service.bat`

