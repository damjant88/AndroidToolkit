$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$outRoot = Join-Path $projectRoot "out"
$distRoot = Join-Path $projectRoot "dist"
$stagingRoot = Join-Path $distRoot "staging"
$inputRoot = Join-Path $distRoot "input"
$jarPath = Join-Path $inputRoot "AndroidToolkit.jar"
$assetsRoot = Join-Path $projectRoot "Assets"

& (Join-Path $projectRoot "build.ps1")

if (Test-Path $distRoot) {
    Remove-Item $distRoot -Recurse -Force
}

New-Item -ItemType Directory -Path $stagingRoot | Out-Null
New-Item -ItemType Directory -Path $inputRoot | Out-Null

Copy-Item -Path (Join-Path $outRoot "*") -Destination $stagingRoot -Recurse -Force
Copy-Item -Path (Join-Path $assetsRoot "*") -Destination $stagingRoot -Recurse -Force

Push-Location $stagingRoot
try {
    jar --create --file $jarPath --main-class androidtoolkit.app.AdbToolkit .
} finally {
    Pop-Location
}

if ($LASTEXITCODE -ne 0) {
    throw "Jar packaging failed."
}

jpackage `
    --type app-image `
    --name AndroidToolkit `
    --input $inputRoot `
    --main-jar AndroidToolkit.jar `
    --dest $distRoot

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed."
}

$launcherPath = Join-Path $distRoot "AndroidToolkit\AndroidToolkit.exe"
Write-Host "Windows launcher created at $launcherPath"
