<#
.SYNOPSIS
    Assembles the VOICEVOX CORE runtime that the Kanji Dojo CC builds bundle (desktop + Android).

.DESCRIPTION
    Fills tools/voicevox/runtime (git-ignored, roughly 300 MB) with

        core/onnxruntime/lib/<platform>/...    VOICEVOX's own ONNX Runtime build, one directory per
                                               desktop platform (windows-x64, linux-x64, linux-arm64,
                                               macos-x64, macos-arm64)
        core/dict/open_jtalk_dic_utf_8-1.11/   OpenJTalk dictionary, platform independent (~102 MB)
        models/4.vvm                           voice model "Kurono Takehiro" (~55 MB)
        android/jniLibs/<abi>/...              Android libraries for arm64-v8a and x86_64

    Everything is downloaded from official releases, so a fresh clone plus this script is all the
    build needs - that is exactly what CI does. A local M1 spike directory is used as a shortcut
    only when it happens to exist.

    Sources:
      * dictionary   - VOICEVOX's own `download-<platform>` helper from the voicevox_core release
                       (it asks for the redistribution permission, the answer is fed to it)
      * voice model  - the voicevox_vvm release
      * onnxruntime  - VOICEVOX/onnxruntime-builder releases
      * Android      - `java_packages.zip` (voicevox_core release) for the Java API AAR, plus the
                       android-* onnxruntime builds

    Invoking it again only fills in what is missing - use -Force to overwrite.

.NOTES
    This script never runs any git command. The runtime directory stays out of the repository
    (.gitignore); see TTS-HANDOFF.md for the licensing notes that apply when redistributing it.
#>

[CmdletBinding()]
param(
    [string[]] $Platforms = @(
        'windows-x64', 'linux-x64', 'linux-arm64', 'macos-x64', 'macos-arm64',
        'android-arm64', 'android-x86_64'
    ),
    [string] $OnnxRuntimeVersion = '1.23.2',
    [string] $VoicevoxCoreVersion = '0.17.0',
    # M1 spike environment (optional; override with -SpikeDir when it lives somewhere else).
    [string] $SpikeDir = 'D:\voicevox-spike',
    [switch] $Force
)

$ErrorActionPreference = 'Stop'
# Invoke-WebRequest crawls through ~300 MB with the progress bar enabled.
$ProgressPreference = 'SilentlyContinue'

$repoRoot       = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$runtimeDir     = Join-Path $repoRoot 'tools/voicevox/runtime'
$libRoot        = Join-Path $runtimeDir 'core/onnxruntime/lib'
$dictDir        = Join-Path $runtimeDir 'core/dict/open_jtalk_dic_utf_8-1.11'
$modelPath      = Join-Path $runtimeDir 'models/4.vvm'
$androidJniLibs = Join-Path $runtimeDir 'android/jniLibs'

$voicevoxCoreReleaseUrl = "https://github.com/VOICEVOX/voicevox_core/releases/download/$VoicevoxCoreVersion"
$voiceModelUrl = 'https://github.com/VOICEVOX/voicevox_vvm/releases/download/0.16.4/4.vvm'

# Used to unpack VOICEVOX's archives (extract-library.py / extract-android-libs.py).
$pythonCommand = Get-Command python -ErrorAction SilentlyContinue
if (-not $pythonCommand) { $pythonCommand = Get-Command python3 -ErrorAction SilentlyContinue }
$python = if ($pythonCommand) { $pythonCommand.Source } else { $null }
if (-not $python) {
    throw "Python 3 is required to unpack VOICEVOX's archives (tools/voicevox/*.py)."
}

# VOICEVOX's own download helper, per host platform.
$voicevoxDownloaders = [ordered]@{
    'windows-x64' = 'download-windows-x64.exe'
    'linux-x64'   = 'download-linux-x64'
    'linux-arm64' = 'download-linux-arm64'
    'macos-x64'   = 'download-osx-x64'
    'macos-arm64' = 'download-osx-arm64'
}

# platform -> release asset tag + the library file name VOICEVOX publishes for it (desktop)
$onnxRuntimeDesktopPlatforms = [ordered]@{
    'windows-x64' = @{ Release = 'win-x64';     File = 'voicevox_onnxruntime.dll' }
    'linux-x64'   = @{ Release = 'linux-x64';   File = 'libvoicevox_onnxruntime.so' }
    'linux-arm64' = @{ Release = 'linux-arm64'; File = 'libvoicevox_onnxruntime.so' }
    'macos-x64'   = @{ Release = 'osx-x86_64';  File = 'libvoicevox_onnxruntime.dylib' }
    'macos-arm64' = @{ Release = 'osx-arm64';   File = 'libvoicevox_onnxruntime.dylib' }
}

# platform -> release asset tag + the Android ABI folder it belongs to
$onnxRuntimeAndroidPlatforms = [ordered]@{
    'android-arm64'  = @{ Release = 'android-arm64'; Abi = 'arm64-v8a' }
    'android-x86_64' = @{ Release = 'android-x64';   Abi = 'x86_64' }
}

function Write-Step {
    param([string] $Message)

    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Get-TempDir {
    param([string] $Name)

    $path = Join-Path ([System.IO.Path]::GetTempPath()) "kanji-dojo-voicevox/$Name"
    if (Test-Path $path) { Remove-Item -Recurse -Force $path }
    New-Item -ItemType Directory -Force -Path $path | Out-Null
    $path
}

function Get-HostPlatformId {
    $information = [System.Runtime.InteropServices.RuntimeInformation]
    $arch = $information::OSArchitecture.ToString().ToLowerInvariant()

    if ($information::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::Windows)) { return 'windows-x64' }
    if ($information::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::OSX)) {
        if ($arch -eq 'arm64') { return 'macos-arm64' }
        return 'macos-x64'
    }
    if ($arch -eq 'arm64') { return 'linux-arm64' }
    return 'linux-x64'
}

function Get-RemoteFile {
    param(
        [string] $Url,
        [string] $Destination
    )

    $parent = Split-Path -Parent $Destination
    if ($parent) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }

    Write-Host "    fetch   $(Split-Path -Leaf $Url)"
    Invoke-WebRequest -Uri $Url -OutFile $Destination -UseBasicParsing
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

function Invoke-VoicevoxDownloader {
    param(
        [string]   $DownloaderPath,
        [string[]] $DownloaderArgs,
        [string]   $WorkingDirectory
    )

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $DownloaderPath
    $startInfo.Arguments = ($DownloaderArgs -join ' ')
    $startInfo.WorkingDirectory = $WorkingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = [System.Diagnostics.Process]::Start($startInfo)
    # The helper asks whether the files may be redistributed; answer before it ever prompts, so this
    # works without a terminal (PowerShell piping would add a BOM and corrupt the answer).
    $process.StandardInput.WriteLine('y')
    $process.StandardInput.WriteLine('y')
    $process.StandardInput.Close()

    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    if ($process.ExitCode -ne 0) {
        Write-Host $stdout
        Write-Host $stderr
        throw "VOICEVOX's download helper failed (exit code $($process.ExitCode))."
    }
}

function New-Dictionary {
    if ((Test-Path $dictDir) -and -not $Force) {
        Write-Host "    skip    open_jtalk_dic_utf_8-1.11 (already present)"
        return
    }

    $spikeDictionary = Join-Path $SpikeDir 'core/dict/open_jtalk_dic_utf_8-1.11'
    if (Test-Path $spikeDictionary) {
        Copy-RuntimeFile -Source $spikeDictionary -Destination $dictDir -What 'open_jtalk_dic_utf_8-1.11'
        return
    }

    $hostPlatform = Get-HostPlatformId
    $downloaderName = $voicevoxDownloaders[$hostPlatform]
    if (-not $downloaderName) {
        throw "No VOICEVOX download helper is known for '$hostPlatform'; copy the dictionary into $dictDir by hand."
    }

    $workDir = Get-TempDir 'dict'
    $downloaderPath = Join-Path $workDir $downloaderName
    Get-RemoteFile -Url "$voicevoxCoreReleaseUrl/$downloaderName" -Destination $downloaderPath

    if ($hostPlatform -notlike 'windows-*') {
        & chmod +x $downloaderPath
    }

    Write-Host "    run     $downloaderName --only dict"
    Invoke-VoicevoxDownloader -DownloaderPath $downloaderPath -DownloaderArgs @('--only', 'dict') -WorkingDirectory $workDir

    $downloaded = Join-Path $workDir 'voicevox_core/dict/open_jtalk_dic_utf_8-1.11'
    if (-not (Test-Path $downloaded)) {
        throw "The download helper did not produce $downloaded"
    }

    Copy-RuntimeFile -Source $downloaded -Destination $dictDir -What 'open_jtalk_dic_utf_8-1.11'
    Remove-Item -Recurse -Force $workDir
}

function New-VoiceModel {
    if ((Test-Path $modelPath) -and -not $Force) {
        Write-Host "    skip    models/4.vvm (already present)"
        return
    }

    $spikeModel = Join-Path $SpikeDir 'models/4.vvm'
    if (Test-Path $spikeModel) {
        Copy-RuntimeFile -Source $spikeModel -Destination $modelPath -What 'models/4.vvm'
        return
    }

    $workDir = Get-TempDir 'model'
    $downloaded = Join-Path $workDir '4.vvm'
    Get-RemoteFile -Url $voiceModelUrl -Destination $downloaded
    Copy-RuntimeFile -Source $downloaded -Destination $modelPath -What 'models/4.vvm'
    Remove-Item -Recurse -Force $workDir
}

function Get-OnnxRuntimeDesktopLibrary {
    param(
        [string]    $Platform,
        [hashtable] $Info
    )

    $destination = Join-Path (Join-Path $libRoot $Platform) $Info.File
    if ((Test-Path $destination) -and -not $Force) {
        Write-Host "    skip    $Platform/$($Info.File) (already present)"
        return
    }

    # The Windows build usually sits in the spike environment already; reuse it instead of downloading.
    if ($Platform -eq 'windows-x64') {
        $spikeLibrary = Join-Path $SpikeDir "core/onnxruntime/lib/$($Info.File)"
        if (Test-Path $spikeLibrary) {
            Copy-RuntimeFile -Source $spikeLibrary -Destination $destination -What "$Platform/$($Info.File)"
            return
        }
    }

    $asset = "voicevox_onnxruntime-$($Info.Release)-$OnnxRuntimeVersion.tgz"
    $url = "https://github.com/VOICEVOX/onnxruntime-builder/releases/download/voicevox_onnxruntime-$OnnxRuntimeVersion/$asset"

    $workDir = Get-TempDir "onnxruntime-$Platform"
    $archive = Join-Path $workDir $asset
    Get-RemoteFile -Url $url -Destination $archive

    # `tar -xzf` cannot create the symlinks that sit next to VOICEVOX's versioned libraries on
    # Windows (that needs admin rights or developer mode), so the real file is pulled out with a
    # helper that writes it under the name the app expects.
    $prefix = $Info.File.Split('.')[0]
    & $python (Join-Path $PSScriptRoot 'extract-library.py') $archive $prefix $destination
    if ($LASTEXITCODE -ne 0) { throw "$asset could not be unpacked into $destination" }

    Remove-Item -Recurse -Force $workDir
}

function Get-OnnxRuntimeAndroidLibrary {
    param(
        [string]    $Platform,
        [hashtable] $Info
    )

    $destination = Join-Path (Join-Path $androidJniLibs $Info.Abi) 'libvoicevox_onnxruntime.so'
    if ((Test-Path $destination) -and -not $Force) {
        Write-Host "    skip    $($Info.Abi)/libvoicevox_onnxruntime.so (already present)"
        return
    }

    $asset = "voicevox_onnxruntime-$($Info.Release)-$OnnxRuntimeVersion.tgz"
    $url = "https://github.com/VOICEVOX/onnxruntime-builder/releases/download/voicevox_onnxruntime-$OnnxRuntimeVersion/$asset"

    $workDir = Get-TempDir "onnxruntime-$Platform"
    $archive = Join-Path $workDir $asset
    Get-RemoteFile -Url $url -Destination $archive

    & $python (Join-Path $PSScriptRoot 'extract-library.py') $archive 'libvoicevox_onnxruntime' $destination
    if ($LASTEXITCODE -ne 0) { throw "$asset could not be unpacked into $destination" }

    Remove-Item -Recurse -Force $workDir
}

function Get-AndroidCoreLibraries {
    $targets = @('arm64-v8a', 'x86_64') | ForEach-Object {
        Join-Path (Join-Path $androidJniLibs $_) 'libvoicevox_core_java_api.so'
    }

    if ((-not $Force) -and (($targets | Where-Object { -not (Test-Path $_) }).Count -eq 0)) {
        Write-Host "    skip    android/jniLibs/*/libvoicevox_core_java_api.so (already present)"
        return
    }

    $workDir = Get-TempDir 'android-packages'
    $archive = Join-Path $workDir 'java_packages.zip'
    Get-RemoteFile -Url "$voicevoxCoreReleaseUrl/java_packages.zip" -Destination $archive

    & $python (Join-Path $PSScriptRoot 'extract-android-libs.py') $archive $androidJniLibs
    if ($LASTEXITCODE -ne 0) { throw "java_packages.zip could not be unpacked into $androidJniLibs" }

    Remove-Item -Recurse -Force $workDir
}

Write-Step "Voicevox runtime -> $runtimeDir"

if (-not (Test-Path $SpikeDir)) {
    Write-Host "    (no spike directory at $SpikeDir - the dictionary and the voice model are downloaded)"
}

Write-Step "OpenJTalk dictionary (~102 MB, shared by every platform)"
New-Dictionary

Write-Step "Voice model (~55 MB, shared by every platform)"
New-VoiceModel

Write-Step "Desktop ONNX Runtime (voicevox_onnxruntime $OnnxRuntimeVersion)"
foreach ($platform in $Platforms) {
    if (-not $onnxRuntimeDesktopPlatforms.Contains($platform)) {
        if (-not $onnxRuntimeAndroidPlatforms.Contains($platform)) {
            Write-Warning "    unknown platform '$platform' - known: $(($onnxRuntimeDesktopPlatforms.Keys + $onnxRuntimeAndroidPlatforms.Keys) -join ', ')"
        }
        continue
    }
    Write-Host "    platform $platform"
    Get-OnnxRuntimeDesktopLibrary -Platform $platform -Info $onnxRuntimeDesktopPlatforms[$platform]
}

$androidPlatforms = $Platforms | Where-Object { $onnxRuntimeAndroidPlatforms.Contains($_) }
if ($androidPlatforms.Count -gt 0) {
    Write-Step "Android libraries (arm64-v8a, x86_64)"
    Get-AndroidCoreLibraries
    foreach ($platform in $androidPlatforms) {
        Write-Host "    platform $platform"
        Get-OnnxRuntimeAndroidLibrary -Platform $platform -Info $onnxRuntimeAndroidPlatforms[$platform]
    }
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
Write-Host "Done. The Compose desktop build picks this directory up automatically, and the Android" -ForegroundColor Green
Write-Host "build takes its jniLibs from runtime/android." -ForegroundColor Green
