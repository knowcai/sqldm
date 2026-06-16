$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PidFile = Join-Path $ScriptDir "sqldm.pid"

if (-not (Test-Path $PidFile)) {
    Write-Host "PID file not found, sqldm may not be running"
    exit 0
}

$processId = Get-Content $PidFile
$proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
if ($proc) {
    Stop-Process -Id $processId -Force
    Write-Host "Stopped sqldm (PID $processId)"
} else {
    Write-Host "Process $processId not found"
}

Remove-Item $PidFile -ErrorAction SilentlyContinue
