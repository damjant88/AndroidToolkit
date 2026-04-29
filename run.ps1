$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Building React frontend..."
Push-Location (Join-Path $projectRoot "frontend")
try {
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed." }
} finally {
    Pop-Location
}

Write-Host "Building Spring Boot backend (with frontend bundled)..."
Push-Location $projectRoot
try {
    cmd /c gradlew.bat :backend:bootJar
    if ($LASTEXITCODE -ne 0) { throw "Backend build failed." }
} finally {
    Pop-Location
}

$jar = Join-Path $projectRoot "backend\build\libs\backend-1.0.0-SNAPSHOT.jar"
Write-Host ""
Write-Host "Starting AndroidToolkit Web App..."
Write-Host "Open: http://localhost:8080"
Write-Host ""

java "-Djava.awt.headless=false" -jar $jar
