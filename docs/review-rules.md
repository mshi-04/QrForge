# QrForge レビュールール

レビューでは責務境界、公開 API の安定性、JNI 境界の安全性を優先する。事実確認できるコード、差分、テスト結果、docs に基づいて指摘する。

## 指摘分類

重要度順に出力する。

1. `Critical`
2. `Suggestion`
3. `Nitpick`

指摘がない分類には `該当なし` と書く。

## 分類基準

| 分類 | 使う場面 |
|------|----------|
| `Critical` | crash、memory safety、公開 API 破壊、責務境界の重大な崩れ、QR 生成の主要失敗 |
| `Suggestion` | 設計一貫性、保守性、テスト容易性、利用者体験を改善すべき問題 |
| `Nitpick` | 動作影響が小さい命名、文体、軽微な整合性 |

## 重点観点

- Rust core が Android / JNI / Kotlin に依存していないか。
- JNI bridge が型変換とエラー変換だけを担当しているか。
- Android app が SDK wrapper 以外の内部 API を呼んでいないか。
- `QrForgeNative`、`external fun`、JNI symbol、`System.loadLibrary` が公開 API や利用例に出ていないか。
- 既存 public API の関数名、引数、戻り値、例外の意味が変わっていないか。
- 入力不正、生成失敗、PNG decode 失敗、native library load failure を区別できるか。
- `null`、空配列、汎用 `RuntimeException` で失敗を曖昧にしていないか。
- Rust panic、JNI local reference、byte array、文字列変換に安全性の懸念がないか。
- 変更内容に対してテストと確認コマンドが足りているか。

## 指摘フォーマット

```text
### Critical

該当なし

### Suggestion

1. 対象ファイル: path/to/File.kt
   該当箇所: 関数名、行番号、差分ブロックなど
   問題点: 何が問題か
   理由: なぜ問題なのか
   修正案: 具体的にどう直すか

### Nitpick

該当なし

### 確認した観点

- 責務境界
- Android SDK 風 API
- JNI 境界のエラー処理

### 残るリスク

- 実行できなかった確認、未検証の環境、外部仕様確認が必要な点
```

## 事実確認

- 推測は推測として書く。
- 実行していないテストを「通った」と書かない。
- 見ていないファイルの内容を断定しない。
- 既存設計と異なる指摘には根拠となる docs または既存コードを示す。
