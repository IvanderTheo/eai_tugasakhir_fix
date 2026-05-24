#!/bin/bash

# GrowBusiness Microservices Startup Script for Linux/Mac

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║          GrowBusiness Microservices Startup Script              ║"
echo "║                   Version 1.0 - Linux/Mac                      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if services are already running
echo "Checking prerequisites..."
echo ""

# Kafka Check / Docker start
echo "[1/5] Checking Kafka..."
if lsof -Pi :9092 -sTCP:LISTEN -t > /dev/null ; then
    echo "✓ Kafka is running on port 9092"
else
    echo "Kafka is not running. Starting via Docker..."
    bash docker/start-kafka.sh || exit 1
    echo "✓ Kafka started on port 9092"
fi

# MySQL Check
echo "[2/5] Checking MySQL..."
if lsof -Pi :3306 -sTCP:LISTEN -t > /dev/null ; then
    echo "✓ MySQL is running on port 3306"
else
    echo "✗ MySQL is NOT running on port 3306"
    echo "Please start MySQL service"
    echo ""
fi

# Java Check
echo "[3/5] Checking Java..."
if java -version 2>&1 | grep -q "17"; then
    echo "✓ Java 17 found"
else
    echo "✗ Java 17 not found. Please install Java 17+"
    exit 1
fi

# Maven Check
echo "[4/5] Checking Maven..."
if command -v mvn > /dev/null 2>&1; then
    echo "✓ Maven found"
else
    echo "✗ Maven not found. Please install Maven"
    exit 1
fi

echo "[5/5] All prerequisites checked"
echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              Building Services...                               ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Build services
echo "Building Admin Service..."
cd admin-service
mvn clean install -q
if [ $? -ne 0 ]; then
    echo "Build failed for admin-service"
    exit 1
fi
cd ..

echo "Building Medical Service..."
cd medical-service
mvn clean install -q
if [ $? -ne 0 ]; then
    echo "Build failed for medical-service"
    exit 1
fi
cd ..

echo "Building Pharmacy Service..."
cd pharmacy-service
mvn clean install -q
if [ $? -ne 0 ]; then
    echo "Build failed for pharmacy-service"
    exit 1
fi
cd ..

echo "Building Payment Service..."
cd payment-service
mvn clean install -q
if [ $? -ne 0 ]; then
    echo "Build failed for payment-service"
    exit 1
fi
cd ..

echo "Building API Gateway..."
cd api-gateway
mvn clean install -q
if [ $? -ne 0 ]; then
    echo "Build failed for api-gateway"
    exit 1
fi
cd ..

echo ""
echo "✓ All services built successfully"
echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         Services Starting (Open New Terminals)                  ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Start services in background or new terminals
echo "Starting Admin Service (Port 8001)..."
(cd admin-service && mvn spring-boot:run) &
sleep 3

echo "Starting Medical Service (Port 8002)..."
(cd medical-service && mvn spring-boot:run) &
sleep 3

echo "Starting Pharmacy Service (Port 8004)..."
(cd pharmacy-service && mvn spring-boot:run) &
sleep 3

echo "Starting Payment Service (Port 8003)..."
(cd payment-service && mvn spring-boot:run) &
sleep 3

echo "Starting API Gateway (Port 8000)..."
(cd api-gateway && mvn spring-boot:run) &
sleep 3

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║             Services Configuration                              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "Admin Service ................ http://localhost:8001"
echo "Medical Service .............. http://localhost:8002"
echo "Pharmacy Service ............. http://localhost:8004"
echo "Payment Service .............. http://localhost:8003"
echo "API Gateway .................. http://localhost:8000"
echo ""
echo "JWT Token Secret: GrowBussinessSecretKeyForJWTTokenGenerationAndValidation1234567890"
echo "Token Expiration: 24 hours"
echo ""
echo "✓ All services started successfully!"
echo ""
echo "To stop all services, use: pkill -f 'spring-boot:run'"
echo ""
