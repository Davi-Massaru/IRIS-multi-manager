. "$PSScriptRoot/smoke.ps1"
$matrix=Get-Api '/resources/web-apps?instances=prod1,prod2,prod3'
$internal=$matrix.rows|Where-Object resourceKey -eq '/api/internal'
Assert-True (($internal.targets|Where-Object instanceId -eq prod1).presence -eq 'PRESENT') 'PROD-01 presence incorrect'
Assert-True (($internal.targets|Where-Object instanceId -eq prod3).presence -eq 'MISSING') 'PROD-03 missing resource not detected'
$before=Get-Api '/resources/web-apps/detail?instances=prod1,prod2&name=%2Fapi%2Fdemo'
$description='Verified fleet change '+[guid]::NewGuid().ToString('N')
$preview=Post-Api '/web-apps/preview' @{targets=@('prod1','prod2');name='/api/demo';changes=@{Description=$description}}
Assert-True ($preview.targets.successCount -eq 2) 'Web-app preflight failed'
$unchanged=Get-Api '/resources/web-apps/detail?instances=prod1&name=%2Fapi%2Fdemo'
Assert-True ($unchanged.results[0].data.Description -ne $description) 'Preflight performed a write'
try {
    $result=Post-Api "/web-apps/execute/$($preview.operationId)" @{confirmed=$true}
    Assert-True ($result.successCount -eq 2) 'Web-app write/verification failed'
    foreach($target in $result.results){Assert-True ($target.data.Description -eq $description) 'Postcondition mismatch'}
    $replayRejected=$false
    try {Post-Api "/web-apps/execute/$($preview.operationId)" @{confirmed=$true}|Out-Null} catch {$replayRejected=$_.Exception.Response.StatusCode.value__ -eq 409}
    Assert-True $replayRejected 'Consumed preview could be replayed'
} finally {
    foreach($target in $before.results){
        $restore=Post-Api '/web-apps/preview' @{targets=@($target.instanceId);name='/api/demo';changes=@{Description=$target.data.Description}}
        $restored=Post-Api "/web-apps/execute/$($restore.operationId)" @{confirmed=$true}
        Assert-True ($restored.successCount -eq 1) 'Test fixture restoration failed'
    }
}
Write-Host 'PASS: presence matrix, preview without writes, two-target write and readback verification, one-use confirmation.'
