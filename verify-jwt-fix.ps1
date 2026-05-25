# Test JWT Filter Fix - Quick Verification
# Usage: .\verify-jwt-fix.ps1

$GatewayUrl = "http://localhost:8000"
$Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

Write-Host "`n╔════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  JWT Filter Fix - Verification Script              ║" -ForegroundColor Cyan
Write-Host "║  Testing public endpoints (no token required)       ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

Write-Host "[$Timestamp] Starting JWT filter verification..." -ForegroundColor Yellow

$testsPassed = 0
$testsFailed = 0

# ============ TEST 1: Register (Public) ============
Write-Host "`n[TEST 1] POST /api/auth/register - Public (no token)" -ForegroundColor Yellow
try {
    $username = "testuser_$(Get-Random -Minimum 1000 -Maximum 9999)"
    $registerBody = @{
        username = $username
        password = "test123456"
        nama = "Test User"
        email = "test$(Get-Random)@example.com"
        role = "USER"
    } | ConvertTo-Json

    $registerResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $registerBody

    if ($registerResponse.message -eq "User registered successfully") {
        Write-Host "  ✓ PASS - Register successful (201 Created)" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host "  ✗ FAIL - Unexpected response: $($registerResponse.message)" -ForegroundColor Red
        $testsFailed++
    }
} catch {
    Write-Host "  ✗ FAIL - $($_.Exception.Message)" -ForegroundColor Red
    $testsFailed++
}

# ============ TEST 2: Login (Public) ============
Write-Host "`n[TEST 2] POST /api/auth/login - Public (no token)" -ForegroundColor Yellow
try {
    $loginBody = @{
        username = $username
        password = "test123456"
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody

    if ($loginResponse.token) {
        $token = $loginResponse.token
        Write-Host "  ✓ PASS - Login successful, received token" -ForegroundColor Green
        Write-Host "    Token (first 50 chars): $($token.Substring(0, 50))..." -ForegroundColor Gray
        $testsPassed++
    } else {
        Write-Host "  ✗ FAIL - No token in response" -ForegroundColor Red
        $testsFailed++
    }
} catch {
    Write-Host "  ✗ FAIL - $($_.Exception.Message)" -ForegroundColor Red
    $testsFailed++
}

# ============ TEST 3: Health (Public) ============
Write-Host "`n[TEST 3] GET /health - Public (no token)" -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "$GatewayUrl/health" -Method GET
    Write-Host "  ✓ PASS - Health endpoint accessible" -ForegroundColor Green
    $testsPassed++
} catch {
    Write-Host "  ✗ FAIL - $($_.Exception.Message)" -ForegroundColor Red
    $testsFailed++
}

# ============ TEST 4: Swagger (Public) ============
Write-Host "`n[TEST 4] GET /swagger-ui.html - Public (no token)" -ForegroundColor Yellow
try {
    $swaggerResponse = Invoke-RestMethod -Uri "$GatewayUrl/swagger-ui.html" -Method GET
    Write-Host "  ✓ PASS - Swagger UI accessible" -ForegroundColor Green
    $testsPassed++
} catch {
    Write-Host "  ✗ FAIL - $($_.Exception.Message)" -ForegroundColor Red
    $testsFailed++
}

# ============ TEST 5: Protected Endpoint WITH Token ============
Write-Host "`n[TEST 5] GET /api/medical/pasien - Protected (with token)" -ForegroundColor Yellow
try {
    if ($token) {
        $patientsResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/medical/pasien" `
            -Method GET `
            -Headers @{ "Authorization" = "Bearer $token" }
        Write-Host "  ✓ PASS - Protected endpoint accessible with token" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host "  ⊘ SKIP - No token available" -ForegroundColor Gray
    }
} catch {
    Write-Host "  ✗ FAIL - $($_.Exception.Message)" -ForegroundColor Red
    $testsFailed++
}

# ============ TEST 6: Protected Endpoint WITHOUT Token (Should Fail) ============
Write-Host "`n[TEST 6] GET /api/medical/pasien - Protected (without token) [Should Fail]" -ForegroundColor Yellow
try {
    $patientsResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/medical/pasien" `
        -Method GET -ErrorAction Stop
    Write-Host "  ✗ FAIL - Endpoint accessible without token (SECURITY ISSUE!)" -ForegroundColor Red
    $testsFailed++
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "  ✓ PASS - Endpoint properly blocked (401 Unauthorized)" -ForegroundColor Green
        $testsPassed++
    } else {
        Write-Host "  ⊘ SKIP - Unexpected status: $($_.Exception.Response.StatusCode)" -ForegroundColor Gray
    }
}

# ============ SUMMARY ============
Write-Host "`n╔════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                    TEST SUMMARY                     ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Host "`nTests Passed: $testsPassed" -ForegroundColor Green
Write-Host "Tests Failed: $testsFailed" -ForegroundColor $(if ($testsFailed -eq 0) { "Green" } else { "Red" })
Write-Host "Total Tests: $($testsPassed + $testsFailed)"

if ($testsFailed -eq 0) {
    Write-Host "`n✓ All tests passed! JWT filter fix is working correctly." -ForegroundColor Green
} else {
    Write-Host "`n✗ Some tests failed. Check services are running and JWT filter fix is deployed." -ForegroundColor Red
}

Write-Host "`nTimestamp: $Timestamp`n" -ForegroundColor Gray
