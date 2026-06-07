---
name: qrforge-coding
description: QrForge の実装・修正作業で、Rust core、JNI bridge、Kotlin wrapper、sample app の責務境界を守って変更計画、実装、確認結果報告を行うための skill。
---

# QrForge コーディング

## 手順

1. `AGENTS.md` を読み、下記の関連 docs を読む。
2. 編集前に、対象範囲、責務境界、変更予定ファイル、実行する確認コマンド、やらないことを提示する。
3. ユーザー依頼が複数レイヤー変更を要求しない限り、選んだレイヤー内に変更を閉じる。
4. 変更レイヤーに合う確認コマンドを `docs/setup.md` から選んで実行する。
5. 変更ファイル、コマンド結果、未実施の確認、残るリスクを報告する。

## 読む文書

- `docs/architecture.md`
- `docs/coding-rules.md`
- `docs/setup.md`

public API や例外を変える場合は `docs/api-design.md` も読む。テストを追加・変更する場合は `docs/unit-test.md` も読む。

## レイヤールール

- Rust core は QR 生成、PNG エンコード、core validation だけを担当する。
- JNI bridge は Kotlin / Rust の型変換とエラー変換だけを担当する。
- Kotlin wrapper は public API、入力検証、Bitmap decode、利用者向け例外を担当する。
- sample app は利用例と手動確認 UI に留める。
- JNI 関数や `System.loadLibrary("qrforge")` を利用者向け API として露出しない。
- `libqrforge.so` は `qrforge/src/main/jniLibs/<abi>/` だけに置く。

## 確認

`docs/setup.md` から最小限の確認コマンドを選ぶ。docs のみの変更では、コードファイルが変わっていないことを確認する。
