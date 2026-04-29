$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Push-Location $projectRoot
try {
    cmd /c gradlew.bat :desktop:build -x test
    if ($LASTEXITCODE -ne 0) { throw "Build failed." }
} finally {
    Pop-Location
}

$jar = Join-Path $projectRoot "desktop\build\libs\desktop-1.0.0-SNAPSHOT.jar"
java -jar $jar
