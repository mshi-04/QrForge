# Git 運用ルール

この文書は、QrForge の branch・commit・PR の運用と、repository に commit している native library (`.so`) のバイナリ差分の扱い方をまとめる。ビルドコマンドは [setup.md](setup.md)、レビュー観点は [review-rules.md](review-rules.md) を参照する。

## ブランチ

| 項目 | ルール |
|------|--------|
| 作業ブランチ | `feature/<topic>` |
| topic | 短い英語（kebab-case） |
| 分岐元・PR の base | `develop` |
| `main` | リリース反映先。直接作業しない |

```text
feature/rust-core
feature/jni-bridge
feature/kotlin-wrapper
```

`main` と `develop` への push、および両ブランチ宛の PR で CI が走る（`.github/workflows/ci.yml`）。

## コミットメッセージ

- 日本語で書く。ユーザーから明示的な指示がない限り英語で commit しない。
- 1 行目は「何を変えたか」が分かる短い要約。
- 英語の Conventional Commits 形式は必須ではない。

```text
Rust core の QR PNG 生成を追加
```

```text
JNI bridge のエラー変換を整理
```

```text
Kotlin wrapper の公開 API を調整
```

## コミット前チェック

| 確認 | 内容 |
|------|------|
| 作業ツリー | `git status --short --branch` で対象外の変更を把握する |
| 既存の変更 | ユーザーが元から持っていた未コミット変更を勝手に戻さない |
| 範囲 | 依頼範囲外の変更を同じ commit に混ぜない |
| 検証 | 変更箇所に対応する確認コマンドを実行する（[setup.md](setup.md)） |
| 未実施 | 環境要因で確認できなかったものは「未検証」と明示する。通ったことにしない |

```bash
git status --short --branch
```

## native library (`.so`) のバイナリ差分

`qrforge/src/main/jniLibs/<abi>/libqrforge.so` は repository に commit している。対応 ABI は `arm64-v8a`・`armeabi-v7a`・`x86_64` の 3 つ。

- `rust/` を変更した時点で、commit 済みの `.so` は古いままになる。その変更を Android 側へ届けるなら、commit 前に再ビルドする（手順は [setup.md](setup.md)）。
- `.so` は diff 上 `Bin NNNN -> MMMM bytes` としか出ず、読んでレビューできない。レビュアーは Rust 側の source diff と検証結果を根拠にするため、commit メッセージと PR 本文で「どの `rust/` 変更がこのバイナリ差分を生んだか」を必ず書く。
- 3 ABI をまとめて再生成したことを確認する。一部 ABI だけ更新された commit は不具合として扱う。

```bash
git diff --stat qrforge/src/main/jniLibs
```

```text
qrforge/src/main/jniLibs/arm64-v8a/libqrforge.so   | Bin 842656 -> 843688 bytes
qrforge/src/main/jniLibs/armeabi-v7a/libqrforge.so | Bin 527936 -> 528644 bytes
qrforge/src/main/jniLibs/x86_64/libqrforge.so      | Bin 786224 -> 787880 bytes
```

次の 2 つは、理由を明示せずに commit しない。

| 状態 | 問題 |
|------|------|
| `rust/` の変更なしに `.so` だけ更新 | 由来不明のバイナリ差分になり、レビューできない |
| `rust/` を変更したまま `.so` を据え置き | Android 側は変更前の native library を使い続ける |

## Cargo.lock

`Cargo.lock` は commit 対象。依存や feature の変更は必ずここに差分として現れるため、差分が出た場合は PR 本文でも触れる。

## PR

- タイトルは日本語で短く書く。
- 本文には次を含める。
  - 何を変えたか
  - なぜ変えたか
  - 実行した確認コマンドとその結果（未実施のものは未実施と書く）
  - `.so` を更新した場合は対応する `rust/` 変更、`Cargo.lock` に差分がある場合はその理由
- 作成前に、依頼範囲外の変更が混ざっていないことを確認する。

## AI エージェント向け

- commit 後は hash・commit メッセージ・検証結果を報告する。
- ユーザーの確認なしに push しない。
- 依頼範囲外の変更を commit に混ぜない。
- 実行できなかった確認は、成功と書かずに未実施として報告する。
