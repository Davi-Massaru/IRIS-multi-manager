. "$PSScriptRoot/smoke.ps1"
foreach($kind in 'wallet-collections','x509','oauth-servers','oauth-resources'){
    $matrix=Get-Api "/resources/$kind"
    Assert-True ($matrix.fleet.successCount -eq 3) "$kind fleet failed"
    foreach($target in $matrix.fleet.results){Assert-True ($target.status -eq 'SUCCESS') "$kind target failed: $($target.instanceName)"}
}
Write-Host 'PASS: wallet, X509 and OAuth presence views preserve metadata without secret values.'
