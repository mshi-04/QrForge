# QrForge AI 開発ガイド

QrForge は、Android/Kotlin から SDK 風に呼び出せる Rust 製 QR コード生成ライブラリ。

## 基本行動

- 実装前に対象範囲・責務境界・確認方法を含む計画を提示する。
- Rust core・JNI bridge・Kotlin wrapper の責務を混ぜない。
- JNI 関数を利用者向け公開 API として扱わない。

## Android ABI / native library

- Rust native library は Android library module の `qrforge/src/main/jniLibs/<abi>/libqrforge.so` に配置する。sample app の `app/src/main/jniLibs` には置かない。
- 既定の対応 ABI は `arm64-v8a`、`armeabi-v7a`、`x86_64` とする。
- `x86` は 32-bit x86 emulator が必要な場合のみ追加する。追加する場合は `i686-linux-android` target、`cargo ndk -t x86`、Android 側 `abiFilters`、README / setup / CI を同時に見直す。
- `System.loadLibrary("qrforge")` は internal 実装に閉じ、ABI 追加のために公開 API や sample app から直接 JNI を扱わせない。
- `.so` を再生成した場合は、対象 ABI のディレクトリと AAR / APK への同梱結果を確認し、実行していない端末検証は未実施として報告する。

## Skill

用途別の skill を、Codex 用は `.agents/skills/`、Claude Code 用は `.claude/skills/` に置く。
`.agents/skills/` を正典とし、Claude Code 用の本文は `python scripts/sync_skills.py --sync` で同期する。
名前と用途は両者で対応しており、本文は同一に保つ。判断の実体は `docs/` 配下にあり、skill は
手順・判断の入口・報告の型だけを持つ。

文書リンク、skill 本文、ABI 設定の整合性は `python scripts/check_repo_consistency.py` で確認する。
これらの script には Python 3.10 以降を使い、実行名が `python3` の環境では読み替える。

| Skill | 用途 | Codex | Claude Code |
|------|------|-------|-------------|
| `coding` | 実装・修正作業 | [.agents/skills/coding/SKILL.md](.agents/skills/coding/SKILL.md) | [.claude/skills/coding/SKILL.md](.claude/skills/coding/SKILL.md) |
| `unit-test` | UnitTest 作成・修正 | [.agents/skills/unit-test/SKILL.md](.agents/skills/unit-test/SKILL.md) | [.claude/skills/unit-test/SKILL.md](.claude/skills/unit-test/SKILL.md) |
| `layer-review` | コードレビュー | [.agents/skills/layer-review/SKILL.md](.agents/skills/layer-review/SKILL.md) | [.claude/skills/layer-review/SKILL.md](.claude/skills/layer-review/SKILL.md) |

`layer-review` は Claude Code 組み込みの `/review`、`/code-review`、`/security-review` と
名前が衝突しないようにしたもの。

## 参照文書

各事実の正典は 1 つに決めてある。重複した記述を見つけたら、正典側を直す。

| 文書 | 正典として扱う内容 |
|------|------------------|
| [docs/architecture.md](docs/architecture.md) | レイヤ、依存方向、生成フロー、責務境界の判断 |
| [docs/api-design.md](docs/api-design.md) | 公開 API の契約、例外分類、値域定数の同期 |
| [docs/coding-rules.md](docs/coding-rules.md) | レイヤ内での書き方（可視性、例外、panic 境界、画素操作） |
| [docs/unit-test.md](docs/unit-test.md) | テストの置き場所、境界、書き方 |
| [docs/setup.md](docs/setup.md) | ビルド・確認コマンド、`.so` の鮮度、CI との対応 |
| [docs/review-rules.md](docs/review-rules.md) | 指摘分類、見る順序、検証の妥当性、出力フォーマット |

詳細な判断は `docs/` 配下の文書を優先する。
