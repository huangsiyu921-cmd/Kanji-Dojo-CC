"""Extract one regular file out of a .tar.gz, following VOICEVOX's versioned-library layout.

VOICEVOX ships its native libraries as a real file plus symlinks, e.g.

    lib/libvoicevox_onnxruntime.so       -> libvoicevox_onnxruntime.so.1
    lib/libvoicevox_onnxruntime.so.1     -> libvoicevox_onnxruntime.so.1.23.2
    lib/libvoicevox_onnxruntime.so.1.23.2

`tar -xzf` (bsdtar on Windows) cannot create those symlinks without developer mode / admin
rights, so `fetch-runtime.ps1` uses this helper instead: it picks the actual file (`isfile()`
skips symlink members) and writes it under the name the app expects.

Usage:
    python extract-library.py <archive.tgz> <name-prefix> <destination>

`name-prefix` is the library name without its version/extension dots, e.g.
`libvoicevox_onnxruntime` or `voicevox_onnxruntime`.
"""

import os
import shutil
import sys
import tarfile


def main() -> int:
    if len(sys.argv) != 4:
        print(__doc__.strip(), file=sys.stderr)
        return 2

    archive, prefix, destination = sys.argv[1:4]

    with tarfile.open(archive, "r:gz") as tar:
        candidates = [
            member
            for member in tar.getmembers()
            # `isfile()` is False for symlinks, which is exactly what we want to skip.
            if member.isfile() and os.path.basename(member.name).startswith(prefix)
        ]
        if not candidates:
            print(f"no regular file matching '{prefix}*' inside {archive}", file=sys.stderr)
            return 1

        # Several candidates can match (e.g. a `.lib` import library next to the real `.dll`);
        # the one we are after is by far the largest.
        member = max(candidates, key=lambda candidate: candidate.size)

        os.makedirs(os.path.dirname(os.path.abspath(destination)), exist_ok=True)
        source = tar.extractfile(member)
        if source is None:
            print(f"cannot read {member.name} from {archive}", file=sys.stderr)
            return 1
        with source, open(destination, "wb") as target:
            shutil.copyfileobj(source, target)

    print(f"    extract {os.path.basename(member.name)} -> {os.path.basename(destination)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
