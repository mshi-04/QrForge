#!/usr/bin/env python3
"""Check that the JNI contract survives R8 in a minified consumer of the :qr-forge library."""

from __future__ import annotations

import struct
import sys
import zipfile
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
APK_PATH = (
    REPOSITORY_ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-release-unsigned.apk"
)
AAR_PATH = (
    REPOSITORY_ROOT / "qr-forge" / "build" / "outputs" / "aar" / "qr-forge-release.aar"
)
CONSUMER_RULES_PATH = REPOSITORY_ROOT / "qr-forge" / "consumer-rules.pro"
ASSEMBLE_COMMAND = (
    r".\gradlew.bat :app:assembleRelease :qr-forge:assembleRelease"
    if sys.platform == "win32"
    else "./gradlew :app:assembleRelease :qr-forge:assembleRelease"
)

LIBRARY_PREFIX = "Lcom/appvoyager/qrforge/"
SAMPLE_PREFIX = "Lcom/appvoyager/qrforge/sample/"
NATIVE_OWNER = "Lcom/appvoyager/qrforge/internal/NativeQrGenerator;"
GENERATION_FAILED = "Lcom/appvoyager/qrforge/internal/NativeQrGenerator$GenerationFailed;"

# Rust resolves these three by name: FindClass on the owner and on the exception, GetStaticMethodID
# on the native entry point, and NewObject on the exception's String constructor.
REQUIRED_METHODS = (
    (NATIVE_OWNER, "nativeGenerateQrPng", "(Ljava/lang/String;II)[B"),
    (GENERATION_FAILED, "<init>", "(Ljava/lang/String;)V"),
)

# R8 must leave nothing else of the library behind. Extra entries mean the build was never
# obfuscated, or the consumer rules keep more than the JNI contract needs.
EXPECTED_LIBRARY_TYPES = frozenset({NATIVE_OWNER, GENERATION_FAILED})


def uleb128(data: bytes, offset: int) -> tuple[int, int]:
    value = 0
    for shift in range(0, 35, 7):
        byte = data[offset]
        offset += 1
        value |= (byte & 0x7F) << shift
        if byte < 0x80:
            break

    return value, offset


class Dex:
    """The slice of the DEX tables needed to state what a JNI lookup will find.

    Only definitions count. `type_ids` and `method_ids` also hold references, so a class R8
    deleted can still appear there and would make a reference-based check pass while
    `FindClass` fails on the device.
    """

    def __init__(self, data: bytes) -> None:
        self.data = data
        self.strings = self._read_strings()
        self.types = self._read_types()
        self.protos = self._read_protos()
        self.method_ids = self._read_method_ids()

    def _table(self, header_offset: int) -> tuple[int, int]:
        size, offset = struct.unpack_from("<II", self.data, header_offset)
        return size, offset

    def _read_strings(self) -> list[str]:
        size, offset = self._table(0x38)
        strings = []
        for index in range(size):
            (data_off,) = struct.unpack_from("<I", self.data, offset + index * 4)
            _, start = uleb128(self.data, data_off)
            end = self.data.index(b"\x00", start)
            strings.append(self.data[start:end].decode("utf-8", errors="replace"))

        return strings

    def _read_types(self) -> list[str]:
        size, offset = self._table(0x40)
        return [
            self.strings[struct.unpack_from("<I", self.data, offset + index * 4)[0]]
            for index in range(size)
        ]

    def _read_protos(self) -> list[str]:
        size, offset = self._table(0x48)
        protos = []
        for index in range(size):
            _, return_idx, parameters_off = struct.unpack_from(
                "<III", self.data, offset + index * 12
            )
            parameters = []
            if parameters_off:
                (count,) = struct.unpack_from("<I", self.data, parameters_off)
                parameters = [
                    self.types[struct.unpack_from("<H", self.data, parameters_off + 4 + i * 2)[0]]
                    for i in range(count)
                ]
            protos.append(f"({''.join(parameters)}){self.types[return_idx]}")

        return protos

    def _read_method_ids(self) -> list[tuple[str, str, str]]:
        size, offset = self._table(0x58)
        return [
            (self.types[class_idx], self.strings[name_idx], self.protos[proto_idx])
            for class_idx, proto_idx, name_idx in (
                struct.unpack_from("<HHI", self.data, offset + index * 8) for index in range(size)
            )
        ]

    def _encoded_methods(self, offset: int, count: int) -> tuple[list[int], int]:
        """Read one delta-encoded encoded_method list, returning absolute method indexes."""
        indexes = []
        method_idx = 0
        for position in range(count):
            diff, offset = uleb128(self.data, offset)
            _, offset = uleb128(self.data, offset)
            _, offset = uleb128(self.data, offset)
            method_idx = diff if position == 0 else method_idx + diff
            indexes.append(method_idx)

        return indexes, offset

    def definitions(self) -> tuple[set[str], set[tuple[str, str, str]]]:
        """The classes this DEX defines, and the methods defined inside them."""
        size, offset = self._table(0x60)
        types = set()
        methods = set()
        for index in range(size):
            class_idx, class_data_off = struct.unpack_from(
                "<I20xI", self.data, offset + index * 32
            )
            types.add(self.types[class_idx])
            if not class_data_off:
                continue

            cursor = class_data_off
            counts = []
            for _ in range(4):
                count, cursor = uleb128(self.data, cursor)
                counts.append(count)

            for field_count in counts[:2]:
                for _ in range(field_count):
                    _, cursor = uleb128(self.data, cursor)
                    _, cursor = uleb128(self.data, cursor)

            for method_count in counts[2:]:
                indexes, cursor = self._encoded_methods(cursor, method_count)
                methods.update(self.method_ids[method_idx] for method_idx in indexes)

        return types, methods


def load_dexes() -> list[Dex]:
    with zipfile.ZipFile(APK_PATH) as apk:
        names = [name for name in apk.namelist() if name.endswith(".dex")]
        if not names:
            raise FileNotFoundError(f"{APK_PATH.name} contains no DEX")

        return [Dex(apk.read(name)) for name in names]


def apk_errors(dexes: list[Dex]) -> list[str]:
    errors = []

    defined = [dex.definitions() for dex in dexes]
    defined_types = set().union(*(types for types, _ in defined))
    defined_methods = set().union(*(methods for _, methods in defined))

    surviving = {
        descriptor
        for descriptor in defined_types
        if descriptor.startswith(LIBRARY_PREFIX) and not descriptor.startswith(SAMPLE_PREFIX)
    }

    unexpected = surviving - EXPECTED_LIBRARY_TYPES
    if unexpected:
        errors.append(
            "app release was not obfuscated, or the consumer rules keep more than the JNI "
            f"contract: {', '.join(sorted(unexpected))}"
        )

    for descriptor in sorted(EXPECTED_LIBRARY_TYPES - surviving):
        errors.append(f"R8 removed or renamed the class {descriptor}")

    for owner, name, proto in REQUIRED_METHODS:
        if (owner, name, proto) not in defined_methods:
            errors.append(f"R8 removed or renamed {owner} {name}{proto}")

    return errors


def aar_errors() -> list[str]:
    """The project dependency and the published AAR must carry the same consumer rules."""
    with zipfile.ZipFile(AAR_PATH) as aar:
        if "proguard.txt" not in aar.namelist():
            return [f"{AAR_PATH.name} does not package proguard.txt"]

        packaged = aar.read("proguard.txt").decode("utf-8").split()

    if packaged != CONSUMER_RULES_PATH.read_text(encoding="utf-8").split():
        return [f"{AAR_PATH.name} packages proguard.txt that differs from consumer-rules.pro"]

    return []


def main() -> int:
    for path in (APK_PATH, AAR_PATH):
        if not path.is_file():
            relative = path.relative_to(REPOSITORY_ROOT)
            print(f"{relative} was not found. Run: {ASSEMBLE_COMMAND}", file=sys.stderr)
            return 1

    failures = apk_errors(load_dexes()) + aar_errors()
    if failures:
        print("Consumer ProGuard check failed:", file=sys.stderr)
        for failure in failures:
            print(f"  {failure}", file=sys.stderr)
        print(
            "  qr-forge/consumer-rules.pro must keep exactly what the JNI bridge resolves by name.",
            file=sys.stderr,
        )
        return 1

    print(
        f"Consumer ProGuard check passed: {len(REQUIRED_METHODS)} JNI member(s) and "
        f"{len(EXPECTED_LIBRARY_TYPES)} type(s) survived R8."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
