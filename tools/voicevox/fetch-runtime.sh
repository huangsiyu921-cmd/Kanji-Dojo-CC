#!/usr/bin/env bash
#
# Assembles the VOICEVOX CORE runtime files used by the desktop build of Kanji Dojo CC.
#
# Fills tools/voicevox/runtime (git-ignored, roughly 180 MB) with
#
#   core/onnxruntime/lib/<platform>/...   VOICEVOX's own ONNX Runtime build, one directory per
#                                        platform (windows-x64, linux-x64, linux-arm64,
#                                        macos-x64, macos-arm64)
#   core/dict/open_jtalk_dic_utf_8-1.11/ OpenJTalk dictionary, platform independent (~102 MB)
#   models/4.vvm                         voice model "Kurono Takehiro" (~55 MB)
#
# The dictionary and the voice model are copied from the M1 spike environment when it is present
# (they are not published as plain downloads); ONNX Runtime is fetched from the
# VOICEVOX/onnxruntime-builder releases. Invoking the script again only fills in what is missing -
# use --force to overwrite.
#
# This script never runs any git command. See TTS-HANDOFF.md for the licensing notes that apply
# when redistributing the runtime files.

set -euo pipefail

platforms=(windows-x64 linux-x64 linux-arm64 macos-x64 macos-arm64)
onnx_version="${ONNXRUNTIME_VERSION:-1.23.2}"
spike_dir="${VOICEVOX_SPIKE_DIR:-$HOME/voicevox-spike}"
python_bin="${PYTHON:-python3}"
force=0

usage() {
    sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
    cat <<'EOF'

Usage: tools/voicevox/fetch-runtime.sh [options]

  --platform <name>              Add a platform to fetch (repeatable); default: all desktops
  --onnxruntime-version <ver>    VOICEVOX ONNX Runtime version (default: 1.23.2)
  --spike-dir <dir>              Where the M1 spike environment lives (default: ~/voicevox-spike)
  --force                        Re-download / overwrite existing files
  -h, --help                     Show this help
EOF
}

selected=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --platform) selected+=("${2:?--platform needs a value}"); shift 2 ;;
        --onnxruntime-version) onnx_version="${2:?--onnxruntime-version needs a value}"; shift 2 ;;
        --spike-dir) spike_dir="${2:?--spike-dir needs a value}"; shift 2 ;;
        --force) force=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown option: $1" >&2; usage; exit 2 ;;
    esac
done
[[ ${#selected[@]} -gt 0 ]] && platforms=("${selected[@]}")

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"
runtime_dir="$repo_root/tools/voicevox/runtime"
lib_root="$runtime_dir/core/onnxruntime/lib"

asset_tag() {
    case "$1" in
        windows-x64) echo win-x64 ;;
        windows-x86) echo win-x86 ;;
        linux-x64) echo linux-x64 ;;
        linux-arm64) echo linux-arm64 ;;
        macos-x64) echo osx-x86_64 ;;
        macos-arm64) echo osx-arm64 ;;
        *) return 1 ;;
    esac
}

library_name() {
    case "$1" in
        windows*) echo voicevox_onnxruntime.dll ;;
        macos*) echo libvoicevox_onnxruntime.dylib ;;
        *) echo libvoicevox_onnxruntime.so ;;
    esac
}

copy_runtime_file() {
    local source="$1" destination="$2" what="$3"
    if [[ -e "$destination" && "$force" == 0 ]]; then
        echo "    skip    $what (already present)"
        return 0
    fi
    if [[ ! -e "$source" ]]; then
        echo "    WARNING: missing $what - expected at $source" >&2
        return 0
    fi
    mkdir -p "$(dirname "$destination")"
    cp -R "$source" "$destination"
    echo "    copy    $what"
}

fetch_onnx_runtime() {
    local platform="$1" tag name destination
    tag="$(asset_tag "$platform")" || { echo "    WARNING: unknown platform '$platform'" >&2; return 0; }
    name="$(library_name "$platform")"
    destination="$lib_root/$platform/$name"

    if [[ -e "$destination" && "$force" == 0 ]]; then
        echo "    skip    $platform/$name (already present)"
        return 0
    fi

    if [[ "$platform" == windows-x64 && -f "$spike_dir/core/onnxruntime/lib/$name" ]]; then
        copy_runtime_file "$spike_dir/core/onnxruntime/lib/$name" "$destination" "$platform/$name"
        return 0
    fi

    local asset="voicevox_onnxruntime-$tag-$onnx_version.tgz"
    local url="https://github.com/VOICEVOX/onnxruntime-builder/releases/download/voicevox_onnxruntime-$onnx_version/$asset"
    local temp
    temp="$(mktemp -d)"

    if ! command -v "$python_bin" >/dev/null 2>&1; then
        echo "ERROR: $python_bin is required to unpack VOICEVOX's ONNX Runtime archives" >&2
        rmdir "$temp" 2>/dev/null || true
        exit 1
    fi

    echo "    fetch   $asset"
    curl -fL --retry 3 -o "$temp/$asset" "$url"

    # VOICEVOX ships a real file plus symlinks next to it; extracting the real one under the
    # expected name is what extract-library.py does (shared with the PowerShell script).
    local prefix="${name%%.*}"
    local status=0
    "$python_bin" "$script_dir/extract-library.py" "$temp/$asset" "$prefix" "$destination" || status=$?
    rm -rf "$temp"
    return "$status"
}

echo "==> Voicevox runtime -> $runtime_dir"

if [[ ! -d "$spike_dir" ]]; then
    echo "WARNING: spike directory $spike_dir not found: the dictionary and the voice model cannot" >&2
    echo "         be copied from it. Get them from the M1 spike setup (pyopenjtalk's dictionary," >&2
    echo "         voicevox_vvm release 4.vvm) - see TTS-HANDOFF.md." >&2
fi

echo "==> OpenJTalk dictionary (~102 MB, shared by every platform)"
copy_runtime_file "$spike_dir/core/dict/open_jtalk_dic_utf_8-1.11" \
    "$runtime_dir/core/dict/open_jtalk_dic_utf_8-1.11" "open_jtalk_dic_utf_8-1.11"

echo "==> Voice model (~55 MB, shared by every platform)"
copy_runtime_file "$spike_dir/models/4.vvm" "$runtime_dir/models/4.vvm" "models/4.vvm"

echo "==> ONNX Runtime (voicevox_onnxruntime $onnx_version)"
for platform in "${platforms[@]}"; do
    echo "    platform $platform"
    fetch_onnx_runtime "$platform"
done

echo "==> Result"
du -sh "$runtime_dir"/* 2>/dev/null || true

echo
echo "Done. The Compose desktop build picks this directory up automatically."
