# Script de verificacion de pureza de la capa Domain (Biodontos)
# Valida que no existan importaciones de Android en el paquete domain

$DomainPath = "app\src\main\java\com\example\biodontos\domain"

if (-not (Test-Path $DomainPath)) {
    Write-Error "Ruta de dominio no encontrada: $DomainPath"
    exit 1
}

Write-Host "Iniciando analisis de pureza arquitectonica en $DomainPath..." -ForegroundColor Cyan

$Violations = @()
$Files = Get-ChildItem -Path $DomainPath -Filter "*.kt" -Recurse

foreach ($File in $Files) {
    $Content = Get-Content $File.FullName
    $LineNumber = 1
    foreach ($Line in $Content) {
        if ($Line -match "import\s+(android\.|androidx\.)") {
            $Violations += [PSCustomObject]@{
                File = $File.Name
                Line = $LineNumber
                Import = $Line.Trim()
            }
        }
        $LineNumber++
    }
}

if ($Violations.Count -gt 0) {
    Write-Host ""
    Write-Host "VIOLACIONES ARQUITECTONICAS ENCONTRADAS:" -ForegroundColor Red
    foreach ($V in $Violations) {
        Write-Host "  -> Archivo: $($V.File) (Linea $($V.Line)): $($V.Import)" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Fallo: La capa de dominio debe ser Kotlin puro sin dependencias de Android." -ForegroundColor Red
    exit 1
} else {
    Write-Host ""
    Write-Host "Verificacion exitosa: 0 dependencias de Android encontradas en la capa de Dominio." -ForegroundColor Green
    exit 0
}
