. "$PSScriptRoot/smoke.ps1"
$target=$processes.results | Where-Object instanceId -eq prod1
$worker=$target.data[0]
$before=(Get-Api "/processes/prod1/$($worker.pid)").results[0].data
$confirmation=@{confirmation="PROD-01 / PID $($worker.pid)";expectedStartTimeUTC=$before.StartTimeUTC}
try {
    $result=Post-Api "/processes/prod1/$($worker.pid)/suspend" $confirmation
    Assert-True ($result.successCount -eq 1) 'Suspend failed'
    Assert-True ($result.results[0].instanceId -eq 'prod1') 'Action routed to incorrect instance'
    Assert-True ($result.results[0].data.after.State -match 'SUSP') 'Suspend postcondition not observed'
} finally {
    $resumed=Post-Api "/processes/prod1/$($worker.pid)/resume" $confirmation
    Assert-True ($resumed.successCount -eq 1) 'Resume failed'
}
try {
    docker compose stop iris-prod2 | Out-Null
    Assert-True ($LASTEXITCODE -eq 0) 'Could not stop demo target'
    $partial=Get-Api '/processes?instances=prod1,prod2,prod3&filter=DemoWorker'
    Assert-True ($partial.successCount -eq 2) 'Offline target collapsed fleet results'
    Assert-True (($partial.results|Where-Object instanceId -eq prod2).status -in @('OFFLINE','TIMEOUT')) 'Offline target not identified'
    $monitorPartial=Get-Api '/monitor/overview?instances=prod1,prod2,prod3'
    Assert-True ($monitorPartial.successCount -eq 2) 'Offline target collapsed overview results'
    Assert-True (($monitorPartial.results|Where-Object instanceId -eq prod2).status -in @('OFFLINE','TIMEOUT')) 'Offline overview target not identified'
} finally {
    docker compose start iris-prod2 | Out-Null
}
$recovered=$false
for($attempt=0;$attempt -lt 20;$attempt++) {
    $status=Get-Api '/instances/status'
    if($status.successCount -eq 3){$recovered=$true;break}
    Start-Sleep -Seconds 2
}
Assert-True $recovered 'Target did not recover'
Write-Host 'PASS: correct-target suspend/resume, verified state, offline isolation and recovery.'
