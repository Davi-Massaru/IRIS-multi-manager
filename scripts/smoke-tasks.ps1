. "$PSScriptRoot/smoke.ps1"
$matrix=Get-Api '/resources/tasks?instances=prod1,prod2,prod3'
$audit=$matrix.rows|Where-Object resourceKey -eq DemoAudit
Assert-True (($audit.targets|Where-Object instanceId -eq prod2).presence -eq 'PRESENT') 'DemoAudit not present on PROD-02'
Assert-True (($audit.targets|Where-Object instanceId -eq prod1).presence -eq 'MISSING') 'DemoAudit incorrectly present on PROD-01'
foreach($action in 'suspend','resume','run'){
    $preview=Post-Api '/tasks/preview' @{targets=@('prod1','prod2');name='DemoCleanup';action=$action}
    Assert-True ($preview.targets.successCount -eq 2) "Task $action preflight failed"
    $result=Post-Api "/tasks/execute/$($preview.operationId)" @{confirmed=$true}
    Assert-True ($result.successCount -eq 2) "Task $action execution failed"
}
Write-Host 'PASS: task presence, exact-target task IDs, preflight and confirmed suspend/resume/run.'
