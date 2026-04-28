$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolkitDir = "C:\AdbToolkit"

if (-not (Test-Path $toolkitDir)) {
    New-Item -ItemType Directory -Path $toolkitDir | Out-Null
}

Push-Location $projectRoot
try {
    cmd /c gradlew.bat :desktop:build -x test
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed."
    }

    $jar = Join-Path $projectRoot "desktop\build\libs\desktop-1.0.0-SNAPSHOT.jar"
    java -jar $jar
} finally {
    Pop-Location
}
