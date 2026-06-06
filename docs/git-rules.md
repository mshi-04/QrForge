# QrForge Git 運用ルール

## コミット

- コミットメッセージは日本語。
- 1 行目は変更内容が分かる短い要約。
- 英語の Conventional Commits は必須ではない。

例:

```text
Rust core の QR PNG 生成を追加
JNI bridge のエラー変換を整理
Kotlin wrapper の公開 API を調整
```

## コミット前

- `git status --short --branch` で対象外の変更を確認する。
- 依頼範囲外の変更を勝手に戻さない。
- 実装変更を含む場合は関連する確認コマンドを実行する。
- 環境要因で確認できない場合は理由を報告する。

## ブランチ

- `feature/<topic>` を使う。
- topic は短い英語にする。
- 基準ブランチは `develop`。
- `main` はリリース反映先として扱う。

例:

```text
feature/rust-core
feature/jni-bridge
feature/kotlin-wrapper
```

## PR

- タイトルは日本語で短く書く。
- 本文には「何を変えたか」「なぜ変えたか」「確認コマンドと結果」を含める。
- 依頼範囲外の変更が混ざっていないことを確認してから作成する。

## AI エージェント向け

- ユーザーから明示されない限り英語コミットメッセージでコミットしない。
- 依頼範囲外の変更をコミットへ混ぜない。
- コミット後は hash、メッセージ、確認結果を報告する。
