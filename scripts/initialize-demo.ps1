$ErrorActionPreference = 'Stop'
$runtimeDirectory = Join-Path $PSScriptRoot '../.runtime'
New-Item -ItemType Directory -Force $runtimeDirectory | Out-Null
$passwordPath = Join-Path $runtimeDirectory 'iris-password'
if (-not (Test-Path -LiteralPath $passwordPath)) {
    $bytes = [byte[]]::new(32)
    [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    [IO.File]::WriteAllText($passwordPath, [Convert]::ToBase64String($bytes))
}
Write-Host 'Demo password prepared in ignored .runtime/iris-password.'
