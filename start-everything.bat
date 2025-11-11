@echo off
echo ========================================
echo Starting CashCached Full Stack Application
echo ========================================
echo.

REM Check if Docker Desktop is running
echo Checking Docker status...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo Docker is not running. Please start Docker Desktop manually and wait for it to be ready.
    echo After Docker Desktop is running, run this script again.
    pause
    exit /b 1
)

echo Docker is running!
echo.

REM Start Docker services
echo ========================================
echo Step 1: Starting Docker Services
echo ========================================
echo Starting MySQL, Kafka, Zookeeper, Redis, and Kafdrop...
docker compose -f compose.yaml up -d

echo Waiting for services to initialize (30 seconds)...
timeout /t 30 /nobreak > nul

echo.
echo ========================================
echo Step 2: Starting Spring Boot Microservices
echo ========================================
echo.

echo Starting Customer Service on port 8081...
start "Customer Service" cmd /k "cd customer && .\mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

echo Starting Product Service on port 8082...
start "Product Service" cmd /k "cd product-pricing && .\mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

echo Starting FD Calculator Service on port 8083...
start "FD Calculator Service" cmd /k "cd fd-calculator && .\mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

echo Starting Accounts Service on port 8084...
start "Accounts Service" cmd /k "cd accounts && .\mvnw.cmd spring-boot:run"
timeout /t 10 /nobreak > nul

echo Starting Main Gateway on port 8080...
start "Main Gateway" cmd /k "cd main && .\mvnw.cmd spring-boot:run"
timeout /t 5 /nobreak > nul

echo.
echo ========================================
echo Step 3: Starting React Frontend
echo ========================================
echo.

echo Installing frontend dependencies and starting Vite dev server...
start "React Frontend" cmd /k "cd main\frontend && (if exist node_modules (echo Dependencies already installed) else (pnpm install || npm install)) && (pnpm dev || npm run dev)"

echo.
echo ========================================
echo All Services Started!
echo ========================================
echo.
echo Docker Services:
echo - MySQL: localhost:3306 (root/Aarav)
echo - Kafka: localhost:9092
echo - Zookeeper: localhost:2181
echo - Redis: localhost:6379
echo - Kafdrop UI: http://localhost:9000
echo.
echo Spring Boot Services:
echo - Customer Service: http://localhost:8081
echo - Product Service: http://localhost:8082
echo - FD Calculator Service: http://localhost:8083
echo - Accounts Service: http://localhost:8084
echo - Main Gateway: http://localhost:8080
echo.
echo Frontend:
echo - React App: http://localhost:5173
echo.
echo Please wait 2-3 minutes for all services to fully start.
echo Check individual command windows for startup status.
echo.
pause
