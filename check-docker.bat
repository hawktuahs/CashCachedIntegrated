@echo off
echo Checking Docker Desktop status...
echo.

REM Try to run docker command
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker command not found in PATH.
    echo.
    echo Please follow these steps:
    echo 1. Open Docker Desktop from Start Menu
    echo 2. Wait for Docker Desktop to fully start (whale icon in system tray should be steady)
    echo 3. Close this window and open a NEW PowerShell/CMD window
    echo 4. Run: docker --version
    echo 5. If it works, run start-everything.bat
    echo.
    pause
    exit /b 1
)

echo Docker version:
docker --version
echo.

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker daemon is not running.
    echo.
    echo Please:
    echo 1. Start Docker Desktop from Start Menu
    echo 2. Wait for it to fully start (whale icon should be steady in system tray)
    echo 3. Run this script again
    echo.
    pause
    exit /b 1
)

echo [SUCCESS] Docker is running!
echo.
echo Docker info:
docker info | findstr "Server Version"
docker info | findstr "Operating System"
echo.
echo You can now run: start-everything.bat
echo.
pause
