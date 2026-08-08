#!/usr/bin/env python3
"""Generate THIRD-PARTY-NOTICES.md for the crates linked into libqrforge.so."""

from __future__ import annotations

import os
import subprocess
import sys
from collections import deque
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
OUTPUT_PATH = REPOSITORY_ROOT / "THIRD-PARTY-NOTICES.md"
JNI_MANIFEST = REPOSITORY_ROOT / "rust" / "qr-forge-jni" / "Cargo.toml"

# The published .so is built for the Android ABIs only, and proc-macro crates run on the host
# without being linked into it. Both filters keep the notices to what is actually distributed.
TARGET = "aarch64-linux-android"
EDGES = "normal,no-proc-macro"

WORKSPACE_CRATES = frozenset({"qr-forge-core", "qr-forge-jni"})

# Every linked crate offers MIT, so the distribution takes MIT uniformly. A dependency that does
# not offer MIT breaks the uniform choice stated in the header, so generation stops instead.
CHOSEN_LICENSE = "MIT"

# cesu8 1.1.0 packages no license file. The text exists only upstream, so its notice points at the
# repository. The version is part of the key because a later release may start packaging one, and
# any other crate missing its license text is an unreviewed change that stops generation.
CRATES_WITHOUT_LICENSE_FILE = frozenset({("cesu8", "1.1.0")})

LICENSE_FILE_NAMES = (
    "LICENSE-MIT",
    "LICENSE-MIT.txt",
    "LICENSE-MIT.md",
    "LICENSE",
    "LICENSE.txt",
    "LICENSE.md",
)

HEADER = f"""# Third-party notices

QrForge の `libqrforge.so` は、次の Rust crate を静的リンクして配布している。いずれも
{CHOSEN_LICENSE} license の条件で再配布しており、各 crate の著作権表示と license 全文を以下に示す。
crate 自身が license 全文を配布物へ同梱していない場合に限り、上流リポジトリへの参照を示す。

この文書は `python scripts/generate_third_party_notices.py` で生成する。依存を追加・更新したら
再生成する。QrForge 自体の license は [LICENSE](LICENSE) を参照。
"""


def cargo_registry_roots() -> list[Path]:
    cargo_home = Path(os.environ.get("CARGO_HOME", Path.home() / ".cargo"))
    source_root = cargo_home / "registry" / "src"
    if not source_root.is_dir():
        return []

    return sorted(path for path in source_root.iterdir() if path.is_dir())


def linked_crates() -> list[tuple[str, str, str]]:
    result = subprocess.run(
        [
            "cargo",
            "tree",
            "--manifest-path",
            str(JNI_MANIFEST),
            "--target",
            TARGET,
            "--edges",
            EDGES,
            "--prefix",
            "none",
            "--no-dedupe",
            "--format",
            "{p}|{l}|{r}",
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    if result.returncode != 0:
        raise RuntimeError(f"cargo tree failed: {result.stderr.strip()}")

    crates = set()
    for line in result.stdout.splitlines():
        line = line.strip()
        if not line or "|" not in line:
            continue

        package, license_expression, repository = line.split("|", 2)
        fields = package.split()
        if len(fields) < 2:
            continue

        name, version = fields[0], fields[1].lstrip("v")
        if name in WORKSPACE_CRATES:
            continue

        crates.add((name, version, license_expression.strip(), repository.strip()))

    return sorted(crates)


def license_text(name: str, version: str, registries: list[Path]) -> str | None:
    for registry in registries:
        crate_root = registry / f"{name}-{version}"
        for file_name in LICENSE_FILE_NAMES:
            candidate = crate_root / file_name
            if candidate.is_file():
                return candidate.read_text(encoding="utf-8", errors="replace").strip()

    return None


def render(
    crates: list[tuple[str, str, str, str]], registries: list[Path]
) -> tuple[str, list[tuple[str, str]]]:
    sections = [
        HEADER,
        "\n## 一覧\n",
        "| Crate | Version | License | Repository |",
        "|-------|---------|---------|------------|",
    ]
    for name, version, expression, repository in crates:
        link = f"<{repository}>" if repository else "—"
        sections.append(f"| `{name}` | {version} | {expression} | {link} |")

    sections.append("\n## License 全文\n")
    missing = []
    for name, version, _, repository in crates:
        sections.append(f"### {name} {version}\n")
        text = license_text(name, version, registries)
        if text is None:
            missing.append((name, version))
            source = f"<{repository}>" if repository else "上流リポジトリ"
            sections.append(
                f"この crate は license 全文を配布物へ同梱していない。著作権表示は {source} を参照。\n"
            )
            continue

        sections.append("```")
        sections.append(text)
        sections.append("```\n")

    return "\n".join(sections) + "\n", missing


def tokenize_spdx(expression: str) -> list[str]:
    # Cargo still carries the pre-SPDX `A/B` form, which means the same as `A OR B`.
    normalized = expression.replace("/", " OR ").replace("(", " ( ").replace(")", " ) ")
    return normalized.split()


def parse_spdx_or(tokens: deque[str], license_id: str) -> bool:
    value = parse_spdx_and(tokens, license_id)
    while tokens and tokens[0] == "OR":
        tokens.popleft()
        right = parse_spdx_and(tokens, license_id)
        value = value or right

    return value


def parse_spdx_and(tokens: deque[str], license_id: str) -> bool:
    value = parse_spdx_atom(tokens, license_id)
    while tokens and tokens[0] == "AND":
        tokens.popleft()
        right = parse_spdx_atom(tokens, license_id)
        value = value and right

    return value


def parse_spdx_atom(tokens: deque[str], license_id: str) -> bool:
    if not tokens:
        raise ValueError("expression ended unexpectedly")

    token = tokens.popleft()
    if token == "(":
        value = parse_spdx_or(tokens, license_id)
        if not tokens or tokens.popleft() != ")":
            raise ValueError("unbalanced parenthesis")

        return value

    if token in {")", "AND", "OR"}:
        raise ValueError(f"misplaced {token}")

    return token == license_id


def satisfied_by(expression: str, license_id: str) -> bool:
    """Whether taking license_id on its own satisfies the expression.

    Substring matching would accept `MIT-0`, a different license, and `Apache-2.0 AND MIT`, which
    carries obligations the uniform choice does not cover. Anything this parser cannot read, such
    as a `WITH` exception, is left unparsed so the caller stops and asks for review.
    """
    tokens = deque(tokenize_spdx(expression))
    if not tokens:
        raise ValueError("expression is empty")

    value = parse_spdx_or(tokens, license_id)
    if tokens:
        raise ValueError(f"unparsed tokens: {' '.join(tokens)}")

    return value


def ensure_chosen_license_applies(crates: list[tuple[str, str, str, str]]) -> None:
    without_choice = []
    for name, version, expression, _ in crates:
        try:
            applies = satisfied_by(expression, CHOSEN_LICENSE)
        except ValueError as error:
            raise RuntimeError(
                f"{name} {version} has a license expression this script cannot read "
                f"('{expression}': {error}). Confirm the terms and extend the parser"
            ) from error

        if not applies:
            without_choice.append(f"{name} {version} ({expression})")

    if without_choice:
        raise RuntimeError(
            f"these crates cannot be redistributed under {CHOSEN_LICENSE} alone, so the uniform "
            f"license choice stated in the header no longer holds: {', '.join(without_choice)}"
        )


def ensure_missing_texts_are_known(missing: list[tuple[str, str]]) -> None:
    unexpected = sorted(set(missing) - CRATES_WITHOUT_LICENSE_FILE)
    if unexpected:
        listed = ", ".join(f"{name} {version}" for name, version in unexpected)
        raise RuntimeError(
            f"no license text was found for {listed}. Run `cargo fetch` so the sources are "
            "unpacked, or add the crate and version to CRATES_WITHOUT_LICENSE_FILE after "
            "confirming that release packages none"
        )


def main() -> int:
    registries = cargo_registry_roots()
    if not registries:
        print("cargo registry sources were not found. Run: cargo fetch", file=sys.stderr)
        return 1

    try:
        crates = linked_crates()
        ensure_chosen_license_applies(crates)
        rendered, missing = render(crates, registries)
        ensure_missing_texts_are_known(missing)
    except RuntimeError as error:
        print(f"Third-party notices generation failed: {error}", file=sys.stderr)
        return 1

    OUTPUT_PATH.write_text(rendered, encoding="utf-8", newline="\n")
    print(
        f"Third-party notices written: {OUTPUT_PATH.relative_to(REPOSITORY_ROOT)} "
        f"({len(crates)} crate(s))."
    )
    for name, version in sorted(set(missing)):
        print(f"  {name} {version} ships no license file; the notice points at its repository.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
