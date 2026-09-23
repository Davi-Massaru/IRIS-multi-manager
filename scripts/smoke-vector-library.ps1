. "$PSScriptRoot/smoke.ps1"
foreach ($module in @('json','sentence_transformers','multimanager_missing_library_123')) {
    $result=Post-Api '/vector/libraries/check' @{instances='prod1,prod2,prod3';module=$module}
    Assert-True ($result.successCount -eq 3) "Library check failed: $module"
    foreach ($target in $result.results) {
        Assert-True ($target.data.module -eq $module) 'Module attribution lost'
        if ($module -eq 'json') { Assert-True ($target.data.available -eq $true) 'Standard library not found' }
        if ($module -eq 'multimanager_missing_library_123') { Assert-True ($target.data.available -eq $false) 'Missing library reported as available' }
        Write-Host "$($target.instanceName): $module = $($target.data.available)"
    }
}
$single=Post-Api '/vector/libraries/check' @{instances='prod2';module='json'}
Assert-True ($single.results.Count -eq 1 -and $single.results[0].instanceId -eq 'prod2') 'Selected instance ignored'
try { Post-Api '/vector/libraries/check' @{instances='prod1';module='os;print(1)'}|Out-Null; throw 'Invalid name accepted' } catch { if($_.Exception.Message -notmatch '400'){throw} }
Write-Host 'PASS: library discovery, missing module, selected targets and invalid input.'
