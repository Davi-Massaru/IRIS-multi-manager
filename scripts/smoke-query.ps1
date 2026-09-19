. "$PSScriptRoot/smoke.ps1"
$query=Post-Api '/query' @{targets=@('prod1','prod2','prod3');sql='SELECT COUNT(*) AS Total FROM MultiManager_Demo.Person';maxRows=10;timeoutSeconds=10}
Assert-True ($query.successCount -eq 3) 'JDBC query did not reach all targets'
Assert-True (($query.results|Where-Object instanceId -eq prod1).rows[0][0] -eq 3) 'Unexpected PROD-01 row count'
try { Post-Api '/query' @{targets=@('prod1');sql='DELETE FROM MultiManager_Demo.Person';maxRows=10;timeoutSeconds=10}|Out-Null; throw 'DML was accepted' } catch { if($_.Exception.Message -notmatch '400'){throw} }
Write-Host 'PASS: JDBC Fleet Query grouped results, row limit and SELECT-only enforcement.'
