. "$PSScriptRoot/smoke.ps1"
$system=Get-Api '/system/summary?instances=prod1,prod2,prod3'
Assert-True ($system.successCount -eq 3) 'System dashboard did not reach all targets'
foreach($target in $system.results){Assert-True ($target.data -ne $null) "System data missing for $($target.instanceName)"}
$events=Get-Api '/events?instances=prod1,prod2,prod3&maxRows=10'
Assert-True ($events.requestedTargets -eq 3) 'Event fanout did not target all instances'
Assert-True ($events.results.Count -eq 3) 'Event rows lost instance attribution'
Write-Host 'PASS: system resources and audit event fanout preserve per-instance results.'
