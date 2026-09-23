. "$PSScriptRoot/smoke.ps1"
$inventory=Get-Api '/vector/assets?instances=prod1,prod2,prod3'
Assert-True ($inventory.successCount -eq 3) 'Vector inventory did not reach all targets'
$assets=@($inventory.results|ForEach-Object {$_.data.assets})
Assert-True (($assets|Where-Object kind -eq 'VECTOR').Count -ge 1) 'VECTOR asset was not discovered'
Assert-True (($assets|Where-Object kind -eq 'EMBEDDING').Count -ge 2) 'EMBEDDING demo assets were not discovered'
foreach($target in $inventory.results) { Assert-True ($target.data.extension.embeddedPython -eq $true) "Embedded Python extension unavailable: $($target.instanceName)" }
$prod1=$inventory.results|Where-Object instanceId -eq prod1
Assert-True ($prod1.data.models[0].configuration.apiKey -eq '[REDACTED]') 'Embedding configuration secret reached the client'
$asset=$prod1.data.assets|Where-Object table -eq 'EmbeddingDocument'|Select-Object -First 1
$rows=Get-Api "/vector/assets/prod1/$($asset.assetId)/rows?includeVector=false"
Assert-True ($rows.results[0].data.rows[0][-1] -eq '[vector hidden]') 'Vector was exposed without explicit request'
$test=Post-Api '/vector/models/test' @{instanceId='prod1';text='IRIS vector smoke test'}
Assert-True ($test.results[0].data.dimensions -eq 4) 'Embedded Python SqlProc did not return the demo vector'
$check=Post-Api '/vector/models/check' @{instanceId='prod1';model='sentence-transformers/all-MiniLM-L6-v2'}
Assert-True ($check.results[0].data.runtimeInstalled -eq $true) 'SentenceTransformers runtime missing from PROD-01'
$download=Post-Api '/vector/models/download' @{instanceId='prod1';model='sentence-transformers/all-MiniLM-L6-v2'}
Assert-True ($download.results[0].data.downloaded -eq $false) 'Offline demo unexpectedly downloaded a model'
$prod3=$inventory.results|Where-Object instanceId -eq prod3
$custom=$prod3.data.assets|Where-Object table -eq 'Document'|Select-Object -First 1
$plan=Post-Api '/vector/regeneration/preview' @{instanceId='prod3';assetId=$custom.assetId;rowId=1}
$regenerated=Post-Api '/vector/actions/apply' @{planId=$plan.planId;confirmation=$plan.confirmation}
Assert-True ($regenerated.results[0].data.affectedRows -eq 1) 'Selected vector regeneration failed'
try { Post-Api '/vector/actions/apply' @{planId=$plan.planId;confirmation=$plan.confirmation}|Out-Null;throw 'Plan token replay was accepted' } catch { if($_.Exception.Message -notmatch '409'){throw} }
$create=Post-Api '/vector/indexes/preview' @{instanceId='prod3';assetId=$custom.assetId;action='CREATE';indexName='SmokeHNSW';distance='Cosine';m=16;efConstruction=64}
$created=Post-Api '/vector/actions/apply' @{planId=$create.planId;confirmation=$create.confirmation}
Assert-True ($created.successCount -eq 1) 'HNSW create failed'
$withIndex=Get-Api '/vector/assets?instances=prod3'
Assert-True ($withIndex.results[0].data.assets[0].indexes[0].name -eq 'SmokeHNSW') 'Created HNSW was not discovered'
$drop=Post-Api '/vector/indexes/preview' @{instanceId='prod3';assetId=$custom.assetId;action='DROP';indexName='SmokeHNSW'}
$dropped=Post-Api '/vector/actions/apply' @{planId=$drop.planId;confirmation=$drop.confirmation}
Assert-True ($dropped.successCount -eq 1) 'HNSW drop failed'
Write-Host 'PASS: vector inventory, config sanitization, source explorer, model controls, SqlProc, regeneration, single-use plans and HNSW lifecycle.'
