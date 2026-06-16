$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigFile = Join-Path $ScriptDir "config.env"
$PidFile = Join-Path $ScriptDir "sqldm.pid"
$LogDir = Join-Path $ScriptDir "logs"
$LogFile = Join-Path $LogDir "app.log"

if (-not (Test-Path $ConfigFile)) {
    Write-Host "Missing config.env. Copy config.env.example first:"
    Write-Host "  Copy-Item $ScriptDir\config.env.example $ConfigFile"
    exit 1
}

$envVars = @{}
Get-Content $ConfigFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -gt 0) {
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim()
        $envVars[$key] = $val
    }
}

$jar = Join-Path $ScriptDir "sqldm.jar"
if (-not (Test-Path $jar)) {
    $jar = Join-Path $ScriptDir "..\target\sqldm-1.0-SNAPSHOT.jar"
}
if (-not (Test-Path $jar)) {
    Write-Host "sqldm.jar not found. Run: mvn clean package -DskipTests"
    exit 1
}

if (Test-Path $PidFile) {
    $oldPid = Get-Content $PidFile -ErrorAction SilentlyContinue
    if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
        Write-Host "sqldm already running (PID $oldPid)"
        exit 0
    }
}

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

foreach ($key in $envVars.Keys) {
    Set-Item -Path "env:$key" -Value $envVars[$key]
}

if (-not $env:SPRING_DATASOURCE_URL) { throw "Set SPRING_DATASOURCE_URL in config.env" }
if (-not $env:SPRING_DATASOURCE_USERNAME) { throw "Set SPRING_DATASOURCE_USERNAME in config.env" }
if (-not $env:SPRING_DATASOURCE_PASSWORD) { throw "Set SPRING_DATASOURCE_PASSWORD in config.env" }
if (-not $env:SERVER_PORT) { $env:SERVER_PORT = "8080" }

function Resolve-JavaExe {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return Join-Path $env:JAVA_HOME "bin\java.exe"
    }
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $verLine = & $javaCmd.Source -version 2>&1 | Select-Object -First 1
        if ($verLine -match 'version "(\d+)') {
            if ([int]$Matches[1] -ge 21) { return $javaCmd.Source }
        }
    }
    throw "Java 21 required. Set JAVA_HOME in config.env (current PATH java is too old)."
}

$javaExe = Resolve-JavaExe

$proc = Start-Process -FilePath $javaExe -ArgumentList "-jar", $jar `
    -RedirectStandardOutput $LogFile -RedirectStandardError "${LogFile}.err" `
    -PassThru -WindowStyle Hidden

$proc.Id | Set-Content $PidFile
Write-Host "sqldm started (PID $($proc.Id))"
Write-Host "Port: $($env:SERVER_PORT)"
Write-Host "Log: $LogFile"
Write-Host "Health: http://localhost:$($env:SERVER_PORT)/actuator/health"
