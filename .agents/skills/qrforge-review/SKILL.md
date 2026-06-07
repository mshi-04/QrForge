---
name: qrforge-review
description: QrForge のコードレビューで、Critical、Suggestion、Nitpick の分類、責務境界、公開 API、JNI 境界、安全性、テスト不足を確認するための skill。
---

# QrForge レビュー

## 手順

1. `docs/review-rules.md` と、変更レイヤーに関係する docs を読む。
2. コード、差分、docs、テストログで確認できる事実に基づいて見る。
3. 指摘を重要度順に先に書く。
4. `Critical`、`Suggestion`、`Nitpick` を使い、指摘がない分類には `該当なし` と書く。
5. 最後に確認した観点と残るリスクを書く。

## 読む文書

- `docs/review-rules.md`
- `docs/architecture.md`
- `docs/coding-rules.md`

public API 変更では `docs/api-design.md` も読む。テスト変更では `docs/unit-test.md` も読む。検証結果を判断するときは `docs/setup.md` も読む。

## 重点観点

- Rust core、JNI bridge、Kotlin wrapper、sample app の責務境界。
- public API の互換性と例外の意味。
- JNI の安全性: panic handling、reference、byte array、string 変換。
- エラー表現: `null`、空配列、曖昧な `RuntimeException` を使っていないか。
- 変更レイヤーに合うテストと確認があるか。
- 未確認の振る舞いや未実行テストを断定していないか。

## 出力フォーマット

```text
### Critical

該当なし

### Suggestion

1. 対象ファイル: path/to/File.kt
   該当箇所: 関数名、行番号、差分ブロックなど
   問題点: 何が問題か
   理由: なぜ問題か
   修正案: どう直すか

### Nitpick

該当なし

### 確認した観点

- 責務境界
- Android SDK 風 API
- JNI 境界のエラー処理

### 残るリスク

- 実行できなかった確認、未検証の環境、外部仕様確認が必要な点
```
