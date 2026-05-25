@echo off
REM Admin Service Authentication Testing Script
REM Run this in PowerShell or CMD to test admin-service endpoints

echo.
echo ========================================
echo Admin Service - Authentication Testing
echo ========================================
echo.

REM Configuration
set API_URL=http://localhost:8001
set USERNAME=testadmin_%RANDOM%
set PASSWORD=Test@12345
set EMAIL=test%RANDOM%@example.com
set ROLES=ADMIN

echo.
echo [1] Registering new user...
echo Username: %USERNAME%
echo Password: %PASSWORD%
echo Email: %EMAIL%
echo.

for /f %%i in ('curl -s -X POST %API_URL%/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"%USERNAME%\",\"password\":\"%PASSWORD%\",\"nama\":\"Test Admin\",\"email\":\"%EMAIL%\",\"role\":\"%ROLES%\"}" ^
  ^| jq -r ".message"') do set REGISTER_MESSAGE=%%i

echo Response: %REGISTER_MESSAGE%

if "%REGISTER_MESSAGE%"=="User registered successfully" (
    echo [OK] Registration successful
) else (
    echo [ERROR] Registration failed
    exit /b 1
)

echo.
echo [2] Logging in to get JWT token...
echo.

REM Store token in variable
for /f %%i in ('curl -s -X POST %API_URL%/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"%USERNAME%\",\"password\":\"%PASSWORD%\"}" ^
  ^| jq -r ".token"') do set TOKEN=%%i

if "%TOKEN%"=="" (
    echo [ERROR] Failed to get token!
    exit /b 1
)

echo [OK] Got JWT Token (first 50 chars):
echo %TOKEN:~0,50%...
echo.

REM Save token to file for reference
echo %TOKEN% > admin_service_token.txt
echo Token saved to: admin_service_token.txt
echo.

echo.
echo [3] Testing GET /api/patients (protected endpoint)...
echo.

curl -s -X GET %API_URL%/api/patients ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" | jq .

echo.
echo [4] Testing POST /api/patients (create new patient)...
echo.

curl -s -X POST %API_URL%/api/patients ^
  -H "Authorization: Bearer %TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d "{\"nama\":\"Test Patient\",\"nomorIdentitas\":\"1234567890\",\"alamat\":\"Jl. Testing\",\"noTelepon\":\"082222222222\",\"email\":\"patient@test.com\"}" | jq .

echo.
echo ========================================
echo Testing Complete!
echo ========================================
echo.
echo Notes:
echo - Token saved to: admin_service_token.txt
echo - Use 'Bearer TOKEN' in Authorization header for other requests
echo - Token expires in 24 hours
echo - Use /api/auth/refresh to refresh expired token
echo.
