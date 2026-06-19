# Local one-shot runner: Android JVM + coverage gate, then Functions + rules.
# Bails on the first failure.
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Write-Host "==> Android JVM tests + coverage" -ForegroundColor Cyan
./gradlew --no-daemon testDebugUnitTest koverXmlReport koverVerify
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "==> Cloud Functions (emulator + Jest)" -ForegroundColor Cyan
Set-Location "$repoRoot/functions"
if (-not (Test-Path "node_modules")) { npm install }
npm test
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "==> Firestore rules" -ForegroundColor Cyan
npm run test:rules
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Set-Location $repoRoot
Write-Host "==> All test layers passed." -ForegroundColor Green
