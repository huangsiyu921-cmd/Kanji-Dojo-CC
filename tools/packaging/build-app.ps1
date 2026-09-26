<#
.SYNOPSIS
    One-shot packaging of the Kanji Dojo CC desktop app for the platform this script runs on.

.DESCRIPTION
    Wraps the Compose Desktop packaging tasks:

        Windows -> :desktopApp:packageMsi   (desktopApp/build/compose/binaries/main/msi)
        Linux   -> :desktopApp:packageDeb   (desktopApp/build/compose/binaries/main/deb)
        macOS   -> :desktopApp:packageDmg   (desktopApp/build/compose/binaries/main/dmg)

    jpackage cannot cross-compile, therefore each target has to be built on its own OS: the script
    fails with an explanation when asked for a platform it is not running on.

    Not covered here (on purpose):
      * Android - release builds have their own signed flow, `:app:assembleFdroidRelease`.
      * iOS     - VOICEVOX on iOS needs an xcframework plus a Kotlin/Native cinterop binding, which
                  is macOS-only work and is still open (TTS-HANDOFF.md, M5). Use Xcode and the
                  iosApp project until then.

    The VOICEVOX runtime files (~180 MB, tools/voicevox/runtime) are *not* packaged into the
    installer yet - that is the M3 milestone. Until then a packaged build falls back to the system
    Japanese voice when the runtime directory is missing.

.NOTES
    Build only: this script runs no git command and uploads nothing.

.EXAMPLE
    pwsh tools/packaging/build-app.ps1                # package for this OS
    pwsh tools/packaging/build-app.ps1 -DryRun        # print the Gradle task graph instead
#>

[CmdletBinding()]
param(
    [ValidateSet('auto', 'windows', 'linux', 'macos')]
    [string] $Target = 'auto',
    # Ask Gradle to print the task graph instead of building (useful to check the setup).
    [switch] $DryRun,
    # Fail when the VOICEVOX runtime for this platform is missing instead of only warning.
    [switch] $RequireRuntime
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

function Get-HostTarget {
    if ($env:OS -eq 'Windows_NT') { return 'windows' }
    if (Test-Path '/System/Library/CoreServices') { return 'macos' }
    return 'linux'
}

function Get-PackageSpec {
    param([string] $Platform)
    switch ($Platform) {
        'windows' { return @{ Task = 'packageMsi'; Output = 'msi' } }
        'linux'   { return @{ Task = 'packageDeb'; Output = 'deb' } }
        'macos'   { return @{ Task = 'packageDmg'; Output = 'dmg' } }
        default   { throw "Unsupported target '$Platform'" }
    }
}

function Get-ArchId {
    $raw = if ($env:PROCESSOR_ARCHITEW6432) {
        $env:PROCESSOR_ARCHITEW6432
    } elseif ($env:PROCESSOR_ARCHITECTURE) {
        $env:PROCESSOR_ARCHITECTURE
    } else {
        (& uname -m 2>$null)
    }
    switch -Regex ("$raw".ToLower()) {
        '^(amd64|x86_64)$' { return 'x64' }
        '^(arm64|aarch64)$' { return 'arm64' }
        '^(x86|i[3-6]86)$' { return 'x86' }
        default { return "$raw".ToLower() }
    }
}

function Get-RuntimeLibrary {
    param([string] $Platform)
    $name = switch -Wildcard ($Platform) {
        'windows*' { 'voicevox_onnxruntime.dll' }
        'macos*'   { 'libvoicevox_onnxruntime.dylib' }
        default    { 'libvoicevox_onnxruntime.so' }
    }
    # Runtime directories are named <os>-<arch>, as in VoicevoxConfig.platformId.
    $platformId = "$Platform-$(Get-ArchId)"
    return Join-Path $repoRoot "tools/voicevox/runtime/core/onnxruntime/lib/$platformId/$name"
}

$hostTarget = Get-HostTarget
$platform = if ($Target -eq 'auto') { $hostTarget } else { $Target }

if ($platform -ne $hostTarget) {
    throw "Cannot build '$platform' on '$hostTarget': jpackage has no cross-compilation. " +
        "Run this script on $platform instead."
}

$package = Get-PackageSpec $platform

Write-Host "==> Packaging Kanji Dojo CC for $platform (:desktopApp:$($package.Task))" -ForegroundColor Cyan

# --- runtime files (informational: M3 packages them into the installer) -----------------------
$library = Get-RuntimeLibrary $platform
$runtimeMessage = "VOICEVOX runtime for $platform is missing ($library). " +
    "Run tools/voicevox/fetch-runtime.ps1 to assemble it; a packaged app without it falls back to " +
    "the system Japanese voice."
if (Test-Path $library) {
    Write-Host "    VOICEVOX runtime for $platform found"
} elseif ($RequireRuntime) {
    throw $runtimeMessage
} else {
    Write-Warning $runtimeMessage
}

# --- build ------------------------------------------------------------------------------------
$gradle = if ($hostTarget -eq 'windows') { Join-Path $repoRoot 'gradlew.bat' } else { Join-Path $repoRoot 'gradlew' }
$arguments = @(":desktopApp:$($package.Task)", '--console=plain')
if ($DryRun) { $arguments += '--dry-run' }

Push-Location $repoRoot
try {
    & $gradle @arguments
    if ($LASTEXITCODE -ne 0) { throw "gradle $($package.Task) failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}

# --- result -----------------------------------------------------------------------------------
$outputDir = Join-Path $repoRoot "desktopApp/build/compose/binaries/main/$($package.Output)"
Write-Host ""
if ($DryRun) {
    Write-Host "Dry run only - nothing was built." -ForegroundColor Yellow
} elseif (Test-Path $outputDir) {
    Write-Host "Package(s) in ${outputDir}:" -ForegroundColor Green
    Get-ChildItem -Path $outputDir -File |
        ForEach-Object { Write-Host ("    {0}  ({1} MB)" -f $_.Name, [math]::Round($_.Length / 1MB, 1)) }
} else {
    Write-Warning "Expected output directory $outputDir was not created."
}
