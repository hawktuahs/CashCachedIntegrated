# CashCached - Complete Startup Instructions

## Prerequisites Installed
✅ Docker Desktop for Windows
✅ Java 17+
✅ Maven
✅ Node.js 18+
✅ MySQL credentials: root/Aarav

## Quick Start Guide

### Step 1: Start Docker Desktop
1. Open **Docker Desktop** from Windows Start Menu
2. Wait for Docker to fully start (the whale icon in system tray should be steady, not animated)
3. This may take 1-2 minutes on first startup

### Step 2: Verify Docker is Running
Open a **NEW** Command Prompt or PowerShell window and run:
```batch
check-docker.bat
```

This will verify Docker is properly running.

### Step 3: Start All Services
Once Docker is confirmed running, execute:
```batch
start-everything.bat
```

This script will:
1. Start Docker containers (MySQL, Kafka, Zookeeper, Redis, Kafdrop)
2. Start all Spring Boot microservices
3. Start the React frontend

## What Gets Started

### Docker Services (via Docker Compose)
- **MySQL 8.0** - Port 3306 (root/Aarav)
- **Kafka** - Port 9092
- **Zookeeper** - Port 2181
- **Redis** - Port 6379
- **Kafdrop** (Kafka UI) - http://localhost:9000

### Spring Boot Microservices
- **Customer Service** - http://localhost:8081
- **Product Pricing Service** - http://localhost:8082
- **FD Calculator Service** - http://localhost:8083
- **Accounts Service** - http://localhost:8084
- **Main Gateway** - http://localhost:8080

### Frontend
- **React App (Vite)** - http://localhost:5173

## Startup Time
- Docker services: ~30 seconds
- Spring Boot services: ~2-3 minutes per service
- React frontend: ~30 seconds

**Total estimated time: 5-7 minutes for all services to be fully operational**

## Accessing the Application

### Main Application
- Frontend: http://localhost:5173
- API Gateway: http://localhost:8080
- API Documentation: http://localhost:8080/swagger-ui.html

### Monitoring
- Kafdrop (Kafka Topics): http://localhost:9000

### Individual Service Swagger UIs
- Customer: http://localhost:8081/swagger-ui.html
- Product: http://localhost:8082/swagger-ui.html
- FD Calculator: http://localhost:8083/swagger-ui.html
- Accounts: http://localhost:8084/swagger-ui.html

## Troubleshooting

### Docker not found
If you get "docker: command not found":
1. Make sure Docker Desktop is running
2. Close your current terminal
3. Open a NEW terminal window
4. Try again

### Port already in use
If any port is busy:
```batch
# Check what's using a port (e.g., 8080)
netstat -ano | findstr :8080

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

### Service fails to start
1. Check the individual service window for error messages
2. Verify Docker services are running: `docker ps`
3. Check MySQL connection: `docker exec -it cashcached-final-bug-fix-mysql-1 mysql -uroot -pAarav`

### Frontend issues
If frontend doesn't start:
```batch
cd main\frontend
pnpm install
pnpm dev
```

## Stopping Services

### Stop Docker Services
```batch
docker compose -f compose.yaml down
```

### Stop Spring Boot Services
Close the individual command windows or press `Ctrl+C` in each window

### Stop Everything
1. Close all service command windows
2. Run: `docker compose -f compose.yaml down`

## Database Access

### MySQL
```batch
# Via Docker
docker exec -it cashcached-final-bug-fix-mysql-1 mysql -uroot -pAarav

# Via local MySQL client
mysql -h localhost -P 3306 -u root -pAarav
```

### Redis
```batch
docker exec -it cashcached-final-bug-fix-redis-1 redis-cli
```

## Configuration Files Updated
All services have been configured with MySQL password: `Aarav`
- ✅ customer/src/main/resources/application.yml
- ✅ accounts/src/main/resources/application.yml
- ✅ product-pricing/src/main/resources/application.yml
- ✅ fd-calculator/src/main/resources/application.yml
- ✅ compose.yaml

## Manual Startup (Alternative)

If you prefer to start services individually:

### 1. Start Docker Services
```batch
docker compose -f compose.yaml up -d
```

### 2. Start Backend Services
```batch
# Customer Service
cd customer
.\mvnw.cmd spring-boot:run

# Product Service (new terminal)
cd product-pricing
.\mvnw.cmd spring-boot:run

# FD Calculator (new terminal)
cd fd-calculator
.\mvnw.cmd spring-boot:run

# Accounts Service (new terminal)
cd accounts
.\mvnw.cmd spring-boot:run

# Main Gateway (new terminal)
cd main
.\mvnw.cmd spring-boot:run
```

### 3. Start Frontend
```batch
cd main\frontend
pnpm install
pnpm dev
```

## Health Checks

After all services start, verify they're running:

```batch
# Check Docker containers
docker ps

# Check Spring Boot services
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8080/actuator/health

# Check frontend
curl http://localhost:5173
```

## Need Help?
- Check individual service logs in their command windows
- View Docker logs: `docker compose -f compose.yaml logs -f`
- Check TROUBLESHOOTING-GUIDE.md for common issues
