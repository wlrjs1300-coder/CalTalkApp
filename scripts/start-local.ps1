[CmdletBinding()]
param(
    [switch]$CopySkillUrl
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$logRoot = Join-Path $repoRoot '.local\runlogs'
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

function Test-ListeningPort([int]$Port) {
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1)
}

function Wait-ListeningPort([int]$Port, [int]$TimeoutSeconds, [string]$Name) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-ListeningPort $Port) { return }
        Start-Sleep -Milliseconds 500
    }
    throw "$Name did not start on port $Port within $TimeoutSeconds seconds."
}

function Start-HiddenProcess(
    [string]$FilePath,
    [string[]]$ArgumentList,
    [string]$WorkingDirectory,
    [string]$LogName
) {
    $stdout = Join-Path $logRoot "$LogName.out.log"
    $stderr = Join-Path $logRoot "$LogName.err.log"
    Start-Process -FilePath $FilePath -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr -WindowStyle Hidden | Out-Null
}

Write-Host '[1/5] Checking Docker infrastructure...'
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    $dockerDesktop = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
    if (-not (Test-Path $dockerDesktop)) {
        throw 'Docker Desktop is not running and its executable was not found.'
    }
    Start-Process -FilePath $dockerDesktop -WindowStyle Hidden | Out-Null
    $deadline = (Get-Date).AddSeconds(120)
    do {
        Start-Sleep -Seconds 2
        docker info *> $null
    } while ($LASTEXITCODE -ne 0 -and (Get-Date) -lt $deadline)
    if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop did not become ready within 120 seconds.' }
}
docker compose --project-directory (Join-Path $repoRoot 'infra') `
    -f (Join-Path $repoRoot 'infra\compose.yaml') up -d | Out-Host
Wait-ListeningPort 5433 60 'PostgreSQL'
Wait-ListeningPort 6379 60 'Redis'

Write-Host '[2/5] Checking Ollama...'
if (-not (Test-ListeningPort 11434)) {
    $ollama = Get-Command ollama -ErrorAction SilentlyContinue
    if ($null -eq $ollama) { throw 'Ollama is not running and the ollama command was not found.' }
    Start-HiddenProcess $ollama.Source @('serve') $repoRoot 'ollama'
    Wait-ListeningPort 11434 30 'Ollama'
}

Write-Host '[3/5] Checking backend...'
$pushRunner = Join-Path $repoRoot 'backend\web-push-runner'
if (-not (Test-Path (Join-Path $pushRunner 'node_modules\web-push'))) {
    npm.cmd install --prefix $pushRunner --no-audit --no-fund | Out-Host
}
if (-not (Test-ListeningPort 8080)) {
    Start-HiddenProcess (Join-Path $repoRoot 'backend\gradlew.bat') @('bootRun', '--console=plain') `
        (Join-Path $repoRoot 'backend') 'backend'
    Wait-ListeningPort 8080 90 'Backend'
}
$backendHealth = Invoke-WebRequest -UseBasicParsing 'http://localhost:8080/actuator/health' -TimeoutSec 10
if ($backendHealth.StatusCode -ne 200) { throw 'Backend health check failed.' }

Write-Host '[4/5] Checking frontend...'
if (-not (Test-ListeningPort 5173)) {
    Start-HiddenProcess 'npm.cmd' @('run', 'dev', '--', '--host', '0.0.0.0') `
        (Join-Path $repoRoot 'frontend') 'frontend'
    Wait-ListeningPort 5173 45 'Frontend'
}

Write-Host '[5/5] Checking Kakao HTTPS tunnel...'
$tunnelUrl = $null
$cloudflaredProcesses = Get-Process cloudflared -ErrorAction SilentlyContinue
$tunnelLogs = Get-ChildItem (Join-Path $repoRoot '.local') -Filter 'cloudflared-*.err.log' `
    -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending
foreach ($log in $tunnelLogs) {
    $match = [regex]::Match((Get-Content $log.FullName -Raw), 'https://[a-z0-9-]+\.trycloudflare\.com')
    if ($match.Success) {
        try {
            $health = Invoke-WebRequest -UseBasicParsing "$($match.Value)/actuator/health" -TimeoutSec 5
            if ($health.StatusCode -eq 200) { $tunnelUrl = $match.Value; break }
        } catch { }
    }
}

if ($null -eq $tunnelUrl) {
    $cloudflared = Get-Command cloudflared -ErrorAction SilentlyContinue
    if ($null -eq $cloudflared) { throw 'cloudflared is not installed or not available on PATH.' }
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $tunnelOut = Join-Path $repoRoot ".local\cloudflared-$stamp.out.log"
    $tunnelErr = Join-Path $repoRoot ".local\cloudflared-$stamp.err.log"
    Start-Process -FilePath $cloudflared.Source -ArgumentList @(
        'tunnel', '--url', 'http://localhost:8080', '--no-autoupdate'
    ) -WorkingDirectory $repoRoot -RedirectStandardOutput $tunnelOut `
        -RedirectStandardError $tunnelErr -WindowStyle Hidden | Out-Null

    $deadline = (Get-Date).AddSeconds(45)
    do {
        Start-Sleep -Seconds 1
        $content = (Get-Content $tunnelErr -Raw -ErrorAction SilentlyContinue) +
            (Get-Content $tunnelOut -Raw -ErrorAction SilentlyContinue)
        $match = [regex]::Match($content, 'https://[a-z0-9-]+\.trycloudflare\.com')
        if ($match.Success) { $tunnelUrl = $match.Value }
    } while ($null -eq $tunnelUrl -and (Get-Date) -lt $deadline)
    if ($null -eq $tunnelUrl) { throw 'Cloudflare quick tunnel URL was not created within 45 seconds.' }
}

$skillUrl = "$tunnelUrl/api/v1/kakao/skill"
if ($CopySkillUrl) { Set-Clipboard $skillUrl }

Write-Host ''
Write-Host 'CalTalk local environment is ready.' -ForegroundColor Green
Write-Host 'Frontend:    http://localhost:5173'
Write-Host 'Backend:     http://localhost:8080'
Write-Host "Kakao skill: $skillUrl" -ForegroundColor Yellow
if ($CopySkillUrl) { Write-Host 'The Kakao skill URL was copied to the clipboard.' }
