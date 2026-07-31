#!/usr/bin/env python3
"""Synchronize Claude Code skill bodies from the canonical Codex copies."""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
CANONICAL_SKILLS_ROOT = REPOSITORY_ROOT / ".agents" / "skills"
CLAUDE_SKILLS_ROOT = REPOSITORY_ROOT / ".claude" / "skills"
SKILL_FILE_NAME = "SKILL.md"


def canonical_skill_files() -> dict[str, Path]:
    return {
        path.parent.name: path
        for path in sorted(CANONICAL_SKILLS_ROOT.glob(f"*/{SKILL_FILE_NAME}"))
    }


def claude_skill_files() -> dict[str, Path]:
    return {
        path.parent.name: path
        for path in sorted(CLAUDE_SKILLS_ROOT.glob(f"*/{SKILL_FILE_NAME}"))
    }


def skill_copy_errors() -> list[str]:
    canonical = canonical_skill_files()
    claude = claude_skill_files()
    errors: list[str] = []

    if not canonical:
        return [f"No canonical skills found under {CANONICAL_SKILLS_ROOT}"]

    for name, source in canonical.items():
        target = claude.get(name)
        if target is None:
            errors.append(f"Missing Claude Code copy: .claude/skills/{name}/{SKILL_FILE_NAME}")
        elif source.read_bytes() != target.read_bytes():
            errors.append(
                f"Skill body differs from canonical copy: .claude/skills/{name}/{SKILL_FILE_NAME}"
            )

    for extra_name in sorted(claude.keys() - canonical.keys()):
        errors.append(
            f"Claude Code skill has no canonical Codex source: "
            f".claude/skills/{extra_name}/{SKILL_FILE_NAME}"
        )

    return errors


def sync_skill_copies() -> int:
    copied = 0
    for name, source in canonical_skill_files().items():
        target = CLAUDE_SKILLS_ROOT / name / SKILL_FILE_NAME
        target.parent.mkdir(parents=True, exist_ok=True)
        if not target.exists() or source.read_bytes() != target.read_bytes():
            shutil.copyfile(source, target)
            copied += 1
    return copied


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--sync",
        action="store_true",
        help="Copy canonical .agents skill bodies into .claude.",
    )
    args = parser.parse_args()

    if args.sync:
        copied = sync_skill_copies()
        print(f"Synchronized {copied} skill file(s).")

    errors = skill_copy_errors()
    if errors:
        print("Skill consistency check failed:")
        for error in errors:
            print(f"- {error}")
        return 1

    print(f"Skill copies are consistent ({len(canonical_skill_files())} skill(s)).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
