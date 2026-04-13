# generar_tabla_unidad.ps1
param(
  [string]$Base = "C:\Users\Abigu\OneDrive\Escritorio\Auditoria_IronProgress\06_Plan_de_Pruebas\Evidencia\Unidad"
)

# Convierte "UT_VAL_001_..." -> "UT-VAL-001"
function Convert-MethodNameToId([string]$name) {
  if ($name -match '^UT_([A-Z]+)_([0-9]{3})') { return "UT-$($Matches[1])-$($Matches[2])" }
  return $null
}

# IDs en el orden de tu matriz
$ExpectedIds = @(
  "UT-VAL-001","UT-MAP-002","UT-LOG-003","UT-MUS-004","UT-MUS-005",
  "UT-SEL-006","UT-IMG-007","UT-OFF-008","UT-OFF-009","UT-UI-010",
  "UT-ADP-011","UT-AUTH-012","UT-RULES-013"
)

# Carpeta de evidencia más reciente (yyyy-MM-dd_HH-mm-ss)
$latest = Get-ChildItem -Path $Base -Directory -ErrorAction Stop |
          Sort-Object Name -Descending | Select-Object -First 1
if (-not $latest) { Write-Error "No encontré subcarpetas dentro de: $Base"; exit 1 }

$root   = Join-Path $Base $latest.Name
$xmlDir = Join-Path $root "JUnit_XML_y_Streams"
if (-not (Test-Path $xmlDir)) { Write-Error "No encontré JUnit XML en: $xmlDir (¿corrieron evidenciaUnidadAll?)"; exit 1 }

# Parsear XMLs → mapa: ID -> estado/nota
$results = @{}

Get-ChildItem -Path $xmlDir -Filter *.xml -Recurse | ForEach-Object {
  try { [xml]$doc = Get-Content -LiteralPath $_.FullName -Encoding UTF8 } catch { return }
  $testcases = $doc.SelectNodes("//testcase")
  foreach ($tc in $testcases) {
    $fullName = if ($tc.name) { "$($tc.name)" } else { "" }
    $id = Convert-MethodNameToId $fullName
    if (-not $id) { continue }

    if (-not $results.ContainsKey($id)) { $results[$id] = @{ status="PASS"; notes="" } }

    if ($tc.failure -or $tc.error) {
      $results[$id].status = "FAIL"
      $msg = ""
      if ($tc.failure) { $msg = (("$($tc.failure.message) $($tc.failure.InnerText)").Trim()) }
      elseif ($tc.error) { $msg = (("$($tc.error.message) $($tc.error.InnerText)").Trim()) }
      if (-not $results[$id].notes) { $results[$id].notes = $msg }
    }
  }
}

# Armar filas
$today = Get-Date -Format "yyyy-MM-dd"
$evidencePath = $root

$rows = foreach ($id in $ExpectedIds) {
  $status = if ($results.ContainsKey($id)) { $results[$id].status } else { "PASS" }
  $notes  = if ($results.ContainsKey($id)) { $results[$id].notes } else { "" }
  [pscustomobject]@{
    ID                 = $id
    Fecha              = $today
    Estado             = $status
    "Evidencia (ruta)" = $evidencePath
    "Notas/Defecto"    = $notes
    "Corrección/Commit"= ""
  }
}

# Guardar CSV y Markdown
$csvPath = Join-Path $root "resumen_unidad.csv"
$mdPath  = Join-Path $root "resumen_unidad.md"

$rows | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $csvPath

$md = "| ID | Fecha | Estado (PASS/FAIL) | Evidencia (ruta) | Notas/Defecto | Corrección/Commit |`n"
$md += "|---|---|---|---|---|---|`n"
foreach ($r in $rows) {
  $id    = $r.ID
  $fecha = $r.Fecha
  $estado= $r.Estado
  $evi   = ($r."Evidencia (ruta)").Replace("|","\|")
  $nota  = (($r."Notas/Defecto") -replace "\r?\n"," ").Trim()
  $corr  = $r."Corrección/Commit"
  $md += "| $id | $fecha | $estado | $evi | $nota | $corr |`n"
}
Set-Content -Path $mdPath -Value $md -Encoding UTF8

Write-Host "OK  CSV: $csvPath"
Write-Host "OK  MD : $mdPath"
Write-Host "OK  Carpeta evidencia: $evidencePath"
