# start-saas.ps1 — Start AndroidToolkit in SaaS mode (local Gradle + Docker infra)
# Usage: .\start-saas.ps1
# Stop:  .\start-saas.ps1 -Stop

param(
    [switch]$Stop
)

$ErrorActionPreference = "Continue"

if ($Stop) {
    Write-Host "Stopping all components..." -ForegroundColor Yellow
    docker compose -f docker-compose.infra.yml down
    Get-Process -Name node -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-Host "Infrastructure stopped. Kill Gradle processes manually if needed (Ctrl+C in their terminals)." -ForegroundColor Yellow
    exit 0
}

Write-Host "=== AndroidToolkit SaaS Mode ===" -ForegroundColor Cyan
Write-Host ""

# 1. Start infrastructure (PostgreSQL + MinIO)
Write-Host "[1/4] Starting PostgreSQL + MinIO..." -ForegroundColor Green
docker compose -f docker-compose.infra.yml up -d

Write-Host "Waiting for PostgreSQL to be ready..."
$retries = 0
do {
    Start-Sleep -Seconds 2
    $retries++
    $ready = docker compose -f docker-compose.infra.yml exec -T postgres pg_isready -U androidtoolkit 2>$null
} while ($LASTEXITCODE -ne 0 -and $retries -lt 15)

if ($retries -ge 15) {
    Write-Host "ERROR: PostgreSQL did not become ready in time." -ForegroundColor Red
    exit 1
}
Write-Host "PostgreSQL ready." -ForegroundColor Green

# 2. Start Backend
Write-Host ""
Write-Host "[2/4] Starting Backend (SaaS profile)..." -ForegroundColor Green
$env:SPRING_PROFILES_ACTIVE = "saas"
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/androidtoolkit"
$env:DATABASE_USERNAME = "androidtoolkit"
$env:DATABASE_PASSWORD = "devpassword"
$env:STORAGE_ENDPOINT = "http://localhost:9000"
$env:STORAGE_BUCKET = "androidtoolkit"
$env:STORAGE_ACCESS_KEY = "minioadmin"
$env:STORAGE_SECRET_KEY = "minioadmin"
$env:JWT_SECRET = "dev-secret-key-at-least-32-characters-long-for-showcase"
$env:CORS_ALLOWED_ORIGINS = "*"

Start-Process -NoNewWindow powershell -ArgumentList "-Command", "cd '$PSScriptRoot'; .\gradlew.bat :backend:bootRun"

# 3. Start Agent
Write-Host "[3/4] Starting Agent..." -ForegroundColor Green
Start-Process -NoNewWindow powershell -ArgumentList "-Command", "cd '$PSScriptRoot'; .\gradlew.bat :agent:bootRun"

# 4. Start Frontend
Write-Host "[4/4] Starting Frontend..." -ForegroundColor Green
Start-Process -NoNewWindow powershell -ArgumentList "-Command", "cd '$PSScriptRoot\frontend'; npm start"

Write-Host ""
Write-Host "=== All components starting ===" -ForegroundColor Cyan
Write-Host "  Backend:    http://localhost:8080  (SaaS/PostgreSQL)" -ForegroundColor White
Write-Host "  Frontend:   http://localhost:3000" -ForegroundColor White
Write-Host "  Agent:      http://localhost:8081" -ForegroundColor White
Write-Host "  MinIO:      http://localhost:9001  (admin: minioadmin/minioadmin)" -ForegroundColor White
Write-Host "  Swagger:    http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host ""
Write-Host "To stop: .\start-saas.ps1 -Stop" -ForegroundColor Yellow
