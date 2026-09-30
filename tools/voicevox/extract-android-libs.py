"""Extract the Android native libraries (and the Java API classes) from VOICEVOX's java_packages.zip.

VOICEVOX publishes `java_packages.zip` on every voicevox_core release; it contains the JVM jar
(checked into `core/libs`) and the AAR with the Android libraries. The AAR cannot be consumed
directly by an Android *library* module, so its contents are unpacked instead:

    jni/<abi>/libvoicevox_core_java_api.so   -> <jniLibs-dir>/<abi>/
    jni/<abi>/libc++_shared.so               -> <jniLibs-dir>/<abi>/
    classes.jar                              -> <classes-jar-destination> (optional)

Usage:
    python extract-android-libs.py <java_packages.zip> <jniLibs-dir> [<classes-jar-destination>]

The ONNX Runtime library for Android is a separate download (see fetch-runtime.ps1), because it
comes from VOICEVOX's onnxruntime-builder releases rather than from the AAR.
"""

import io
import os
import sys
import zipfile


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__.strip(), file=sys.stderr)
        return 2

    packages_zip = sys.argv[1]
    jni_libs_dir = sys.argv[2]
    classes_jar = sys.argv[3] if len(sys.argv) > 3 else None

    with zipfile.ZipFile(packages_zip) as outer:
        aar_name = next((name for name in outer.namelist() if name.endswith(".aar")), None)
        if aar_name is None:
            print(f"no .aar inside {packages_zip}", file=sys.stderr)
            return 1
        aar_bytes = outer.read(aar_name)

    written = 0
    with zipfile.ZipFile(io.BytesIO(aar_bytes)) as aar:
        for name in aar.namelist():
            parts = name.split("/")
            if len(parts) != 3 or parts[0] != "jni" or not parts[2].endswith(".so"):
                continue

            abi, filename = parts[1], parts[2]
            target_dir = os.path.join(jni_libs_dir, abi)
            os.makedirs(target_dir, exist_ok=True)
            with aar.open(name) as source, open(os.path.join(target_dir, filename), "wb") as target:
                target.write(source.read())
            print(f"    extract {abi}/{filename}")
            written += 1

        if classes_jar:
            os.makedirs(os.path.dirname(os.path.abspath(classes_jar)), exist_ok=True)
            with open(classes_jar, "wb") as target:
                target.write(aar.read("classes.jar"))
            print(f"    extract classes.jar -> {os.path.basename(classes_jar)}")

    if written == 0:
        print(f"no jni libraries found in {aar_name}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
