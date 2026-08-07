#!/usr/bin/env python3
"""Check repository documentation and Android ABI consistency."""

from __future__ import annotations

import re
from pathlib import Path
from urllib.parse import unquote, urlsplit

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
EXCLUDED_DIRECTORY_NAMES = {
    ".git",
    ".gradle",
    ".idea",
    ".kotlin",
    "build",
    "target",
}
MARKDOWN_LINK_PATTERN = re.compile(r"!?\[[^\]]*\]\((?P<target><[^>]+>|[^)\s]+)")
ABI_FILTER_PATTERN = re.compile(r"abiFilters\s*\+=\s*listOf\((?P<body>.*?)\)", re.DOTALL)
QUOTED_VALUE_PATTERN = re.compile(r'"([^"]+)"')


def is_excluded(path: Path) -> bool:
    return any(part in EXCLUDED_DIRECTORY_NAMES for part in path.parts)


def markdown_files() -> list[Path]:
    return [
        path
        for path in sorted(REPOSITORY_ROOT.rglob("*.md"))
        if not is_excluded(path.relative_to(REPOSITORY_ROOT))
    ]


def markdown_link_errors() -> tuple[list[str], int]:
    errors: list[str] = []
    checked_links = 0

    for markdown_file in markdown_files():
        relative_file = markdown_file.relative_to(REPOSITORY_ROOT)
        inside_fence = False
        for line_number, line in enumerate(
            markdown_file.read_text(encoding="utf-8").splitlines(), start=1
        ):
            if line.lstrip().startswith("```"):
                inside_fence = not inside_fence
                continue
            if inside_fence:
                continue

            for match in MARKDOWN_LINK_PATTERN.finditer(line):
                raw_target = match.group("target").strip("<>")
                if raw_target.startswith("#") or urlsplit(raw_target).scheme:
                    continue

                file_target = unquote(raw_target.split("#", maxsplit=1)[0])
                if not file_target:
                    continue
                checked_links += 1

                resolved_target = (markdown_file.parent / file_target).resolve()
                try:
                    resolved_target.relative_to(REPOSITORY_ROOT)
                except ValueError:
                    errors.append(
                        f"{relative_file}:{line_number}: relative link leaves repository: "
                        f"{raw_target}"
                    )
                    continue

                if not resolved_target.exists():
                    errors.append(
                        f"{relative_file}:{line_number}: missing relative link target: "
                        f"{raw_target}"
                    )

    return errors, checked_links


def abi_consistency_errors() -> tuple[list[str], set[str]]:
    build_file = REPOSITORY_ROOT / "qr-forge" / "build.gradle.kts"
    jni_libs_root = REPOSITORY_ROOT / "qr-forge" / "src" / "main" / "jniLibs"
    errors: list[str] = []

    if not build_file.is_file():
        return ["qr-forge/build.gradle.kts: build file was not found"], set()

    match = ABI_FILTER_PATTERN.search(build_file.read_text(encoding="utf-8"))
    if match is None:
        return ["qr-forge/build.gradle.kts: abiFilters list was not found"], set()

    configured_abis = set(QUOTED_VALUE_PATTERN.findall(match.group("body")))
    if not jni_libs_root.is_dir():
        return ["qr-forge/src/main/jniLibs: directory was not found"], configured_abis

    packaged_abis = {path.name for path in jni_libs_root.iterdir() if path.is_dir()}

    missing_directories = configured_abis - packaged_abis
    extra_directories = packaged_abis - configured_abis
    if missing_directories:
        errors.append(
            "jniLibs is missing configured ABI directories: "
            + ", ".join(sorted(missing_directories))
        )
    if extra_directories:
        errors.append(
            "jniLibs has ABI directories absent from abiFilters: "
            + ", ".join(sorted(extra_directories))
        )

    for abi in sorted(configured_abis & packaged_abis):
        library = jni_libs_root / abi / "libqrforge.so"
        if not library.is_file():
            errors.append(f"Missing native library: {library.relative_to(REPOSITORY_ROOT)}")

    return errors, configured_abis


def main() -> int:
    link_errors, checked_links = markdown_link_errors()
    abi_errors, configured_abis = abi_consistency_errors()
    errors = link_errors + abi_errors

    if errors:
        print("Repository consistency check failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print(
        "Repository consistency check passed: "
        f"{checked_links} relative Markdown link(s), "
        f"{len(configured_abis)} ABI directory/directories."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
