param([string]$Module = "app", [double]$MinCov = 65.0)

$ErrorActionPreference = "Stop"
$stamp = Get-Date -Format "yyyy-MM-dd_HHmm"
$evi = "06_Plan_de_Pruebas\Evidencia\Unidad\$stamp"
New-Item -ItemType Directory -Force -Path $evi | Out-Null

# 1) Tests + cobertura
.\gradlew :$Module:testDebugUnitTest :$Module:jacocoTestReport -i *>&1 | Tee-Object "$evi\console.txt"

# 2) Copiar reportes
Copy-Item -Recurse -Force "$Module\build\reports\tests\testDebugUnitTest" "$evi\tests"
Copy-Item -Recurse -Force "$Module\build\reports\jacoco\jacocoTestReport" "$evi\jacoco"

# 3) FAILs
$failXml = Get-ChildItem "$Module\build\test-results\testDebugUnitTest" -Filter *.xml -Recurse
$failCount = 0
foreach ($f in $failXml) {
  [xml]$x = Get-Content $f.FullName
  $failCount += [int]$x.testsuite.failures + [int]$x.testsuite.errors
}

# 4) Cobertura LINE
$jacocoXml = Join-Path "$Module\build\reports\jacoco\jacocoTestReport" "jacocoTestReport.xml"
if (!(Test-Path $jacocoXml)) { "No se encontró jacocoTestReport.xml" | Tee-Object "$evi\resumen.txt"; exit 2 }
[xml]$jx = Get-Content $jacocoXml
$lineNode = $jx.report.counter | Where-Object { $_.type -eq "LINE" }
if (-not $lineNode) { "Sin counter LINE en JaCoCo." | Tee-Object "$evi\resumen.txt"; exit 2 }
$covered = [double]$lineNode.covered
$missed  = [double]$lineNode.missed
$cov = if (($covered+$missed) -gt 0) { $covered/($covered+$missed) } else { 0 }
$covPct = [math]::Round($cov*100,2)

# 5) Resumen + umbral
"`n==== RESUMEN UNIDAD ($stamp) ====" | Tee-Object -FilePath "$evi\resumen.txt" -Append
"Fallos totales: $failCount" | Tee-Object -FilePath "$evi\resumen.txt" -Append
"Cobertura LINE JaCoCo: $covPct %" | Tee-Object -FilePath "$evi\resumen.txt" -Append
"Reportes: $evi" | Tee-Object -FilePath "$evi\resumen.txt" -Append

if ($failCount -gt 0 -or $covPct -lt $MinCov) {
  "ALERTA: NO APROBADO (criterios de salida no cumplidos)" | Tee-Object -FilePath "$evi\resumen.txt" -Append
  exit 1
} else {
  "OK: Suite aprobada según criterios." | Tee-Object -FilePath "$evi\resumen.txt" -Append
  exit 0
}
