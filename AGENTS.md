# QrForge AI 開発ガイド

QrForge は、Android/Kotlin から SDK 風に呼び出せる Rust 製 QR コード生成ライブラリ。

**現在の Phase: Phase 6 完了** — 詳細は [docs/development-plan.md](docs/development-plan.md)

## 基本行動

- 日本語で回答する。
- 実装前に対象範囲・責務境界・確認方法を含む計画を提示する。
- Rust core・JNI bridge・Kotlin wrapper の責務を混ぜない。
- JNI 関数を利用者向け公開 API として扱わない。
- 変更後は確認コマンドを実行し結果を報告する（コマンドは [docs/setup.md](docs/setup.md)）。
- `rg` コマンドを使用しない（`Grep` ツールを使う）。

## 参照文書

| 文書 | 内容 |
|------|------|
| [docs/architecture.md](docs/architecture.md) | レイヤ構成・責務・依存方向 |
| [docs/development-plan.md](docs/development-plan.md) | Phase 計画・完了条件 |
| [docs/coding-rules.md](docs/coding-rules.md) | Kotlin・Rust・JNI の実装ルール |
| [docs/unit-test.md](docs/unit-test.md) | UnitTest・Instrumented Test の作成ルール |
| [docs/api-design.md](docs/api-design.md) | 公開 API・例外・呼び出し例 |
| [docs/setup.md](docs/setup.md) | ビルド・テスト実行手順 |
| [docs/review-rules.md](docs/review-rules.md) | レビュー指摘分類・フォーマット |
| [docs/git-rules.md](docs/git-rules.md) | コミット・ブランチ・PR ルール |

詳細な判断は `docs/` 配下の文書を優先する。
