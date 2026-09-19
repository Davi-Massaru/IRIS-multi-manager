. "$PSScriptRoot/smoke.ps1"
foreach($kind in 'users','roles','resources'){
    $matrix=Get-Api "/resources/$kind"
    Assert-True ($matrix.fleet.successCount -eq 3) "$kind fleet failed"
    Assert-True ($matrix.rows.Count -gt 0) "$kind empty"
}
Write-Host 'PASS: users, roles and resources matrices across three instances.'
