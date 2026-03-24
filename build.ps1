$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$srcRoot = Join-Path $projectRoot "src"
$outRoot = Join-Path $projectRoot "out"

if (Test-Path $outRoot) {
    Remove-Item $outRoot -Recurse -Force
}

New-Item -ItemType Directory -Path $outRoot | Out-Null

$javaFiles = Get-ChildItem -Path $srcRoot -Recurse -Filter *.java | ForEach-Object { $_.FullName }

if (-not $javaFiles) {
    throw "No Java source files were found under $srcRoot."
}

javac -d $outRoot $javaFiles

if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed."
}

Write-Host "Build completed in $outRoot"
