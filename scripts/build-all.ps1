# Build every version target sequentially; jars land in build/libs/<target>/.
# Usage: powershell -File scripts\build-all.ps1  (optional: -Targets 26.3,1.21.8)
param(
    [string]$Targets = ""
)

$allTargets = Get-ChildItem "$PSScriptRoot\..\versions" -Directory | Select-Object -ExpandProperty Name
if ($Targets -ne "") {
    $allTargets = $Targets -split ','
}

$failed = @()
foreach ($t in $allTargets) {
    Write-Host "=== Building target $t ===" -ForegroundColor Cyan
    & "$PSScriptRoot\..\gradlew.bat" "-Ptarget=$t" build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "=== Target $t FAILED ===" -ForegroundColor Red
        $failed += $t
    }
}

if ($failed.Count -gt 0) {
    Write-Error "Failed targets: $($failed -join ', ')"
    exit 1
}
Write-Host "=== All targets built ===" -ForegroundColor Green
