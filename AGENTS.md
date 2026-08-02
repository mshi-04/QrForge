# QrForge AI 開発ガイド

Codex 用の開発ガイド。

QrForge は、Android/Kotlin から SDK 風に呼び出せる Rust 製 QR コード生成ライブラリ。

## 基本行動

- 実装前に対象範囲・責務境界・確認方法を含む計画を提示する。
- Rust core・JNI bridge・Kotlin wrapper の責務を混ぜない。
- JNI 関数を利用者向け公開 API として扱わない。

## Android ABI / native library

ABI・native library の配置とレイヤー責務は [docs/architecture.md](docs/architecture.md)、ビルド・
再生成・同梱確認は [docs/setup.md](docs/setup.md)、公開範囲は [docs/api-design.md](docs/api-design.md)
を正典とする。運用詳細はこのファイルに重複させない。

## Skill

文書リンクと ABI 設定の整合性は `python scripts/check_repo_consistency.py` で確認する。
この script には Python 3.10 以降を使い、実行名が `python3` の環境では読み替える。

| Skill | 用途 | 定義 |
|------|------|------|
| `coding` | 実装・修正作業 | [.agents/skills/coding/SKILL.md](.agents/skills/coding/SKILL.md) |
| `unit-test` | UnitTest 作成・修正 | [.agents/skills/unit-test/SKILL.md](.agents/skills/unit-test/SKILL.md) |
| `layer-review` | コードレビュー | [.agents/skills/layer-review/SKILL.md](.agents/skills/layer-review/SKILL.md) |

`layer-review` は Claude Code 組み込みの `/review`、`/code-review`、`/security-review` と
名前が衝突しないようにしたもの。

## 参照文書

| 文書 | 正典として扱う内容 |
|------|------------------|
| [docs/architecture.md](docs/architecture.md) | レイヤ、依存方向、生成フロー、責務境界の判断 |
| [docs/api-design.md](docs/api-design.md) | 公開 API の契約、例外分類、値域定数の同期 |
| [docs/coding-rules.md](docs/coding-rules.md) | レイヤ内での書き方（可視性、例外、panic 境界、画素操作） |
| [docs/unit-test.md](docs/unit-test.md) | テストの置き場所、境界、書き方 |
| [docs/setup.md](docs/setup.md) | ビルド・確認コマンド、`.so` の鮮度、CI との対応 |
| [docs/review-rules.md](docs/review-rules.md) | 指摘分類、見る順序、検証の妥当性、出力フォーマット |

詳細な判断は `docs/` 配下の文書を優先する。
