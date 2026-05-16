# QrForge AI 開発ガイド

QrForge は、Android/Kotlin から SDK 風に呼び出せる Rust 製 QR コード生成ライブラリ。

## 基本行動

- 日本語で回答する。
- 実装前に対象範囲・責務境界・確認方法を含む計画を提示する。
- Rust core・JNI bridge・Kotlin wrapper の責務を混ぜない。
- JNI 関数を利用者向け公開 API として扱わない。
- 変更後は確認コマンドを実行し結果を報告する（コマンドは [docs/setup.md](docs/setup.md)）。
- `rg` コマンドを使用しない（`Grep` ツールを使う）。

## Android ABI / native library

- Rust native library は Android library module の `qrforge/src/main/jniLibs/<abi>/libqrforge.so` に配置する。sample app の `app/src/main/jniLibs` には置かない。
- 既定の対応 ABI は `arm64-v8a`、`armeabi-v7a`、`x86_64` とする。
- `x86` は 32-bit x86 emulator が必要な場合のみ追加する。追加する場合は `i686-linux-android` target、`cargo ndk -t x86`、Android 側 `abiFilters`、README / setup / CI を同時に見直す。
- `System.loadLibrary("qrforge")` は internal 実装に閉じ、ABI 追加のために公開 API や sample app から直接 JNI を扱わせない。
- `.so` を再生成した場合は、対象 ABI のディレクトリと AAR / APK への同梱結果を確認し、実行していない端末検証は未実施として報告する。

## 参照文書

| 文書 | 内容 |
|------|------|
| [docs/architecture.md](docs/architecture.md) | レイヤ構成・責務・依存方向 |
| [docs/coding-rules.md](docs/coding-rules.md) | Kotlin・Rust・JNI の実装ルール |
| [docs/unit-test.md](docs/unit-test.md) | UnitTest・Instrumented Test の作成ルール |
| [docs/api-design.md](docs/api-design.md) | 公開 API・例外・呼び出し例 |
| [docs/setup.md](docs/setup.md) | ビルド・テスト実行手順 |
| [docs/review-rules.md](docs/review-rules.md) | レビュー指摘分類・フォーマット |
| [docs/sub-agent-guidelines.md](docs/sub-agent-guidelines.md) | サブエージェント運用ガイドライン |
| [docs/git-rules.md](docs/git-rules.md) | コミット・ブランチ・PR ルール |

詳細な判断は `docs/` 配下の文書を優先する。
