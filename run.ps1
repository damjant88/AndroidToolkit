$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$outRoot = Join-Path $projectRoot "out"
$storageRoot = "C:\AdbToolkit"

if (-not (Test-Path $outRoot)) {
    & (Join-Path $projectRoot "build.ps1")
}

if (-not (Test-Path $storageRoot)) {
    New-Item -ItemType Directory -Path $storageRoot | Out-Null
}

java -cp "$outRoot;Assets" androidtoolkit.app.AdbToolkit
