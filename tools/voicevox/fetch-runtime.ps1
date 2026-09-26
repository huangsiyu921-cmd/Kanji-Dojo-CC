<#
.SYNOPSIS
    Assembles the VOICEVOX CORE runtime files used by the desktop build of Kanji Dojo CC.

.DESCRIPTION
    Fills tools/voicevox/runtime (git-ignored, roughly 180 MB) with

        core/onnxruntime/lib/<platform>/...  VOICEVOX's own ONNX Runtime build, one directory per
                                             platform (windows-x64, linux-x64, linux-arm64,
                                             macos-x64, macos-arm64)
        core/dict/open_jtalk_dic_utf_8-1.11/ OpenJTalk dictionary, platform independent (~102 MB)
        models/4.vvm                         voice model "Kurono Takehiro", (~55 MB)

    The dictionary and the voice model are copied from the M1 spike environment when it is present
    (they are not published as plain downloads); ONNX Runtime is fetched from the
    VOICEVOX/onnxruntime-builder releases.

    Invoking it again only fills in what is missing - use -Force to overwrite.

.NOTES
    This script never runs any git command. The runtime directory stays out of the repository
    (.gitignore); see TTS-HANDOFF.md for the licensing notes that apply when redistributing it.
#>

[CmdletBinding()]
param(
    [string[]] $Platforms = @('windows-x64', 'linux-x64', 'linux-arm64', 'macos-x64', 'macos-arm64'),
    [string]   $OnnxRuntimeVersion = '1.23.2',
    # M1 spike environment; override with -SpikeDir when it lives somewhere else.
    [string]   $SpikeDir = 'D:\voicevox-spike',
    [switch]   $Force
)

$ErrorActionPreference = 'Stop'

$repoRoot   = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$runtimeDir = Join-Path $repoRoot 'tools/voicevox/runtime'
$libRoot    = Join-Path $runtimeDir 'core/onnxruntime/lib'

# Used to unpack VOICEVOX's ONNX Runtime archives (see extract-library.py).
$pythonCommand = Get-Command python -ErrorAction SilentlyContinue
$python = if ($pythonCommand) { $pythonCommand.Source } else { $null }

# platform -> release asset tag + the library file name VOICEVOX publishes for it
$onnxRuntimePlatforms = [ordered]@{
    'windows-x64' = @{ Release = 'win-x64';     File = 'voicevox_onnxruntime.dll' }
    'linux-x64'   = @{ Release = 'linux-x64';   File = 'libvoicevox_onnxruntime.so' }
    'linux-arm64' = @{ Release = 'linux-arm64'; File = 'libvoicevox_onnxruntime.so' }
    'macos-x64'   = @{ Release = 'osx-x86_64';  File = 'libvoicevox_onnxruntime.dylib' }
    'macos-arm64' = @{ Release = 'osx-arm64';   File = 'libvoicevox_onnxruntime.dylib' }
}

function Write-Step {
    param([string] $Message)
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Copy-RuntimeFile {
    param(
        [string] $Source,
        [string] $Destination,
        [string] $What
    )

    if ((Test-Path $Destination) -and -not $Force) {
        Write-Host "    skip    $What (already present)"
        return
    }
    if (-not (Test-Path $Source)) {
        Write-Warning "    missing $What - expected at $Source"
        return
    }

    $parent = Split-Path -Parent $Destination
    if ($parent) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
    Copy-Item -Path $Source -Destination $Destination -Recurse -Force
    Write-Host "    copy    $What"
}

function Get-OnnxRuntimeLibrary {
    param(
        [string]  $Platform,
        [hashtable] $Info
    )

    $destination = Join-Path (Join-Path $libRoot $Platform) $Info.File
    if ((Test-Path $destination) -and -not $Force) {
        Write-Host "    skip    $Platform/$($Info.File) (already present)"
        return
    }

    # The Windows build already sits in the spike environment; reuse it instead of downloading.
    if ($Platform -eq 'windows-x64') {
        $spikeLibrary = Join-Path $SpikeDir "core/onnxruntime/lib/$($Info.File)"
        if (Test-Path $spikeLibrary) {
            Copy-RuntimeFile -Source $spikeLibrary -Destination $destination -What "$Platform/$($Info.File)"
            return
        }
    }

    $asset  = "voicevox_onnxruntime-$($Info.Release)-$OnnxRuntimeVersion.tgz"
    $url    = "https://github.com/VOICEVOX/onnxruntime-builder/releases/download/voicevox_onnxruntime-$OnnxRuntimeVersion/$asset"
    $temp   = Join-Path ([System.IO.Path]::GetTempPath()) "kanji-dojo-voicevox/$Platform"
    $archive = Join-Path $temp $asset

    if (Test-Path $temp) { Remove-Item -Recurse -Force $temp }
    New-Item -ItemType Directory -Force -Path $temp | Out-Null

    Write-Host "    fetch   $asset"
    if (-not $python) {
        throw "Python 3 is required to unpack VOICEVOX's ONNX Runtime archives (see extract-library.py)."
    }
    Invoke-WebRequest -Uri $url -OutFile $archive -UseBasicParsing

    # `tar -xzf` cannot create the symlinks that sit next to VOICEVOX's versioned libraries on
    # Windows (that needs admin rights or developer mode), so the real file is pulled out with a
    # helper that writes it under the name the app expects.
    $prefix = $Info.File.Split('.')[0]
    & $python (Join-Path $PSScriptRoot 'extract-library.py') $archive $prefix $destination
    if ($LASTEXITCODE -ne 0) { throw "$asset could not be unpacked into $destination" }

    Remove-Item -Recurse -Force $temp
}

Write-Step "Voicevox runtime -> $runtimeDir"

if (-not (Test-Path $SpikeDir)) {
    Write-Warning "spike directory $SpikeDir not found: the dictionary and the voice model cannot be copied from it."
    Write-Warning "Get them from the M1 spike setup (pyopenjtalk's dictionary, voicevox_vvm release 4.vvm) - see TTS-HANDOFF.md."
}

Write-Step "OpenJTalk dictionary (~102 MB, shared by every platform)"
Copy-RuntimeFile `
    -Source (Join-Path $SpikeDir 'core/dict/open_jtalk_dic_utf_8-1.11') `
    -Destination (Join-Path $runtimeDir 'core/dict/open_jtalk_dic_utf_8-1.11') `
    -What 'open_jtalk_dic_utf_8-1.11'

Write-Step "Voice model (~55 MB, shared by every platform)"
Copy-RuntimeFile `
    -Source (Join-Path $SpikeDir 'models/4.vvm') `
    -Destination (Join-Path $runtimeDir 'models/4.vvm') `
    -What 'models/4.vvm'

Write-Step "ONNX Runtime (voicevox_onnxruntime $OnnxRuntimeVersion)"
foreach ($platform in $Platforms) {
    if (-not $onnxRuntimePlatforms.Contains($platform)) {
        Write-Warning "    unknown platform '$platform' - known: $($onnxRuntimePlatforms.Keys -join ', ')"
        continue
    }
    Write-Host "    platform $platform"
    Get-OnnxRuntimeLibrary -Platform $platform -Info $onnxRuntimePlatforms[$platform]
}

Write-Step "Result"
Get-ChildItem -Path $runtimeDir -Recurse -File |
    Group-Object { $_.DirectoryName.Replace($runtimeDir, '').TrimStart('\').Replace('\', '/') } |
    Sort-Object Name |
    ForEach-Object {
        $sizeMb = [math]::Round((($_.Group | Measure-Object -Property Length -Sum).Sum / 1MB), 1)
        Write-Host ("    {0,-45} {1,8} MB  ({2} file(s))" -f $_.Name, $sizeMb, $_.Count)
    }

Write-Host ""
Write-Host "Done. The Compose desktop build picks this directory up automatically." -ForegroundColor Green
