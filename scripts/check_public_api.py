#!/usr/bin/env python3
"""Check the public API of the :qrforge library against a committed snapshot."""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
AAR_PATH = REPOSITORY_ROOT / "qrforge" / "build" / "outputs" / "aar" / "qrforge-release.aar"
SNAPSHOT_PATH = REPOSITORY_ROOT / "qrforge" / "api" / "qrforge.api"
ASSEMBLE_COMMAND = "gradlew :qrforge:assembleRelease"
INTERNAL_PACKAGE_PREFIX = "com.appvoyager.qrforge.internal."
SOURCE_FILE_MARKER = "Compiled from "


def resolve_javap() -> str:
    """Locate javap on PATH, falling back to JAVA_HOME."""
    on_path = shutil.which("javap")
    if on_path is not None:
        return on_path

    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = shutil.which("javap", path=str(Path(java_home) / "bin"))
        if candidate is not None:
            return candidate

    raise FileNotFoundError("javap was not found on PATH or under JAVA_HOME")


def extract_classes(destination: Path) -> Path:
    """Unpack classes.jar out of the release AAR and return its directory."""
    classes_jar = destination / "classes.jar"
    with zipfile.ZipFile(AAR_PATH) as aar:
        with aar.open("classes.jar") as source, classes_jar.open("wb") as target:
            shutil.copyfileobj(source, target)

    classes_root = destination / "classes"
    with zipfile.ZipFile(classes_jar) as jar:
        jar.extractall(classes_root)

    return classes_root


def class_names(classes_root: Path) -> list[str]:
    """List every compiled class as a binary name, excluding the internal package."""
    names = []
    for class_file in classes_root.rglob("*.class"):
        name = class_file.relative_to(classes_root).with_suffix("").as_posix().replace("/", ".")
        if name.startswith(INTERNAL_PACKAGE_PREFIX):
            continue
        names.append(name)

    return sorted(names)


def is_public_declaration(line: str) -> bool:
    return line.startswith("public ")


def declared_type_name(header: str) -> str | None:
    """Read the binary name out of a javap type declaration."""
    tokens = header.split()
    for keyword in ("class", "interface", "enum"):
        if keyword in tokens:
            return tokens[tokens.index(keyword) + 1].split("<", maxsplit=1)[0]

    return None


def is_public_member(line: str, owner: str | None) -> bool:
    """Reject Kotlin's name-mangled members.

    `internal` members and `$default` overloads carry a `$` in their own name. Neither is
    part of the documented contract, and both move on internal refactors that
    docs/api-design.md explicitly allows. A nested type's constructor also carries a `$`,
    but that one comes from the enclosing type, so it is compared against the owner first.
    """
    head = line.split("(", maxsplit=1)[0].split("=", maxsplit=1)[0].strip().rstrip(";").strip()
    tokens = head.split()
    if not tokens:
        return False

    name = tokens[-1]
    if name == owner:
        return True

    return "$" not in name.rsplit(".", maxsplit=1)[-1]


def render_api(javap_output: str) -> str:
    """Reduce javap output to a deterministic, comparable public API listing."""
    blocks: list[str] = []
    header: str | None = None
    owner: str | None = None
    members: list[str] = []

    for raw_line in javap_output.splitlines():
        line = raw_line.strip()
        if not line or line.startswith(SOURCE_FILE_MARKER):
            continue

        if line.endswith("{"):
            header = line
            owner = declared_type_name(line)
            members = []
            continue

        if line == "}":
            if header is not None and is_public_declaration(header):
                blocks.append("\n".join([header, *sorted(members), "}"]))
            header = None
            continue

        if header is not None and is_public_member(line, owner):
            members.append(f"  {line}")

    return "\n\n".join(sorted(blocks)) + "\n"


def current_api() -> str:
    if not AAR_PATH.is_file():
        raise FileNotFoundError(
            f"{AAR_PATH.relative_to(REPOSITORY_ROOT)} was not found. Run `{ASSEMBLE_COMMAND}` first."
        )

    javap = resolve_javap()
    with tempfile.TemporaryDirectory() as workspace:
        classes_root = extract_classes(Path(workspace))
        names = class_names(classes_root)
        if not names:
            raise RuntimeError("classes.jar contained no class outside the internal package")

        completed = subprocess.run(
            [javap, "-public", "-constants", "-classpath", str(classes_root), *names],
            capture_output=True,
            check=True,
            text=True,
        )

    return render_api(completed.stdout)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--update",
        action="store_true",
        help="overwrite the committed snapshot with the current public API",
    )
    arguments = parser.parse_args()

    try:
        api = current_api()
    except (FileNotFoundError, RuntimeError, subprocess.CalledProcessError) as error:
        print(f"Public API check failed: {error}")
        return 1

    relative_snapshot = SNAPSHOT_PATH.relative_to(REPOSITORY_ROOT)

    if arguments.update:
        SNAPSHOT_PATH.parent.mkdir(parents=True, exist_ok=True)
        SNAPSHOT_PATH.write_text(api, encoding="utf-8", newline="\n")
        print(f"Public API snapshot written: {relative_snapshot}")
        return 0

    if not SNAPSHOT_PATH.is_file():
        print(
            f"Public API check failed: {relative_snapshot} was not found. "
            "Run `python scripts/check_public_api.py --update` to create it."
        )
        return 1

    expected = SNAPSHOT_PATH.read_text(encoding="utf-8")
    if expected != api:
        print(f"Public API check failed: the public API differs from {relative_snapshot}.")
        print("Review the change against docs/api-design.md, then run:")
        print("  python scripts/check_public_api.py --update")
        print("")
        expected_lines = set(expected.splitlines())
        actual_lines = set(api.splitlines())
        for line in sorted(expected_lines - actual_lines):
            print(f"- {line}")
        for line in sorted(actual_lines - expected_lines):
            print(f"+ {line}")
        return 1

    class_count = api.count("\n\n") + 1
    print(f"Public API check passed: {class_count} public type(s) match {relative_snapshot}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
