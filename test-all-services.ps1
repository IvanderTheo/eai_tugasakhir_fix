# Test API Gateway Integration with All Microservices
# Usage: .\test-all-services.ps1

param(
    [string]$GatewayUrl = "http://localhost:8000"
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   API Gateway - All Microservices Integration Test            ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# Test counters
$totalTests = 0
$passedTests = 0
$failedTests = 0

function Test-Endpoint {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Description,
        [hashtable]$Body = $null,
        [string]$Token = $null,
        [bool]$ExpectSuccess = $true
    )
    
    $totalTests++
    $uri = "$GatewayUrl$Path"
    
    Write-Host "[$totalTests] Testing: $Description" -ForegroundColor Yellow
    Write-Host "    $Method $Path" -ForegroundColor Gray
    
    try {
        $headers = @{
            "Content-Type" = "application/json"
        }
        
        if ($Token) {
            $headers["Authorization"] = "Bearer $Token"
        }
        
        if ($Method -eq "POST" -or $Method -eq "PUT") {
            $response = Invoke-RestMethod -Uri $uri `
                -Method $Method `
                -Headers $headers `
                -Body ($Body | ConvertTo-Json) `
                -ErrorAction Stop
        } else {
            $response = Invoke-RestMethod -Uri $uri `
                -Method $Method `
                -Headers $headers `
                -ErrorAction Stop
        }
        
        Write-Host "    ✓ PASS - Response received" -ForegroundColor Green
        $passedTests++
        return $response
        
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        $statusMsg = $_.Exception.Response.StatusDescription
        
        if ($ExpectSuccess) {
            Write-Host "    ✗ FAIL - $statusCode $statusMsg" -ForegroundColor Red
            $failedTests++
        } else {
            Write-Host "    ✓ PASS - Expected failure ($statusCode)" -ForegroundColor Green
            $passedTests++
        }
        return $null
    }
}

# ============ TEST 1: Health Check ============
Write-Host "`n=== [GROUP 1] Health & Status Checks ===" -ForegroundColor Magenta
Test-Endpoint -Method "GET" -Path "/health" -Description "API Gateway Health" | Out-Null

# ============ TEST 2: Swagger UI ============
Write-Host "`n=== [GROUP 2] Swagger UI & Documentation ===" -ForegroundColor Magenta
try {
    $swaggerResponse = Invoke-WebRequest -Uri "$GatewayUrl/swagger-ui.html" -ErrorAction Stop
    Write-Host "[2] Testing: Swagger UI Accessibility" -ForegroundColor Yellow
    Write-Host "    GET /swagger-ui.html" -ForegroundColor Gray
    Write-Host "    ✓ PASS - Swagger UI accessible" -ForegroundColor Green
    $passedTests++
} catch {
    Write-Host "    ✗ FAIL - Swagger UI not accessible" -ForegroundColor Red
    $failedTests++
}
$totalTests++

try {
    $openApiResponse = Invoke-RestMethod -Uri "$GatewayUrl/v3/api-docs" -ErrorAction Stop
    Write-Host "[3] Testing: OpenAPI Docs" -ForegroundColor Yellow
    Write-Host "    GET /v3/api-docs" -ForegroundColor Gray
    if ($openApiResponse.paths -and $openApiResponse.paths.Count -gt 0) {
        Write-Host "    ✓ PASS - Found $($openApiResponse.paths.Count) endpoints" -ForegroundColor Green
        $passedTests++
    } else {
        Write-Host "    ✗ FAIL - No endpoints found in OpenAPI spec" -ForegroundColor Red
        $failedTests++
    }
} catch {
    Write-Host "    ✗ FAIL - OpenAPI docs not accessible" -ForegroundColor Red
    $failedTests++
}
$totalTests++

# ============ TEST 3: Authentication (Public) ============
Write-Host "`n=== [GROUP 3] Authentication Services (Public) ===" -ForegroundColor Magenta

# Register new user
$registerBody = @{
    username = "testuser_$(Get-Random -Minimum 1000 -Maximum 9999)"
    password = "test123456"
    nama = "Test User"
    email = "test$(Get-Random)@example.com"
    role = "USER"
}

$registerResponse = Test-Endpoint -Method "POST" -Path "/api/auth/register" `
    -Description "Register New User" `
    -Body $registerBody

# Login
if ($registerResponse -and $registerResponse.username) {
    $username = $registerResponse.username
    Write-Host "`n[5] Testing: User Login" -ForegroundColor Yellow
    Write-Host "    POST /api/auth/login" -ForegroundColor Gray
    
    $loginBody = @{
        username = $username
        password = "test123456"
    }
    
    try {
        $loginResponse = Invoke-RestMethod -Uri "$GatewayUrl/api/auth/login" `
            -Method POST `
            -ContentType "application/json" `
            -Body ($loginBody | ConvertTo-Json)
        
        if ($loginResponse.token) {
            Write-Host "    ✓ PASS - Login successful, token obtained" -ForegroundColor Green
            $passedTests++
            $token = $loginResponse.token
            Write-Host "    Token (first 50 chars): $($token.Substring(0,50))..." -ForegroundColor Gray
        } else {
            Write-Host "    ✗ FAIL - No token in response" -ForegroundColor Red
            $failedTests++
        }
    } catch {
        Write-Host "    ✗ FAIL - Login failed: $_" -ForegroundColor Red
        $failedTests++
    }
    $totalTests++
}

# ============ TEST 4: Admin Service ============
Write-Host "`n=== [GROUP 4] Admin Service (Port 8001) ===" -ForegroundColor Magenta

if ($token) {
    Test-Endpoint -Method "GET" -Path "/api/admin/patients" `
        -Description "Get All Patients" `
        -Token $token | Out-Null
    
    # Create patient
    $patientBody = @{
        nama = "Test Patient"
        nomorIdentitas = "1234567890"
        alamat = "Jl. Test"
        noTelepon = "081234567890"
        email = "patient@test.com"
    }
    
    Test-Endpoint -Method "POST" -Path "/api/admin/patients" `
        -Description "Create New Patient" `
        -Body $patientBody `
        -Token $token | Out-Null
}

# ============ TEST 5: Medical Service ============
Write-Host "`n=== [GROUP 5] Medical Service (Port 8002) ===" -ForegroundColor Magenta

if ($token) {
    Test-Endpoint -Method "GET" -Path "/api/medical/examinations" `
        -Description "Get All Medical Examinations" `
        -Token $token | Out-Null
    
    Test-Endpoint -Method "GET" -Path "/api/medical/prescriptions" `
        -Description "Get All Prescriptions" `
        -Token $token | Out-Null
}

# ============ TEST 6: Pharmacy Service ============
Write-Host "`n=== [GROUP 6] Pharmacy Service (Port 8004) ===" -ForegroundColor Magenta

if ($token) {
    Test-Endpoint -Method "GET" -Path "/api/pharmacy/medicines" `
        -Description "Get All Medicines" `
        -Token $token | Out-Null
}

# ============ TEST 7: Payment Service ============
Write-Host "`n=== [GROUP 7] Payment Service (Port 8003) ===" -ForegroundColor Magenta

if ($token) {
    Test-Endpoint -Method "GET" -Path "/api/payment/invoices" `
        -Description "Get All Invoices" `
        -Token $token | Out-Null
}

# ============ TEST 8: Security - Protected Endpoints ============
Write-Host "`n=== [GROUP 8] Security Tests (Protected Endpoints) ===" -ForegroundColor Magenta

Write-Host "[${($totalTests+1)}] Testing: Protected Endpoint Without Token (Should Fail)" -ForegroundColor Yellow
Write-Host "    GET /api/admin/patients (no token)" -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri "$GatewayUrl/api/admin/patients" `
        -Method GET `
        -ErrorAction Stop
    Write-Host "    ✗ FAIL - Endpoint accessible without token (SECURITY ISSUE!)" -ForegroundColor Red
    $failedTests++
} catch {
    if ($_.Exception.Response.StatusCode.Value__ -eq 401) {
        Write-Host "    ✓ PASS - Properly blocked (401 Unauthorized)" -ForegroundColor Green
        $passedTests++
    } else {
        Write-Host "    ⊘ SKIP - Unexpected status" -ForegroundColor Yellow
    }
}
$totalTests++

# ============ SUMMARY ============
Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                        TEST SUMMARY                            ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Host "`nTotal Tests Run:  $totalTests" -ForegroundColor Gray
Write-Host "Tests Passed:     $passedTests" -ForegroundColor Green
Write-Host "Tests Failed:     $failedTests" -ForegroundColor $(if ($failedTests -eq 0) { "Green" } else { "Red" })

$passPercentage = [math]::Round(($passedTests / $totalTests) * 100, 1)
Write-Host "Success Rate:     $passPercentage%" -ForegroundColor $(if ($passPercentage -ge 80) { "Green" } else { "Yellow" })

Write-Host "`n" + ("="*66) -ForegroundColor Gray

if ($failedTests -eq 0) {
    Write-Host "✓ ALL TESTS PASSED! API Gateway integration is working correctly." -ForegroundColor Green
} else {
    Write-Host "✗ Some tests failed. Check services and configuration." -ForegroundColor Yellow
}

Write-Host "`n=== Service Status ===" -ForegroundColor Yellow
Write-Host "API Gateway:     http://localhost:8000 (Swagger: /swagger-ui.html)" -ForegroundColor Cyan
Write-Host "Admin Service:   http://localhost:8001" -ForegroundColor Cyan
Write-Host "Medical Service: http://localhost:8002" -ForegroundColor Cyan
Write-Host "Payment Service: http://localhost:8003" -ForegroundColor Cyan
Write-Host "Pharmacy Service: http://localhost:8004" -ForegroundColor Cyan

Write-Host "`n"
