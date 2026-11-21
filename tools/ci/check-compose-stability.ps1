<#
.SYNOPSIS
    Validates Jetpack Compose composable stability against a committed baseline.

.DESCRIPTION
    Reads Compose compiler output from app/build/compose_compiler/app_debug-composables.txt
    and app_debug-module.json (plus optional app_debug-composables.csv for FQN resolution).
    Excludes @Preview functions (name ending in Preview or inside presentation.previews).

    - Default mode: Compares current compiler output against app/stability/app.stability.
      Reports '+' (added), '-' (removed), and '~' (regression: lost skippable or parameter
      degraded from stable to unstable). Exits with code 1 if baseline is missing or '~'
      regressions are detected.
    - -UpdateBaseline mode: Writes deterministic baseline to app/stability/app.stability.
#>
[CmdletBinding()]
param(
    [switch]$UpdateBaseline,
    [string]$ComposeReportsDir = "app/build/compose_compiler",
    [string]$BaselinePath = "app/stability/app.stability"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

function Resolve-RepoPath {
    param([string]$InputPath)
    if ([System.IO.Path]::IsPathRooted($InputPath)) {
        return $InputPath
    }
    return Join-Path $RepoRoot $InputPath
}

$ReportsFullDir = Resolve-RepoPath $ComposeReportsDir
$BaselineFullPath = Resolve-RepoPath $BaselinePath
$ComposablesTxtPath = Join-Path $ReportsFullDir "app_debug-composables.txt"
$ComposablesCsvPath = Join-Path $ReportsFullDir "app_debug-composables.csv"
$ModuleJsonPath = Join-Path $ReportsFullDir "app_debug-module.json"

if (-not (Test-Path $ComposablesTxtPath)) {
    Write-Error "Missing Compose compiler report: $ComposablesTxtPath. Run './gradlew :app:compileDebugKotlin' first."
    exit 1
}

if (-not (Test-Path $ModuleJsonPath)) {
    Write-Error "Missing Compose module metrics: $ModuleJsonPath. Run './gradlew :app:compileDebugKotlin' first."
    exit 1
}

$ModuleMetrics = Get-Content -Path $ModuleJsonPath -Raw -Encoding UTF8 | ConvertFrom-Json

function Parse-ComposablesTxt {
    param(
        [string]$TxtPath,
        [string]$CsvPath
    )

    $lines = [System.IO.File]::ReadAllLines($TxtPath, [System.Text.Encoding]::UTF8)
    $parsed = New-Object System.Collections.Generic.List[object]

    $inFun = $false
    $inDefaultBlock = $false
    $current = $null

    foreach ($rawLine in $lines) {
        $line = $rawLine.TrimEnd("`r")

        if ($line -match '^(?<prefix>(?:(?:restartable|skippable|readonly|inline)\s+)*)(?:scheme\([^)]*\)\s+)?fun\s+(?<name>[^\s(]+)\((?<rest>.*)$') {
            if ($inFun -and $null -ne $current) {
                $parsed.Add($current)
            }

            $prefix = $Matches['prefix']
            $funcName = $Matches['name']
            $rest = $Matches['rest']

            $current = [PSCustomObject]@{
                Name        = $funcName
                Id          = $funcName
                Restartable = ($prefix -match '\brestartable\b')
                Skippable   = ($prefix -match '\bskippable\b')
                Params      = New-Object System.Collections.Generic.List[object]
            }

            if ($rest -eq ')') {
                $parsed.Add($current)
                $current = $null
                $inFun = $false
                $inDefaultBlock = $false
            }
            else {
                $inFun = $true
                $inDefaultBlock = $false
            }
            continue
        }

        if ($inFun) {
            if ($line -match '^\)') {
                if ($null -ne $current) {
                    $parsed.Add($current)
                    $current = $null
                }
                $inFun = $false
                $inDefaultBlock = $false
                continue
            }

            if ($inDefaultBlock) {
                if ($line -match '^tmp[0-9]+_group@') {
                    $inDefaultBlock = $false
                }
                continue
            }

            if ($line -match '^  (?:unused\s+)?(?:(?<stab>stable|unstable|uncertain)\s+)?(?<pname><this>|[A-Za-z_][A-Za-z0-9_]*):\s*(?<ptype>.+)$') {
                $stab = if ([string]::IsNullOrEmpty($Matches['stab'])) { "uncertain" } else { $Matches['stab'] }
                $pname = $Matches['pname']
                $rawType = $Matches['ptype']

                if ($rawType -match '\$composer\.startReplaceGroup\(') {
                    $inDefaultBlock = $true
                }

                $cleanType = ($rawType -replace '\s*=\s*@(?:static|dynamic)\s+.*$', '').Trim()
                $current.Params.Add([PSCustomObject]@{
                    Stability = $stab
                    Name      = $pname
                    Type      = $cleanType
                })
            }
        }
    }

    if ($inFun -and $null -ne $current) {
        $parsed.Add($current)
    }

    # Correlate with app_debug-composables.csv when available for FQN and package exclusion
    if (Test-Path $CsvPath) {
        $csvRows = Import-Csv -Path $CsvPath -Encoding UTF8
        if ($csvRows.Count -eq $parsed.Count) {
            $allMatch = $true
            for ($i = 0; $i -lt $parsed.Count; $i++) {
                if ($csvRows[$i].name -ne $parsed[$i].Name) {
                    $allMatch = $false
                    break
                }
            }
            if ($allMatch) {
                for ($i = 0; $i -lt $parsed.Count; $i++) {
                    if (-not [string]::IsNullOrWhiteSpace($csvRows[$i].package)) {
                        $parsed[$i].Id = $csvRows[$i].package
                    }
                }
            }
        }
    }

    # Filter out @Preview composables and presentation.previews package entries
    $excludedCount = 0
    $filtered = New-Object System.Collections.Generic.List[object]
    foreach ($item in $parsed) {
        $isPreviewName = $item.Name.EndsWith("Preview", [System.StringComparison]::Ordinal)
        $isPreviewPkg = ($item.Id -match '(^|\.)presentation\.previews(\.|$)')
        if ($isPreviewName -or $isPreviewPkg) {
            $excludedCount++
            continue
        }
        $filtered.Add($item)
    }

    # Disambiguate rare same-package overloads deterministically
    $idCounts = @{}
    foreach ($item in $filtered) {
        if (-not $idCounts.ContainsKey($item.Id)) {
            $idCounts[$item.Id] = 0
        }
        $idCounts[$item.Id]++
    }
    foreach ($item in $filtered) {
        if ($idCounts[$item.Id] -gt 1) {
            $paramNames = ($item.Params | ForEach-Object { $_.Name }) -join ","
            $item.Id = "$($item.Id)($paramNames)"
        }
    }

    $sorted = $filtered | Sort-Object -Property @{ Expression = { $_.Id }; Ascending = $true }
    # Enforce exact ASCII/Ordinal sort matching LC_ALL=C
    $sortedArray = @($sorted)
    [System.Array]::Sort($sortedArray, [System.Comparison[object]]{
        param($a, $b)
        return [System.StringComparer]::Ordinal.Compare($a.Id, $b.Id)
    })

    return [PSCustomObject]@{
        Entries       = $sortedArray
        ExcludedCount = $excludedCount
    }
}

function Parse-BaselineFile {
    param([string]$Path)

    $lines = [System.IO.File]::ReadAllLines($Path, [System.Text.Encoding]::UTF8)
    $map = @{}
    $orderedIds = New-Object System.Collections.Generic.List[string]
    $current = $null

    foreach ($rawLine in $lines) {
        $line = $rawLine.TrimEnd("`r")
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
            continue
        }
        if ($line -match '^@composable\s+(?<id>\S+)\s+\[restartable=(?<rest>true|false),\s*skippable=(?<skip>true|false)\]$') {
            $id = $Matches['id']
            $current = [PSCustomObject]@{
                Id          = $id
                Restartable = ($Matches['rest'] -eq 'true')
                Skippable   = ($Matches['skip'] -eq 'true')
                Params      = New-Object System.Collections.Generic.List[object]
            }
            $map[$id] = $current
            $orderedIds.Add($id)
            continue
        }
        if ($null -ne $current -and $line -match '^  - (?<stab>stable|unstable|uncertain)\s+(?<pname><this>|[A-Za-z_][A-Za-z0-9_]*):\s*(?<ptype>.+)$') {
            $current.Params.Add([PSCustomObject]@{
                Stability = $Matches['stab']
                Name      = $Matches['pname']
                Type      = $Matches['ptype'].Trim()
            })
        }
    }

    return [PSCustomObject]@{
        Map = $map
        Ids = $orderedIds
    }
}

$parsedResult = Parse-ComposablesTxt -TxtPath $ComposablesTxtPath -CsvPath $ComposablesCsvPath
$currentEntries = $parsedResult.Entries
$excludedCount = $parsedResult.ExcludedCount

if ($UpdateBaseline) {
    $parentDir = Split-Path -Parent $BaselineFullPath
    if (-not [string]::IsNullOrWhiteSpace($parentDir) -and -not (Test-Path $parentDir)) {
        New-Item -ItemType Directory -Path $parentDir -Force | Out-Null
    }

    $strongSkipping = $false
    if ($null -ne $ModuleMetrics.featureFlags -and $null -ne $ModuleMetrics.featureFlags.StrongSkipping) {
        $strongSkipping = [bool]$ModuleMetrics.featureFlags.StrongSkipping
    }

    $sb = New-Object System.Text.StringBuilder
    [void]$sb.Append("# Biodontos Compose Stability Baseline`n")
    [void]$sb.Append("# Source: app/build/compose_compiler/app_debug-composables.txt`n")
    [void]$sb.Append("#`n")
    [void]$sb.Append("# Module Summary:`n")
    [void]$sb.Append("#   totalComposables: $($ModuleMetrics.totalComposables)`n")
    [void]$sb.Append("#   restartableComposables: $($ModuleMetrics.restartableComposables)`n")
    [void]$sb.Append("#   skippableComposables: $($ModuleMetrics.skippableComposables)`n")
    [void]$sb.Append("#   knownStableArguments: $($ModuleMetrics.knownStableArguments)`n")
    [void]$sb.Append("#   knownUnstableArguments: $($ModuleMetrics.knownUnstableArguments)`n")
    [void]$sb.Append("#   effectivelyStableClasses: $($ModuleMetrics.effectivelyStableClasses) / $($ModuleMetrics.totalClasses)`n")
    [void]$sb.Append("#   strongSkipping: $($strongSkipping.ToString().ToLowerInvariant())`n")
    [void]$sb.Append("#   trackedComposables: $($currentEntries.Count) (excluded Previews: $excludedCount)`n")
    [void]$sb.Append("`n")

    foreach ($entry in $currentEntries) {
        $restStr = $entry.Restartable.ToString().ToLowerInvariant()
        $skipStr = $entry.Skippable.ToString().ToLowerInvariant()
        [void]$sb.Append("@composable $($entry.Id) [restartable=$restStr, skippable=$skipStr]`n")
        foreach ($p in $entry.Params) {
            [void]$sb.Append("  - $($p.Stability) $($p.Name): $($p.Type)`n")
        }
        [void]$sb.Append("`n")
    }

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($BaselineFullPath, $sb.ToString(), $utf8NoBom)

    Write-Host "Updated Compose stability baseline at: $BaselineFullPath"
    Write-Host "Tracked composables: $($currentEntries.Count) (excluded Previews: $excludedCount)"
    exit 0
}

# Check mode
if (-not (Test-Path $BaselineFullPath)) {
    Write-Error @"
Compose stability baseline not found at: $BaselineFullPath
Generate and review the baseline before running checks:
  1. ./gradlew :app:compileDebugKotlin
  2. .\tools\ci\check-compose-stability.ps1 -UpdateBaseline
  3. Review and commit app/stability/app.stability
"@
    exit 1
}

$baseline = Parse-BaselineFile -Path $BaselineFullPath
$currentMap = @{}
foreach ($entry in $currentEntries) {
    $currentMap[$entry.Id] = $entry
}

$allIdsSet = New-Object System.Collections.Generic.HashSet[string]([System.StringComparer]::Ordinal)
foreach ($id in $baseline.Ids) { [void]$allIdsSet.Add($id) }
foreach ($entry in $currentEntries) { [void]$allIdsSet.Add($entry.Id) }

$allIds = New-Object string[] $allIdsSet.Count
$allIdsSet.CopyTo($allIds)
[System.Array]::Sort($allIds, [System.StringComparer]::Ordinal)

$added = New-Object System.Collections.Generic.List[string]
$removed = New-Object System.Collections.Generic.List[string]
$regressions = New-Object System.Collections.Generic.List[string]
$improvements = New-Object System.Collections.Generic.List[string]

foreach ($id in $allIds) {
    $inBase = $baseline.Map.ContainsKey($id)
    $inCurr = $currentMap.ContainsKey($id)

    if (-not $inBase -and $inCurr) {
        $c = $currentMap[$id]
        $added.Add("+ $id [restartable=$($c.Restartable.ToString().ToLowerInvariant()), skippable=$($c.Skippable.ToString().ToLowerInvariant())]")
        continue
    }

    if ($inBase -and -not $inCurr) {
        $removed.Add("- $id")
        continue
    }

    $b = $baseline.Map[$id]
    $c = $currentMap[$id]
    $reasons = New-Object System.Collections.Generic.List[string]
    $impReasons = New-Object System.Collections.Generic.List[string]

    if ($b.Skippable -and -not $c.Skippable) {
        $reasons.Add("lost skippable (true -> false)")
    }
    elseif (-not $b.Skippable -and $c.Skippable) {
        $impReasons.Add("gained skippable (false -> true)")
    }

    $currParamByName = @{}
    foreach ($cp in $c.Params) {
        if (-not $currParamByName.ContainsKey($cp.Name)) {
            $currParamByName[$cp.Name] = $cp
        }
    }

    foreach ($bp in $b.Params) {
        if ($currParamByName.ContainsKey($bp.Name)) {
            $cp = $currParamByName[$bp.Name]
            if ($bp.Stability -eq "stable" -and $cp.Stability -eq "unstable") {
                $reasons.Add("param '$($bp.Name)' degraded (stable -> unstable: $($cp.Type))")
            }
            elseif ($bp.Stability -eq "unstable" -and $cp.Stability -eq "stable") {
                $impReasons.Add("param '$($bp.Name)' improved (unstable -> stable: $($cp.Type))")
            }
        }
    }

    if ($reasons.Count -gt 0) {
        $regressions.Add("~ $id`: $($reasons -join '; ')")
    }
    elseif ($impReasons.Count -gt 0) {
        $improvements.Add("^ $id`: $($impReasons -join '; ')")
    }
}

Write-Host "=== Biodontos Compose Stability Check ==="
Write-Host "Baseline: $BaselineFullPath ($($baseline.Ids.Count) tracked)"
Write-Host "Current : $ComposablesTxtPath ($($currentEntries.Count) tracked, $excludedCount Preview(s) excluded)"

if ($added.Count -gt 0) {
    Write-Host "`nAdded composables ($($added.Count)):"
    $added | ForEach-Object { Write-Host "  $_" }
}

if ($removed.Count -gt 0) {
    Write-Host "`nRemoved composables ($($removed.Count)):"
    $removed | ForEach-Object { Write-Host "  $_" }
}

if ($improvements.Count -gt 0) {
    Write-Host "`nStability improvements ($($improvements.Count)):"
    $improvements | ForEach-Object { Write-Host "  $_" }
}

if ($regressions.Count -gt 0) {
    Write-Host "`nStability REGRESSIONS detected ($($regressions.Count)):"
    $regressions | ForEach-Object { Write-Host "  $_" }
    Write-Error "Compose stability check FAILED with $($regressions.Count) regression(s). Fix instability or justify and run -UpdateBaseline."
    exit 1
}

Write-Host "`nCompose stability check PASSED (0 regressions; +$($added.Count) added, -$($removed.Count) removed)."
exit 0
