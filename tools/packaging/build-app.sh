#!/usr/bin/env bash
#
# One-shot packaging of the Kanji Dojo CC desktop app for the platform this script runs on.
#
#   Linux -> :desktopApp:packageDeb   (desktopApp/build/compose/binaries/main/deb)
#   macOS -> :desktopApp:packageDmg   (desktopApp/build/compose/binaries/main/dmg)
#   Windows -> :desktopApp:packageMsi (use build-app.ps1 / Git Bash)
#
# jpackage cannot cross-compile, so each target has to be built on its own OS; asking for a
# different platform fails with an explanation.
#
# Not covered here (on purpose):
#   * Android - release builds have their own signed flow, `:app:assembleFdroidRelease`.
#   * iOS     - VOICEVOX on iOS needs an xcframework plus a Kotlin/Native cinterop binding, which
#               is macOS-only work and is still open (TTS-HANDOFF.md, M5). Use Xcode and the
#               iosApp project until then.
#
# The VOICEVOX runtime files (~180 MB, tools/voicevox/runtime) are not packaged into the
# installer yet - that is the M3 milestone. Until then a packaged build falls back to the system
# Japanese voice when the runtime directory is missing.
#
# Build only: this script runs no git command and uploads nothing.

set -euo pipefail

target="auto"
dry_run=0
require_runtime=0

usage() {
    sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'
    cat <<'EOF'

Usage: tools/packaging/build-app.sh [options]

  --target <auto|windows|linux|macos>   Platform to package (default: auto = this OS)
  --dry-run                             Print the Gradle task graph instead of building
  --require-runtime                     Fail when the VOICEVOX runtime files are missing
  -h, --help                            Show this help
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --target) target="${2:?--target needs a value}"; shift 2 ;;
        --dry-run) dry_run=1; shift ;;
        --require-runtime) require_runtime=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown option: $1" >&2; usage; exit 2 ;;
    esac
done

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../.." && pwd)"

detect_host() {
    case "$(uname -s)" in
        Darwin) echo macos ;;
        Linux) echo linux ;;
        MINGW*|MSYS*|CYGWIN*) echo windows ;;
        *) echo unknown ;;
    esac
}

host="$(detect_host)"
[[ "$target" == "auto" ]] && target="$host"

if [[ "$target" != "$host" ]]; then
    echo "Cannot build '$target' on '$host': jpackage has no cross-compilation." >&2
    echo "Run this script on $target instead." >&2
    exit 1
fi

case "$target" in
    linux) task=packageDeb; out_dir=deb ;;
    macos) task=packageDmg; out_dir=dmg ;;
    windows) task=packageMsi; out_dir=msi ;;
    *) echo "Unsupported target '$target'" >&2; exit 1 ;;
esac

detect_arch() {
    case "$(uname -m)" in
        x86_64|amd64) echo x64 ;;
        arm64|aarch64) echo arm64 ;;
        i[3-6]86) echo x86 ;;
        *) uname -m ;;
    esac
}

runtime_library() {
    # Runtime directories are named <os>-<arch>, as in VoicevoxConfig.platformId.
    local platform_id="$1-$(detect_arch)" name
    case "$1" in
        windows*) name=voicevox_onnxruntime.dll ;;
        macos*) name=libvoicevox_onnxruntime.dylib ;;
        *) name=libvoicevox_onnxruntime.so ;;
    esac
    echo "$repo_root/tools/voicevox/runtime/core/onnxruntime/lib/$platform_id/$name"
}

echo "==> Packaging Kanji Dojo CC for $target (:desktopApp:$task)"

library="$(runtime_library "$target")"
if [[ -f "$library" ]]; then
    echo "    VOICEVOX runtime for $target found"
elif [[ "$require_runtime" == 1 ]]; then
    echo "VOICEVOX runtime for $target is missing ($library)." >&2
    echo "Run tools/voicevox/fetch-runtime.sh first." >&2
    exit 1
else
    echo "WARNING: VOICEVOX runtime for $target is missing ($library)." >&2
    echo "         Run tools/voicevox/fetch-runtime.sh to assemble it; without it the packaged" >&2
    echo "         app falls back to the system Japanese voice." >&2
fi

gradle="$repo_root/gradlew"
[[ -x "$gradle" ]] || chmod +x "$gradle"

args=(":desktopApp:$task" "--console=plain")
[[ "$dry_run" == 1 ]] && args+=("--dry-run")

( cd "$repo_root" && "$gradle" "${args[@]}" )

output_dir="$repo_root/desktopApp/build/compose/binaries/main/$out_dir"
echo
if [[ "$dry_run" == 1 ]]; then
    echo "Dry run only - nothing was built."
elif [[ -d "$output_dir" ]]; then
    echo "Package(s) in $output_dir:"
    ls -lh "$output_dir" | tail -n +2 | awk '{ printf "    %s  (%s)\n", $9, $5 }'
else
    echo "WARNING: expected output directory $output_dir was not created." >&2
fi
