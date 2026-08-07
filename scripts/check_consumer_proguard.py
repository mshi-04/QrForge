#!/usr/bin/env python3
"""Check that the JNI symbols survive R8 in a minified consumer of the :qr-forge library."""

from __future__ import annotations

import sys
import zipfile
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
APK_PATH = (
    REPOSITORY_ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-release-unsigned.apk"
)
ASSEMBLE_COMMAND = (
    r".\gradlew.bat :app:assembleRelease"
    if sys.platform == "win32"
    else "./gradlew :app:assembleRelease"
)

INTERNAL_PREFIX = "Lcom/appvoyager/qrforge/internal/NativeQrGenerator"

JNI_SYMBOLS = (
    f"{INTERNAL_PREFIX};",
    f"{INTERNAL_PREFIX}$GenerationFailed;",
    "nativeGenerateQrPng",
)

# Nothing keeps this sibling, so R8 renames it. Its original name surviving means the APK
# was never obfuscated, and every JNI symbol below would pass without being protected.
OBFUSCATION_WITNESS = f"{INTERNAL_PREFIX}$NativeLibraryUnavailable;"


def dex_bytes() -> bytes:
    """Concatenate every DEX in the release APK so symbol names can be searched."""
    with zipfile.ZipFile(APK_PATH) as apk:
        names = [name for name in apk.namelist() if name.endswith(".dex")]
        if not names:
            raise FileNotFoundError(f"{APK_PATH.name} contains no DEX")

        return b"".join(apk.read(name) for name in names)


def errors(dex: bytes) -> list[str]:
    if OBFUSCATION_WITNESS.encode("ascii") in dex:
        return ["app release was not obfuscated; the consumer rules were not exercised"]

    return [
        f"R8 removed or renamed {symbol}"
        for symbol in JNI_SYMBOLS
        if symbol.encode("ascii") not in dex
    ]


def main() -> int:
    if not APK_PATH.is_file():
        relative = APK_PATH.relative_to(REPOSITORY_ROOT)
        print(f"{relative} was not found. Run: {ASSEMBLE_COMMAND}", file=sys.stderr)
        return 1

    failures = errors(dex_bytes())
    if failures:
        print("Consumer ProGuard check failed:", file=sys.stderr)
        for failure in failures:
            print(f"  {failure}", file=sys.stderr)
        print(
            "  qr-forge/consumer-rules.pro must keep every symbol the JNI bridge resolves by name.",
            file=sys.stderr,
        )
        return 1

    print(f"Consumer ProGuard check passed: {len(JNI_SYMBOLS)} JNI symbol(s) survived R8.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
