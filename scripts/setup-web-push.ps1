[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repoRoot 'backend\.env'
if (-not (Test-Path -LiteralPath $envPath)) {
    Copy-Item -LiteralPath (Join-Path $repoRoot 'backend\.env.example') -Destination $envPath
}

$generated = npx.cmd --yes web-push generate-vapid-keys --json | ConvertFrom-Json
if (-not $generated.publicKey -or -not $generated.privateKey) {
    throw 'VAPID key generation failed.'
}

$content = Get-Content -LiteralPath $envPath -Raw
function Set-EnvValue([string]$Name, [string]$Value) {
    $script:content = if ($script:content -match "(?m)^$([regex]::Escape($Name))=") {
        [regex]::Replace($script:content, "(?m)^$([regex]::Escape($Name))=.*$", "$Name=$Value")
    } else {
        $script:content.TrimEnd() + "`r`n$Name=$Value`r`n"
    }
}

Set-EnvValue 'WEB_PUSH_VAPID_PUBLIC_KEY' $generated.publicKey
Set-EnvValue 'WEB_PUSH_VAPID_PRIVATE_KEY' $generated.privateKey
if ($content -notmatch '(?m)^WEB_PUSH_VAPID_SUBJECT=') {
    Set-EnvValue 'WEB_PUSH_VAPID_SUBJECT' 'mailto:caltalk-local@example.com'
}
[IO.File]::WriteAllText($envPath, $content, [Text.UTF8Encoding]::new($false))
Write-Host 'Web Push keys were generated and stored in backend/.env.' -ForegroundColor Green
Write-Host 'Restart the backend before enabling notifications.'
