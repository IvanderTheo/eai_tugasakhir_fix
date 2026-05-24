@echo off
setlocal
cd /d "%~dp0.."

echo Starting Kafka (Docker)...
docker compose up -d kafka
if %errorlevel% neq 0 (
    echo Failed to start Kafka. Is Docker Desktop running?
    exit /b 1
)

echo Waiting for Kafka to be healthy...
set /a attempts=0
:wait_loop
set /a attempts+=1
docker compose ps kafka 2>nul | findstr /i "healthy" >nul
if %errorlevel% equ 0 goto ready
if %attempts% geq 60 (
    echo Kafka did not become healthy in time. Check: docker compose logs kafka
    exit /b 1
)
timeout /t 2 /nobreak >nul
goto wait_loop

:ready
echo Kafka is ready on localhost:9092
docker compose ps kafka
exit /b 0
