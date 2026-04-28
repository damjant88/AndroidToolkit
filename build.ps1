$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Push-Location $projectRoot
try {
    cmd /c gradlew.bat :desktop:build -x test
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed."
    }
    Write-Host "Build completed: desktop\build\libs\desktop-1.0.0-SNAPSHOT.jar"
} finally {
    Pop-Location
}
