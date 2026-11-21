<#
.SYNOPSIS
    Hermetic ADB instrumented test runner and ADB utilities for Biodontos (Windows/PowerShell).

.DESCRIPTION
    Supports subcommands:
      - run (default): Runs `adb shell am instrument -w -r` with hermetic setup, timeout
        enforcement, and strict INSTRUMENTATION_STATUS_CODE parsing (-1/-2 fail, -3/-4 skip).
        Never gates on `am instrument` exit code ($LASTEXITCODE is always 0).
      - forward: `adb forward LOCAL REMOTE` (host -> device)
      - reverse: `adb reverse REMOTE LOCAL` (device -> host)
      - wifi-pair: Android 11+ `adb pair` + mDNS discovery + `adb connect` on separate port
      - devices: Lists attached transports (`adb devices -l`) and mDNS services
#>
[CmdletBinding(PositionalBinding = $false)]
param(
    [Parameter(Position = 0)]
    [string]$Subcommand = "run",

    [Parameter(Position = 1)]
    [string]$Arg1,

    [Parameter(Position = 2)]
    [string]$Arg2,

    [Parameter(Position = 3)]
    [string]$Arg3,

    [Alias("s")]
    [string]$Serial = $env:ANDROID_SERIAL,
    [switch]$Parallel,
    [Nullable[int]]$NumShards = $null,
    [Nullable[int]]$ShardIndex = $null,
    [ValidateSet("", "small", "medium", "large")]
    [string]$Size = "",
    [string]$Class = "",
    [string]$Package = "",
    [switch]$Orchestrator,
    [switch]$Install,
    [string]$AppApk = "app/build/outputs/apk/debug/app-debug.apk",
    [string]$TestApk = "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk",
    [string]$OrchestratorApk = "",
    [string]$TestServicesApk = "",
    [string[]]$ExtraArg = @(),
    [int]$TimeoutSeconds = 600,
    [int]$BootTimeoutSeconds = 120,
    [string]$ArtifactsDir = "artifacts",
    [switch]$SkipSetup
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$AppPkg = "com.example.biodontos"
$TestPkg = "com.example.biodontos.test"
$TestRunner = "$TestPkg/androidx.test.runner.AndroidJUnitRunner"
$OrchestratorRunner = "androidx.test.orchestrator/.AndroidTestOrchestrator"

$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

function Resolve-RepoPath {
    param([string]$InputPath)
    if ([string]::IsNullOrWhiteSpace($InputPath)) { return "" }
    if ([System.IO.Path]::IsPathRooted($InputPath)) { return $InputPath }
    return Join-Path $RepoRoot $InputPath
}

function Sanitize-Serial {
    param([string]$DevSerial)
    return ($DevSerial -replace '[:_/]', '-')
}

function Invoke-Adb {
    param([string[]]$AdbArgs)
    $output = & adb @AdbArgs 2>&1
    $rc = $LASTEXITCODE
    return [PSCustomObject]@{
        ExitCode = $rc
        Output   = ($output | Out-String)
    }
}

function Invoke-AdbRetry {
    param(
        [string[]]$AdbArgs,
        [int]$MaxAttempts = 3,
        [int]$DelaySeconds = 2
    )
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $res = Invoke-Adb -AdbArgs $AdbArgs
        if ($res.ExitCode -eq 0) {
            return $res
        }
        if ($attempt -ge $MaxAttempts) {
            throw "ADB command failed after $MaxAttempts attempts: adb $($AdbArgs -join ' ')`n$($res.Output)"
        }
        Write-Warning "[retry] Transient adb error (attempt $attempt/$MaxAttempts); restarting adb server..."
        & adb kill-server *> $null
        & adb start-server *> $null
        Start-Sleep -Seconds $DelaySeconds
    }
}

function Invoke-AdbBinaryExecOut {
    param(
        [string]$DevSerial,
        [string[]]$ExecOutArgs,
        [string]$OutFilePath
    )
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "adb"
    $allArgs = @("-s", $DevSerial, "exec-out") + $ExecOutArgs
    $psi.Arguments = ($allArgs | ForEach-Object {
        if ($_ -match '\s') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ }
    }) -join ' '
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true

    $proc = [System.Diagnostics.Process]::Start($psi)
    try {
        $fs = [System.IO.File]::Create($OutFilePath)
        try {
            $proc.StandardOutput.BaseStream.CopyTo($fs)
        }
        finally {
            $fs.Dispose()
        }
        $null = $proc.StandardError.ReadToEnd()
        $proc.WaitForExit()
        return $proc.ExitCode
    }
    finally {
        $proc.Dispose()
    }
}

function Invoke-AdbWithTimeout {
    param(
        [string[]]$AdbArgs,
        [int]$TimeoutSec,
        [string]$OutFilePath
    )
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "adb"
    $psi.Arguments = ($AdbArgs | ForEach-Object {
        if ($_ -match '\s') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ }
    }) -join ' '
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true

    $proc = [System.Diagnostics.Process]::Start($psi)
    try {
        $stdoutTask = $proc.StandardOutput.ReadToEndAsync()
        $stderrTask = $proc.StandardError.ReadToEndAsync()
        $completed = $proc.WaitForExit($TimeoutSec * 1000)
        if (-not $completed) {
            try { $proc.Kill() } catch {}
            $partialOut = ""
            try { $partialOut = $stdoutTask.GetAwaiter().GetResult() } catch {}
            [System.IO.File]::WriteAllText($OutFilePath, $partialOut, (New-Object System.Text.UTF8Encoding($false)))
            return 124
        }
        $proc.WaitForExit()
        $fullOut = $stdoutTask.GetAwaiter().GetResult() + $stderrTask.GetAwaiter().GetResult()
        [System.IO.File]::WriteAllText($OutFilePath, $fullOut, (New-Object System.Text.UTF8Encoding($false)))
        return $proc.ExitCode
    }
    finally {
        $proc.Dispose()
    }
}

function Wait-ForDeviceBoot {
    param(
        [string]$DevSerial,
        [int]$MaxWaitSec
    )
    Write-Host "[$DevSerial] Waiting for device transport..."
    $null = Invoke-AdbRetry -AdbArgs @("-s", $DevSerial, "wait-for-device")

    Write-Host "[$DevSerial] Polling sys.boot_completed == 1 (timeout ${MaxWaitSec}s)..."
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($true) {
        $prop = (& adb -s $DevSerial shell getprop sys.boot_completed 2>$null | Out-String).Trim()
        if ($prop -eq "1") { break }
        if ($sw.Elapsed.TotalSeconds -ge $MaxWaitSec) {
            throw "[$DevSerial] Device did not complete boot within ${MaxWaitSec}s."
        }
        Start-Sleep -Seconds 1
    }
    & adb -s $DevSerial shell input keyevent KEYCODE_WAKEUP *> $null
    & adb -s $DevSerial shell input keyevent 82 *> $null
}

function Invoke-HermeticPreamble {
    param([string]$DevSerial)
    Write-Host "[$DevSerial] Applying hermetic preamble (pm clear, force-stop, animation scales=0)..."
    $null = Invoke-AdbRetry -AdbArgs @("-s", $DevSerial, "shell", "pm", "clear", $AppPkg)
    & adb -s $DevSerial shell pm clear $TestPkg *> $null
    & adb -s $DevSerial shell am force-stop $AppPkg *> $null
    $null = Invoke-AdbRetry -AdbArgs @("-s", $DevSerial, "shell", "settings", "put", "global", "window_animation_scale", "0")
    $null = Invoke-AdbRetry -AdbArgs @("-s", $DevSerial, "shell", "settings", "put", "global", "transition_animation_scale", "0")
    $null = Invoke-AdbRetry -AdbArgs @("-s", $DevSerial, "shell", "settings", "put", "global", "animator_duration_scale", "0")
    & adb -s $DevSerial logcat -c *> $null
}

function Save-FailureArtifacts {
    param(
        [string]$DevSerial,
        [string]$OutDir
    )
    if (-not (Test-Path $OutDir)) {
        New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
    }
    Write-Host "[$DevSerial] Capturing failure diagnostics into $OutDir ..."

    # Binary-clean screencap via exec-out FileStream (avoids PowerShell CRLF/UTF-16 corruption)
    try { $null = Invoke-AdbBinaryExecOut -DevSerial $DevSerial -ExecOutArgs @("screencap", "-p") -OutFilePath (Join-Path $OutDir "fail.png") } catch {}
    try { (& adb -s $DevSerial logcat -d -v threadtime 2>$null) | Set-Content -Path (Join-Path $OutDir "logcat.txt") -Encoding UTF8 } catch {}
    try { (& adb -s $DevSerial logcat -d -b crash -v threadtime 2>$null) | Set-Content -Path (Join-Path $OutDir "crash.txt") -Encoding UTF8 } catch {}
    try { (& adb -s $DevSerial logcat -d -b events -v descriptive 2>$null) | Set-Content -Path (Join-Path $OutDir "events.txt") -Encoding UTF8 } catch {}
    try { (& adb -s $DevSerial shell dumpsys activity 2>$null) | Set-Content -Path (Join-Path $OutDir "dumpsys-activity.txt") -Encoding UTF8 } catch {}

    # Debuggable app private storage grab via exec-out run-as + tar
    $tarOut = Join-Path $OutDir "app-data.tar"
    $appDataDir = Join-Path $OutDir "app-data"
    try {
        $tarRc = Invoke-AdbBinaryExecOut -DevSerial $DevSerial -ExecOutArgs @("run-as", $AppPkg, "tar", "cf", "-", ".") -OutFilePath $tarOut
        if ($tarRc -eq 0 -and (Test-Path $tarOut) -and ((Get-Item $tarOut).Length -gt 0) -and (Get-Command tar -ErrorAction SilentlyContinue)) {
            New-Item -ItemType Directory -Path $appDataDir -Force | Out-Null
            & tar -xf $tarOut -C $appDataDir 2>$null
            if ($LASTEXITCODE -eq 0) {
                Remove-Item $tarOut -Force -ErrorAction SilentlyContinue
            }
        }
    }
    catch {}
}

function Restore-DeviceState {
    param([string]$DevSerial)
    try { & adb -s $DevSerial shell am force-stop $AppPkg *> $null } catch {}
    try { & adb -s $DevSerial shell pm clear $AppPkg *> $null } catch {}
    try { & adb -s $DevSerial shell settings put global window_animation_scale 1 *> $null } catch {}
    try { & adb -s $DevSerial shell settings put global transition_animation_scale 1 *> $null } catch {}
    try { & adb -s $DevSerial shell settings put global animator_duration_scale 1 *> $null } catch {}
    try { & adb -s $DevSerial forward --remove-all *> $null } catch {}
    try { & adb -s $DevSerial reverse --remove-all *> $null } catch {}
}

function Test-InstrumentationLog {
    param([string]$LogPath)
    $lines = [System.IO.File]::ReadAllLines($LogPath, [System.Text.Encoding]::UTF8)
    $passed = 0; $errors = 0; $failures = 0; $ignored = 0; $assumption = 0
    $finalSeen = $false; $finalCode = ""

    foreach ($raw in $lines) {
        $line = $raw.TrimEnd("`r")
        if ($line -match '^INSTRUMENTATION_STATUS_CODE:\s*(?<code>-?\d+)$') {
            switch ($Matches['code']) {
                "0"  { $passed++ }
                "-1" { $errors++ }
                "-2" { $failures++ }
                "-3" { $ignored++ }
                "-4" { $assumption++ }
            }
        }
        elseif ($line -match '^INSTRUMENTATION_CODE:\s*(?<code>-?\d+)$') {
            $finalSeen = $true
            $finalCode = $Matches['code']
        }
    }

    $finalLabel = if ($finalSeen) { $finalCode } else { "MISSING" }
    Write-Host "Results: passed=$passed, failures(-2)=$failures, errors(-1)=$errors, ignored(-3)=$ignored, assumption_skipped(-4)=$assumption, INSTRUMENTATION_CODE=$finalLabel"
    return ($errors -eq 0 -and $failures -eq 0 -and $finalSeen -and $finalCode -ne "0")
}

function Invoke-SingleDeviceRun {
    param(
        [string]$DevSerial,
        [Nullable[int]]$ShardIdx,
        [Nullable[int]]$TotalShards,
        [string]$ArtifactsRoot
    )
    $devDir = Join-Path $ArtifactsRoot (Sanitize-Serial $DevSerial)
    if (-not (Test-Path $devDir)) {
        New-Item -ItemType Directory -Path $devDir -Force | Out-Null
    }
    $logFile = Join-Path $devDir "instrument.log"

    $args = New-Object System.Collections.Generic.List[string]
    $args.Add("-s"); $args.Add($DevSerial)
    $args.Add("shell"); $args.Add("am"); $args.Add("instrument"); $args.Add("-w"); $args.Add("-r")

    if (-not [string]::IsNullOrWhiteSpace($Size)) { $args.Add("-e"); $args.Add("size"); $args.Add($Size) }
    if (-not [string]::IsNullOrWhiteSpace($Class)) { $args.Add("-e"); $args.Add("class"); $args.Add($Class) }
    if (-not [string]::IsNullOrWhiteSpace($Package)) { $args.Add("-e"); $args.Add("package"); $args.Add($Package) }
    if ($null -ne $TotalShards -and $null -ne $ShardIdx) {
        $args.Add("-e"); $args.Add("numShards"); $args.Add($TotalShards.ToString())
        $args.Add("-e"); $args.Add("shardIndex"); $args.Add($ShardIdx.ToString())
    }
    foreach ($kv in $ExtraArg) {
        if ([string]::IsNullOrWhiteSpace($kv)) { continue }
        $idx = $kv.IndexOf('=')
        if ($idx -gt 0) {
            $args.Add("-e")
            $args.Add($kv.Substring(0, $idx))
            $args.Add($kv.Substring($idx + 1))
        }
    }

    if ($Orchestrator) {
        $args.Add("-e"); $args.Add("clearPackageData"); $args.Add("true")
        $args.Add("-e"); $args.Add("targetInstrumentation"); $args.Add($TestRunner)
        $args.Add($OrchestratorRunner)
    }
    else {
        $args.Add($TestRunner)
    }

    Write-Host "[$DevSerial] Executing: adb $($args -join ' ')"
    $rc = Invoke-AdbWithTimeout -AdbArgs $args.ToArray() -TimeoutSec $TimeoutSeconds -OutFilePath $logFile

    if ($rc -eq 124) {
        Write-Error "[$DevSerial] am instrument timed out after ${TimeoutSeconds}s."
        Save-FailureArtifacts -DevSerial $DevSerial -OutDir $devDir
        return 124
    }
    if ($rc -ne 0) {
        Write-Error "[$DevSerial] adb transport failure running am instrument (exit=$rc)."
        if (Test-Path $logFile) { Get-Content $logFile | Write-Host }
        Save-FailureArtifacts -DevSerial $DevSerial -OutDir $devDir
        return $rc
    }

    # Never trust am instrument $LASTEXITCODE (always 0); evaluate parsed status codes
    $ok = Test-InstrumentationLog -LogPath $logFile
    if (-not $ok) {
        Write-Warning "[$DevSerial] FAILED: Instrumentation tests reported errors/failures or runner crash."
        Save-FailureArtifacts -DevSerial $DevSerial -OutDir $devDir
        return 1
    }

    Write-Host "[$DevSerial] PASSED."
    return 0
}

switch ($Subcommand.ToLowerInvariant()) {
    "devices" {
        & adb devices -l
        Write-Host ""
        & adb mdns services
        exit $LASTEXITCODE
    }
    "forward" {
        if ([string]::IsNullOrWhiteSpace($Arg1) -or [string]::IsNullOrWhiteSpace($Arg2)) {
            throw "Usage: run-instrumented-adb.ps1 forward [-Serial SERIAL] <LOCAL> <REMOTE>"
        }
        Write-Host "Setting up adb forward (HOST -> DEVICE): LOCAL=$Arg1 REMOTE=$Arg2"
        if (-not [string]::IsNullOrWhiteSpace($Serial)) {
            & adb -s $Serial forward $Arg1 $Arg2
        }
        else {
            & adb forward $Arg1 $Arg2
        }
        exit $LASTEXITCODE
    }
    "reverse" {
        if ([string]::IsNullOrWhiteSpace($Arg1) -or [string]::IsNullOrWhiteSpace($Arg2)) {
            throw "Usage: run-instrumented-adb.ps1 reverse [-Serial SERIAL] <REMOTE> <LOCAL>"
        }
        Write-Host "Setting up adb reverse (DEVICE -> HOST): REMOTE=$Arg1 LOCAL=$Arg2"
        if (-not [string]::IsNullOrWhiteSpace($Serial)) {
            & adb -s $Serial reverse $Arg1 $Arg2
        }
        else {
            & adb reverse $Arg1 $Arg2
        }
        exit $LASTEXITCODE
    }
    "wifi-pair" {
        if ([string]::IsNullOrWhiteSpace($Arg1) -or [string]::IsNullOrWhiteSpace($Arg2)) {
            throw "Usage: run-instrumented-adb.ps1 wifi-pair <HOST:PAIR_PORT> <PAIR_CODE> [HOST:CONNECT_PORT]"
        }
        Write-Host "Checking mDNS daemon status..."
        & adb mdns check
        Write-Host "Discovered mDNS services:"
        & adb mdns services
        Write-Host "Pairing with $Arg1 ..."
        & adb pair $Arg1 $Arg2
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

        $connectAddr = $Arg3
        if ([string]::IsNullOrWhiteSpace($connectAddr)) {
            $pairIp = ($Arg1 -split ':')[0]
            $mdnsOut = & adb mdns services 2>$null
            foreach ($line in $mdnsOut) {
                if ($line -match "_adb-tls-connect\._tcp\s+(?<addr>$([regex]::Escape($pairIp)):\d+)") {
                    $connectAddr = $Matches['addr']
                    break
                }
            }
        }
        if (-not [string]::IsNullOrWhiteSpace($connectAddr)) {
            Write-Host "Connecting to separate debug port $connectAddr ..."
            & adb connect $connectAddr
            & adb devices -l
        }
        else {
            Write-Host "Paired successfully. Find the separate connect port under Wireless debugging and run: adb connect <HOST:CONNECT_PORT>"
        }
        exit 0
    }
    "run" {
        # Handled below
    }
    "help" {
        Get-Help $MyInvocation.MyCommand.Path -Detailed
        exit 0
    }
    default {
        throw "Unknown subcommand '$Subcommand'. Supported: run, forward, reverse, wifi-pair, devices, help."
    }
}

& adb start-server *> $null

$targetSerials = New-Object System.Collections.Generic.List[string]
if (-not [string]::IsNullOrWhiteSpace($Serial)) {
    $targetSerials.Add($Serial)
}
else {
    $devLines = & adb devices
    foreach ($line in $devLines | Select-Object -Skip 1) {
        if ($line -match '^(?<ser>\S+)\s+device(\s|$)') {
            $targetSerials.Add($Matches['ser'])
        }
    }
}

if ($targetSerials.Count -eq 0) {
    throw "No devices in 'device' state found. Run 'adb devices -l'."
}
if ($targetSerials.Count -gt 1 -and -not $Parallel -and [string]::IsNullOrWhiteSpace($Serial)) {
    throw "Multiple devices attached ($($targetSerials -join ', ')). Specify -Serial <SERIAL> or pass -Parallel."
}

$artifactsFullDir = Resolve-RepoPath $ArtifactsDir
$appApkFull = Resolve-RepoPath $AppApk
$testApkFull = Resolve-RepoPath $TestApk
$orchApkFull = Resolve-RepoPath $OrchestratorApk
$svcApkFull = Resolve-RepoPath $TestServicesApk

$overallExit = 0
try {
    if (-not $SkipSetup) {
        foreach ($s in $targetSerials) {
            Wait-ForDeviceBoot -DevSerial $s -MaxWaitSec $BootTimeoutSeconds
            if ($Install) {
                if (-not (Test-Path $appApkFull)) { throw "App APK not found: $appApkFull" }
                if (-not (Test-Path $testApkFull)) { throw "Test APK not found: $testApkFull" }
                Write-Host "[$s] Installing app APK (-r -g) and androidTest APK (-r -t -g)..."
                $null = Invoke-AdbRetry -AdbArgs @("-s", $s, "install", "-r", "-g", $appApkFull)
                $null = Invoke-AdbRetry -AdbArgs @("-s", $s, "install", "-r", "-t", "-g", $testApkFull)
                if (-not [string]::IsNullOrWhiteSpace($orchApkFull)) {
                    $null = Invoke-AdbRetry -AdbArgs @("-s", $s, "install", "--force-queryable", "-r", $orchApkFull)
                }
                if (-not [string]::IsNullOrWhiteSpace($svcApkFull)) {
                    $null = Invoke-AdbRetry -AdbArgs @("-s", $s, "install", "--force-queryable", "-r", $svcApkFull)
                }
            }
            Invoke-HermeticPreamble -DevSerial $s
        }
    }

    if ($Parallel -and $targetSerials.Count -gt 1) {
        $jobs = New-Object System.Collections.Generic.List[object]
        $scriptPath = $MyInvocation.MyCommand.Path
        for ($i = 0; $i -lt $targetSerials.Count; $i++) {
            $s = $targetSerials[$i]
            $jobArgs = @{
                Serial         = $s
                NumShards      = $targetSerials.Count
                ShardIndex     = $i
                Size           = $Size
                Class          = $Class
                Package        = $Package
                Orchestrator   = [bool]$Orchestrator
                ExtraArg       = $ExtraArg
                TimeoutSeconds = $TimeoutSeconds
                ArtifactsDir   = $artifactsFullDir
            }
            $job = Start-Job -ScriptBlock {
                param($p, $ja)
                & $p -Serial $ja.Serial -NumShards $ja.NumShards -ShardIndex $ja.ShardIndex -Size $ja.Size -Class $ja.Class -Package $ja.Package -Orchestrator:($ja.Orchestrator) -ExtraArg $ja.ExtraArg -TimeoutSeconds $ja.TimeoutSeconds -ArtifactsDir $ja.ArtifactsDir -SkipSetup
                if ($LASTEXITCODE -ne 0) {
                    throw "Instrumentation failed on $($ja.Serial) (exit=$LASTEXITCODE)"
                }
            } -ArgumentList $scriptPath, $jobArgs
            $jobs.Add($job)
        }
        Wait-Job -Job $jobs | Out-Null
        foreach ($job in $jobs) {
            try {
                Receive-Job -Job $job -ErrorAction Stop
            }
            catch {
                Write-Warning $_.Exception.Message
                $overallExit = 1
            }
            if ($job.State -ne 'Completed') {
                $overallExit = 1
            }
            Remove-Job -Job $job -Force
        }
    }
    else {
        $overallExit = Invoke-SingleDeviceRun -DevSerial $targetSerials[0] -ShardIdx $ShardIndex -TotalShards $NumShards -ArtifactsRoot $artifactsFullDir
    }
}
finally {
    if (-not $SkipSetup) {
        foreach ($s in $targetSerials) {
            Restore-DeviceState -DevSerial $s
        }
        try { & adb forward --remove-all *> $null } catch {}
        try { & adb reverse --remove-all *> $null } catch {}
    }
}

exit $overallExit
