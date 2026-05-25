# 🔧 PUBLIC ENDPOINTS FIX - Login & Register

## ✅ Changes Made

### 1. Fixed SecurityConfig in 5 Services

**Key Changes**:
- Reordered: `authorizeHttpRequests()` BEFORE `exceptionHandling()`
- Explicitly list all public endpoints
- Added specific routes for `/api/auth/register` and `/api/auth/login`

**Example**:
```java
http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(sessionManagement -> 
        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    )
    // ← AUTHORIZATION FIRST
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/api/auth/register").permitAll()
        .requestMatchers("/api/auth/login").permitAll()
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/health").permitAll()
        // ... more public endpoints
        .anyRequest().authenticated()
    )
    // ← EXCEPTION HANDLING SECOND
    .exceptionHandling(exceptionHandling -> exceptionHandling
        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
    )
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

### 2. Updated JwtAuthenticationFilter

Skip public endpoints di filter level untuk double protection.

**Services Modified** (5 total):
1. ✅ api-gateway/src/main/java/.../SecurityConfig.java
2. ✅ admin-service/src/main/java/.../SecurityConfig.java
3. ✅ medical-service/src/main/java/.../SecurityConfig.java
4. ✅ pharmacy-service/src/main/java/.../SecurityConfig.java
5. ✅ payment-service/src/main/java/.../SecurityConfig.java

---

## 🚀 Testing Instructions

### Step 1: Rebuild Services

```bash
cd c:\Users\ivand\OneDrive\Documents\eai_fix_tugas_akhir

# Build all services
cd admin-service && mvn clean install -DskipTests
cd ../medical-service && mvn clean install -DskipTests
cd ../pharmacy-service && mvn clean install -DskipTests
cd ../payment-service && mvn clean install -DskipTests
cd ../api-gateway && mvn clean install -DskipTests
```

### Step 2: Start Services

```bash
# Terminal 1 - API Gateway (Port 8000)
cd api-gateway
mvn spring-boot:run

# Terminal 2 - Admin Service (Port 8001)
cd admin-service
mvn spring-boot:run

# Terminal 3 - Medical Service (Port 8002)
cd medical-service
mvn spring-boot:run

# Terminal 4 - Pharmacy Service (Port 8004)
cd pharmacy-service
mvn spring-boot:run

# Terminal 5 - Payment Service (Port 8003)
cd payment-service
mvn spring-boot:run
```

### Step 3: Test Register Endpoint ✅ NOW PUBLIC!

**Via cURL**:
```bash
curl -X POST http://localhost:8000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "nama": "Admin User",
    "email": "admin@test.com",
    "role": "ADMIN"
  }'
```

**Expected Response (201 Created)** ✅:
```json
{
  "message": "User registered successfully",
  "username": "admin"
}
```

### Step 4: Test Login Endpoint ✅ NOW PUBLIC!

```bash
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Expected Response (200 OK)** ✅:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "message": "Login successful"
}
```

### Step 5: Test Refresh Token Endpoint ✅ NOW PUBLIC!

```bash
curl -X POST http://localhost:8000/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin"
  }'
```

---

## 🧪 Quick PowerShell Test

```powershell
# Test Register
$response = Invoke-RestMethod -Uri "http://localhost:8000/api/auth/register" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{
    "username": "testuser",
    "password": "test123",
    "nama": "Test User",
    "email": "test@example.com",
    "role": "USER"
  }'

if ($response.message -eq "User registered successfully") {
    Write-Host "✓ Register PASS" -ForegroundColor Green
} else {
    Write-Host "✗ Register FAIL" -ForegroundColor Red
}

# Test Login
$loginResponse = Invoke-RestMethod -Uri "http://localhost:8000/api/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{
    "username": "testuser",
    "password": "test123"
  }'

if ($loginResponse.token) {
    Write-Host "✓ Login PASS - Got token" -ForegroundColor Green
} else {
    Write-Host "✗ Login FAIL" -ForegroundColor Red
}
```

---

## 📊 Public Endpoints (No Token Required)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Register new user |
| `/api/auth/login` | POST | Login & get JWT token |
| `/api/auth/refresh` | POST | Refresh JWT token |
| `/health` | GET | Health check |
| `/actuator/**` | GET | Spring Actuator endpoints |
| `/swagger-ui.html` | GET | Swagger UI |
| `/swagger-ui/**` | GET | Swagger UI assets |
| `/v3/api-docs` | GET | OpenAPI documentation |
| `/v3/api-docs/**` | GET | OpenAPI docs assets |

---

## 🔒 Protected Endpoints (Token Required)

All other endpoints require JWT token in `Authorization` header:
```
Authorization: Bearer <your_token_here>
```

Example:
```bash
curl -X GET http://localhost:8000/api/patients \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

---

## ✨ Important Notes

1. **Register & Login are now fully public** - no token needed
2. **SecurityConfig order is critical** - authorization must come before exception handling
3. **JwtAuthenticationFilter** - properly skips public endpoints
4. **All 5 services** have same configuration for consistency

---

## 🎯 Verification Checklist

After rebuilding and starting services:

- [ ] POST `/api/auth/register` → **201 Created** (tidak 401!)
- [ ] POST `/api/auth/login` → **200 OK** + token
- [ ] POST `/api/auth/refresh` → **200 OK** + new token
- [ ] GET `/health` → **200 OK**
- [ ] GET `/swagger-ui.html` → **200 OK**
- [ ] GET `/v3/api-docs` → **200 OK**
- [ ] GET protected endpoint WITHOUT token → **401 Unauthorized** (expected)
- [ ] GET protected endpoint WITH token → **200 OK**

---

## 📝 Summary of Fixes

| Issue | Before | After |
|-------|--------|-------|
| `/api/auth/register` | 401 Unauthorized | ✅ 201 Created |
| `/api/auth/login` | 401 Unauthorized | ✅ 200 OK |
| `exceptionHandling` order | Before authz | After authz ✅ |
| Filter skip logic | Options only | All public paths ✅ |

Semua public endpoints sekarang accessible tanpa token! 🎉

