# Admin Service - Authentication Testing Script (PowerShell)
# Usage: .\test-admin-service.ps1

$ApiUrl = "http://localhost:8001"
$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$RandomNum = Get-Random -Minimum 1000 -Maximum 9999
$Username = "testadmin_$RandomNum"
$Password = "Test@12345"
$Email = "test$RandomNum@example.com"
$Role = "ADMIN"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Admin Service - Authentication Testing" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# ============ STEP 1: Register User ============
Write-Host "[1] Registering new user..." -ForegroundColor Yellow
Write-Host "    Username: $Username"
Write-Host "    Password: $Password"
Write-Host "    Email: $Email"
Write-Host "    Role: $Role`n"

try {
    $registerBody = @{
        username = $Username
        password = $Password
        nama = "Test Admin"
        email = $Email
        role = $Role
    } | ConvertTo-Json

    $registerResponse = Invoke-RestMethod -Uri "$ApiUrl/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody

    Write-Host "[OK] Registration successful: $($registerResponse.message)" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Registration failed: $_" -ForegroundColor Red
    exit 1
}

# ============ STEP 2: Login & Get Token ============
Write-Host "`n[2] Logging in to get JWT token..." -ForegroundColor Yellow

try {
    $loginBody = @{
        username = $Username
        password = $Password
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$ApiUrl/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody

    $Token = $loginResponse.token
    
    if ([string]::IsNullOrEmpty($Token)) {
        throw "No token in response"
    }

    Write-Host "[OK] Got JWT Token (first 60 chars):" -ForegroundColor Green
    Write-Host "    $($Token.Substring(0, [Math]::Min(60, $Token.Length)))..." -ForegroundColor Cyan

    # Save token to file
    $Token | Out-File -FilePath "admin_service_token_$Timestamp.txt"
    Write-Host "    Token saved to: admin_service_token_$Timestamp.txt`n" -ForegroundColor Gray

} catch {
    Write-Host "[ERROR] Login failed: $_" -ForegroundColor Red
    exit 1
}

# ============ STEP 3: Test Protected Endpoint - Get All Patients ============
Write-Host "[3] Testing GET /api/patients (protected endpoint)..." -ForegroundColor Yellow

try {
    $patientsResponse = Invoke-RestMethod -Uri "$ApiUrl/api/patients" `
        -Method GET `
        -Headers @{
            "Authorization" = "Bearer $Token"
            "Content-Type" = "application/json"
        }

    Write-Host "[OK] Successfully retrieved patients:" -ForegroundColor Green
    Write-Host "    Count: $($patientsResponse.count)"
    Write-Host "    Data: $(($patientsResponse.data | ConvertTo-Json) | ConvertFrom-Json | ConvertTo-Json -Depth 1)`n"

} catch {
    Write-Host "[ERROR] Failed to get patients: $_" -ForegroundColor Red
}

# ============ STEP 4: Create New Patient ============
Write-Host "[4] Testing POST /api/patients (create new patient)..." -ForegroundColor Yellow

try {
    $patientBody = @{
        nama = "Pasien Test $Timestamp"
        nomorIdentitas = "1234567890"
        alamat = "Jl. Testing Street"
        noTelepon = "082222222222"
        email = "pasien$RandomNum@test.com"
    } | ConvertTo-Json

    $createResponse = Invoke-RestMethod -Uri "$ApiUrl/api/patients" `
        -Method POST `
        -Headers @{
            "Authorization" = "Bearer $Token"
            "Content-Type" = "application/json"
        } `
        -Body $patientBody

    Write-Host "[OK] Patient created successfully:" -ForegroundColor Green
    Write-Host "    ID: $($createResponse.data.id)"
    Write-Host "    Nama: $($createResponse.data.nama)"
    Write-Host "    Email: $($createResponse.data.email)`n"

} catch {
    Write-Host "[ERROR] Failed to create patient: $_" -ForegroundColor Red
}

# ============ STEP 5: Get Patient by ID ============
Write-Host "[5] Testing GET /api/patients/{id} (get by ID)..." -ForegroundColor Yellow

try {
    $patientByIdResponse = Invoke-RestMethod -Uri "$ApiUrl/api/patients/1" `
        -Method GET `
        -Headers @{
            "Authorization" = "Bearer $Token"
            "Content-Type" = "application/json"
        }

    Write-Host "[OK] Successfully retrieved patient:" -ForegroundColor Green
    Write-Host "    ID: $($patientByIdResponse.data.id)"
    Write-Host "    Nama: $($patientByIdResponse.data.nama)`n"

} catch {
    Write-Host "[INFO] Get by ID returned (patient may not exist): $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
}

# ============ STEP 6: Search Patient ============
Write-Host "[6] Testing GET /api/patients/search (search patient)..." -ForegroundColor Yellow

try {
    $searchResponse = Invoke-RestMethod -Uri "$ApiUrl/api/patients/search?nama=Test" `
        -Method GET `
        -Headers @{
            "Authorization" = "Bearer $Token"
            "Content-Type" = "application/json"
        }

    Write-Host "[OK] Search results:" -ForegroundColor Green
    Write-Host "    Found: $($searchResponse.count) patients`n"

} catch {
    Write-Host "[INFO] Search returned: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
}

# ============ FINAL SUMMARY ============
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Complete!" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Summary:" -ForegroundColor Yellow
Write-Host "  ✓ User registered successfully" -ForegroundColor Green
Write-Host "  ✓ JWT token obtained" -ForegroundColor Green
Write-Host "  ✓ Protected endpoints tested" -ForegroundColor Green
Write-Host ""
Write-Host "Credentials for future testing:" -ForegroundColor Yellow
Write-Host "  Username: $Username" -ForegroundColor Cyan
Write-Host "  Password: $Password" -ForegroundColor Cyan
Write-Host "  Token file: admin_service_token_$Timestamp.txt" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Use token in Swagger UI (http://localhost:8001/swagger-ui.html)" -ForegroundColor Gray
Write-Host "  2. Or use 'Bearer TOKEN' in Authorization header for API calls" -ForegroundColor Gray
Write-Host "  3. Token expires in 24 hours" -ForegroundColor Gray
Write-Host ""
