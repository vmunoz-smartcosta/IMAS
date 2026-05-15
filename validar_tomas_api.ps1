param(
    [string]$BaseUrl = "http://172.16.1.198/WCFActiveIntegration/Service1.svc",
    [int]$TimeoutSec = 120
)

$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$headers = @{
    "Content-Type" = "application/json; charset=utf-8"
}

$endpoints = @(
    "ObtenerTomaFisica",
    "ObtenerTipoToma",
    "ObtenerTomaDetalle"
)

function Get-ResultSummary {
    param(
        [string]$Endpoint,
        [object]$ResponseObject
    )

    $resultPropName = ($ResponseObject.PSObject.Properties.Name | Where-Object { $_ -like "*Result" } | Select-Object -First 1)
    $count = 0
    $sample = $null

    if ($resultPropName) {
        $resultValue = $ResponseObject.$resultPropName
        if ($resultValue -is [System.Array]) {
            $count = $resultValue.Count
            if ($count -gt 0) { $sample = $resultValue[0] }
        } elseif ($null -ne $resultValue) {
            $count = 1
            $sample = $resultValue
        }
    }

    [pscustomobject]@{
        Endpoint   = $Endpoint
        ResultKey  = $resultPropName
        Count      = $count
        FirstItem  = $sample
    }
}

Write-Host ""
Write-Host "BaseUrl: $BaseUrl"
Write-Host "TimeoutSec: $TimeoutSec"
Write-Host ""

$allOk = $true
$summaries = @()

foreach ($ep in $endpoints) {
    $url = "$BaseUrl/$ep"
    Write-Host "POST $url"
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $url -Method Post -Headers $headers -Body '{}' -TimeoutSec $TimeoutSec
        $obj = $response.Content | ConvertFrom-Json
        $summary = Get-ResultSummary -Endpoint $ep -ResponseObject $obj
        $summaries += $summary
        Write-Host ("  OK status={0} resultKey={1} count={2}" -f $response.StatusCode, $summary.ResultKey, $summary.Count)
        if ($null -ne $summary.FirstItem) {
            Write-Host "  Primer item:"
            $summary.FirstItem | ConvertTo-Json -Depth 8
        }
    } catch {
        $allOk = $false
        Write-Host ("  ERROR {0}" -f $_.Exception.Message)
    }
    Write-Host ""
}

Write-Host "Resumen:"
$summaries | Select-Object Endpoint, ResultKey, Count | Format-Table -AutoSize

if (-not $allOk) {
    exit 1
}

exit 0
