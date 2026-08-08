#!/usr/bin/env python3
"""Check that 64-bit native libraries load on devices with a 16 KB page size."""

from __future__ import annotations

import struct
import sys
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_JNI_LIBS = REPOSITORY_ROOT / "qr-forge" / "src" / "main" / "jniLibs"
LIBRARY_NAME = "libqrforge.so"
REBUILD_COMMAND = (
    "cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o qr-forge/src/main/jniLibs "
    "build --release --manifest-path rust/qr-forge-jni/Cargo.toml"
)

REQUIRED_ALIGNMENT = 16 * 1024

ELF_MAGIC = b"\x7fELF"
ELFCLASS64 = 2
PT_LOAD = 1


def load_alignments(data: bytes) -> list[int] | None:
    """The p_align of every PT_LOAD segment, or None when the object is 32-bit.

    Android requires the 16 KB page size only for 64-bit ABIs, so a 32-bit object
    carrying 4 KB segments is not a failure.
    """
    if data[:4] != ELF_MAGIC:
        raise ValueError("not an ELF object")

    if data[4] != ELFCLASS64:
        return None

    program_header_offset, = struct.unpack_from("<Q", data, 0x20)
    entry_size, entry_count = struct.unpack_from("<HH", data, 0x36)

    alignments = []
    for index in range(entry_count):
        entry = program_header_offset + index * entry_size
        segment_type, = struct.unpack_from("<I", data, entry)
        if segment_type == PT_LOAD:
            alignments.append(struct.unpack_from("<Q", data, entry + 0x30)[0])

    return alignments


def alignment_errors(root: Path) -> list[str]:
    errors = []
    checked = 0

    for library in sorted(root.glob(f"*/{LIBRARY_NAME}")):
        abi = library.parent.name
        try:
            alignments = load_alignments(library.read_bytes())
        except (ValueError, IndexError, struct.error) as error:
            errors.append(f"{abi}: {LIBRARY_NAME} could not be parsed as ELF ({error})")
            continue

        if alignments is None:
            continue

        if not alignments:
            errors.append(f"{abi}: {LIBRARY_NAME} has no PT_LOAD segment")
            continue

        checked += 1
        misaligned = [value for value in alignments if value < REQUIRED_ALIGNMENT]
        if misaligned:
            reported = ", ".join(hex(value) for value in sorted(set(misaligned)))
            errors.append(
                f"{abi}: {LIBRARY_NAME} has PT_LOAD alignment {reported}, "
                f"expected at least {hex(REQUIRED_ALIGNMENT)}"
            )

    if not checked and not errors:
        errors.append(f"{root} contains no 64-bit {LIBRARY_NAME}")

    return errors


def main() -> int:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_JNI_LIBS
    if not root.is_dir():
        print(f"{root} was not found. Run: {REBUILD_COMMAND}", file=sys.stderr)
        return 1

    failures = alignment_errors(root)
    if failures:
        print("Native alignment check failed:", file=sys.stderr)
        for failure in failures:
            print(f"  {failure}", file=sys.stderr)
        print(
            "  64-bit ABIs must be linked with -Wl,-z,max-page-size=16384 so the library "
            "loads on 16 KB page size devices.",
            file=sys.stderr,
        )
        return 1

    print(f"Native alignment check passed: 64-bit {LIBRARY_NAME} aligned to 16 KB pages.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
