param([string]$BaseUrl='http://localhost:8080')
$ErrorActionPreference='Stop'
$session=[Microsoft.PowerShell.Commands.WebRequestSession]::new()
$headers=@{'X-Requested-With'='IRIS-Multi-Manager'}
function Assert-True($condition,[string]$message) { if(-not $condition){throw $message} }
function Get-Api([string]$path) { Invoke-RestMethod "$BaseUrl/api$path" -WebSession $session -TimeoutSec 45 }
function Post-Api([string]$path,$body) { Invoke-RestMethod "$BaseUrl/api$path" -Method Post -ContentType application/json -Headers $headers -Body ($body|ConvertTo-Json -Depth 12 -Compress) -WebSession $session -TimeoutSec 45 }
Assert-True ((Invoke-WebRequest $BaseUrl -TimeoutSec 10).StatusCode -eq 200) 'Frontend unavailable'
Assert-True ((Get-Api '/health').status -eq 'UP') 'Backend unavailable'
$compose=(& docker compose -f (Join-Path $PSScriptRoot '../docker-compose.yml') config --format json | ConvertFrom-Json)
$password=$compose.configs.iris_password.content
Assert-True (-not [string]::IsNullOrWhiteSpace($password)) 'Demo password missing from Compose config'
try {
    $instances=Get-Api '/instances'
    Assert-True ($instances.Count -eq 3) 'Expected three demo targets'
    foreach($instance in $instances) {
        $connected=Post-Api "/instances/$($instance.id)/connect" @{username='_SYSTEM';password=$password}
        Assert-True ($connected.data.apiVersion -eq 2) "SysAdmin info failed: $($instance.name)"
    }
} finally {$password=$null}
$status=Get-Api '/instances/status'
Assert-True ($status.successCount -eq 3) 'Instance validation failed'
$processes=Get-Api '/processes?instances=prod1,prod2,prod3&filter=DemoWorker'
Assert-True ($processes.successCount -eq 3) 'Process aggregation failed'
foreach($target in $processes.results){
    Assert-True (@($target.data).Count -eq 1) "Expected one demo worker: $($target.instanceName)"
    Assert-True ($target.data[0].instanceId -eq $target.instanceId) 'Process attribution lost'
    $detail=Get-Api "/processes/$($target.instanceId)/$($target.data[0].pid)"
    Assert-True ($detail.successCount -eq 1) 'Process detail failed'
}
Write-Host 'PASS: frontend, backend, three SysAdmin info calls, unified worker search, instance attribution and process detail.'
