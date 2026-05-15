# QrForge AI 開発ガイド

## リポジトリの目的

QrForge は、Android/Kotlin から SDK のように呼び出せる Rust 製 QR コード生成ライブラリを作るためのリポジトリです。Kotlin 側では `QrForge.createBitmap(text)` のような自然な API を提供し、Rust 側では文字列から QR コード PNG データを生成します。

## AI エージェントの基本行動

- 日本語で回答する。
- 実装前に必ず対象範囲、責務境界、確認方法を含む計画を提示する。
- 既存ファイルがある場合は、内容を確認してから追記または更新する。
- Rust core、JNI bridge、Kotlin wrapper の責務を混ぜない。
- JNI 関数を利用者向け公開 API として扱わない。
- 変更後は関連する確認コマンドを実行し、結果を報告する。
- このリポジトリでは `rg` コマンドを使用しない。

## 実装前に読む文書

- [docs/architecture.md](docs/architecture.md): 全体アーキテクチャ、責務、依存方向
- [docs/development-plan.md](docs/development-plan.md): Phase ごとの実装計画、完了条件、テスト観点
- [docs/coding-rules.md](docs/coding-rules.md): Kotlin、Rust、JNI の実装ルール
- [docs/api-design.md](docs/api-design.md): Android 向け公開 API、例外、呼び出し例
- [docs/review-rules.md](docs/review-rules.md): AI レビュー時の指摘分類、観点、出力形式
- [docs/git-rules.md](docs/git-rules.md): Git 操作、コミットメッセージ、コミット前確認のルール

詳細な判断は `docs/` 配下の文書を優先する。
