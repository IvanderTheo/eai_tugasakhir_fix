@echo off
REM GrowBusiness Microservices Startup Script for Windows

setlocal enabledelayedexpansion

echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║          GrowBusiness Microservices Startup Script              ║
echo ║                   Version 1.0 - Windows                        ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

REM Check if services are already running
echo Checking prerequisites...
echo.

REM Kafka Check / Docker start
echo [1/5] Checking Kafka...
netstat -an | findstr "9092" > nul
if %errorlevel% equ 0 (
    echo ✓ Kafka is running on port 9092
) else (
    echo Kafka is not running. Starting via Docker...
    call docker\start-kafka.bat
    if %errorlevel% neq 0 (
        echo ✗ Failed to start Kafka. Ensure Docker Desktop is running, then run: docker\start-kafka.bat
        exit /b 1
    )
    echo ✓ Kafka started on port 9092
)

REM MySQL Check
echo [2/5] Checking MySQL...
netstat -an | findstr "3306" > nul
if %errorlevel% equ 0 (
    echo ✓ MySQL is running on port 3306
) else (
    echo ✗ MySQL is NOT running on port 3306
    echo Please start MySQL service
    echo.
)

echo [3/5] Checking Java...
java -version 2>&1 | findstr "17" > nul
if %errorlevel% equ 0 (
    echo ✓ Java 17 found
) else (
    echo ✗ Java 17 not found. Please install Java 17+
    exit /b 1
)

echo [4/5] Checking Maven...
mvn -version > nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Maven found
) else (
    echo ✗ Maven not found. Please install Maven
    exit /b 1
)

echo [5/5] All prerequisites checked
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║              Building Services...                               ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

REM Build services
echo Building Admin Service...
cd admin-service
call mvn clean install -q
if %errorlevel% neq 0 (
    echo Build failed for admin-service
    exit /b 1
)
cd ..

echo Building Medical Service...
cd medical-service
call mvn clean install -q
if %errorlevel% neq 0 (
    echo Build failed for medical-service
    exit /b 1
)
cd ..

echo Building Pharmacy Service...
cd pharmacy-service
call mvn clean install -q
if %errorlevel% neq 0 (
    echo Build failed for pharmacy-service
    exit /b 1
)
cd ..

echo Building Payment Service...
cd payment-service
call mvn clean install -q
if %errorlevel% neq 0 (
    echo Build failed for payment-service
    exit /b 1
)
cd ..

echo Building API Gateway...
cd api-gateway
call mvn clean install -q
if %errorlevel% neq 0 (
    echo Build failed for api-gateway
    exit /b 1
)
cd ..

echo.
echo ✓ All services built successfully
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║         Services Starting (Open New Terminals)                  ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

REM Start services in new terminals
echo Starting Admin Service (Port 8001)...
start "Admin Service" cmd /k "cd admin-service && mvn spring-boot:run"
timeout /t 3 /nobreak

echo Starting Medical Service (Port 8002)...
start "Medical Service" cmd /k "cd medical-service && mvn spring-boot:run"
timeout /t 3 /nobreak

echo Starting Pharmacy Service (Port 8004)...
start "Pharmacy Service" cmd /k "cd pharmacy-service && mvn spring-boot:run"
timeout /t 3 /nobreak

echo Starting Payment Service (Port 8003)...
start "Payment Service" cmd /k "cd payment-service && mvn spring-boot:run"
timeout /t 3 /nobreak

echo Starting API Gateway (Port 8000)...
start "API Gateway" cmd /k "cd api-gateway && mvn spring-boot:run"
timeout /t 3 /nobreak

echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║             Services Configuration                              ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
echo Admin Service ................ http://localhost:8001
echo Medical Service .............. http://localhost:8002
echo Pharmacy Service ............. http://localhost:8004
echo Payment Service .............. http://localhost:8003
echo API Gateway .................. http://localhost:8000
echo.
echo Swagger UI (test API + JSON):
echo   Admin ........ http://localhost:8001/swagger-ui.html
echo   Medical ...... http://localhost:8002/swagger-ui.html
echo   Pharmacy ..... http://localhost:8004/swagger-ui.html
echo   Payment ...... http://localhost:8003/swagger-ui.html
echo   Gateway index  http://localhost:8000/swagger-ui.html
echo.
echo JWT Token Secret: GrowBussinessSecretKeyForJWTTokenGenerationAndValidation1234567890
echo Token Expiration: 24 hours
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║              Quick Start Commands                               ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
echo 1. Register User:
echo    curl -X POST "http://localhost:8000/api/auth/register?username=admin&password=admin123&nama=Admin&email=admin@test.com&role=ADMIN"
echo.
echo 2. Login:
echo    curl -X POST "http://localhost:8000/api/auth/login?username=admin&password=admin123"
echo.
echo 3. Get All Patients:
echo    curl -X GET "http://localhost:8000/api/admin/patients" -H "Authorization: Bearer YOUR_TOKEN"
echo.
echo For more information, see MICROSERVICES_SETUP.md
echo.
echo ✓ All services started successfully!
echo Type 'exit' in service terminals to stop them
echo.
pause
