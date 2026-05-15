# QrForge レビュールール

## 目的

この文書は、AI が QrForge のコードレビューを行うときの指摘分類、出力形式、重点確認観点を定める。QrForge は Rust core、JNI bridge、Kotlin wrapper、Android app の責務境界が品質に直結するため、レビューでは単なるスタイル指摘よりも、境界違反、公開 API の安定性、JNI 境界の安全性を優先して確認する。

レビュー時は、事実確認できるコード、差分、テスト結果、ドキュメントに基づいて指摘する。確認できない内容は断定せず、「可能性がある」「確認が必要」と明記する。

## 指摘分類

指摘は重要度順に次の 3 分類へ分ける。

1. `Critical`
2. `Suggestion`
3. `Nitpick`

分類は必ずこの順序で出力する。各分類内では、上から順に項番を振る。項番は分類ごとに `1` から始める。指摘がない分類には `該当なし` と書く。

## Critical

`Critical` は、修正しないとクラッシュ、セキュリティ問題、メモリ安全性の問題、公開 API の破壊、責務境界の重大な崩れ、または QR 生成機能の主要失敗につながる指摘に使う。

該当例:

- JNI 境界で Rust panic がそのまま外へ漏れる。
- Rust core の `unwrap` / `expect` により不正入力で native crash が発生し得る。
- JNI の local reference や byte array 変換でメモリ安全性の懸念がある。
- Rust core が Android の `Bitmap` や JNI 型に依存している。
- Android app が JNI 関数を直接呼び出している。
- Kotlin wrapper を通さず、利用者向け API に Rust/JNI の詳細が漏れている。
- `QrForge.createBitmap(text)` など既存 public API の戻り値、例外、意味を破壊的に変更している。
- 生成失敗を `null`、空 `ByteArray`、不明な `RuntimeException` で曖昧に扱っている。
- PNG bytes ではないデータを `createPngBytes` が返す可能性がある。
- native library load failure と QR 生成失敗を区別できず、利用者が復旧判断できない。

## Suggestion

`Suggestion` は、重大な不具合ではないが、設計の一貫性、保守性、テスト容易性、利用者体験を改善するために対応すべき指摘に使う。

該当例:

- `QrOptions` の default 値が Kotlin と Rust で重複し、将来的にずれる可能性がある。
- 入力バリデーションの責務が Kotlin wrapper と Rust core に散らばっている。
- `Bitmap` 変換の失敗理由が利用者に伝わりにくい。
- JNI bridge に軽微なロジックが増え始めており、core へ移した方がよい。
- Android app と SDK wrapper の package が近すぎて、将来のライブラリ化で分離しづらい。
- public API に将来拡張しづらい引数設計が入っている。
- Rust core のテストが Android/JNI なしで十分に確認できない。
- `ByteArray` 生成と `Bitmap` 生成のテスト観点が不足している。
- エラー型や例外 message が利用者向けとして曖昧である。

## Nitpick

`Nitpick` は、動作や設計への影響が小さいが、読みやすさ、命名、文書整合性、軽微な保守性を上げる指摘に使う。

該当例:

- internal class 名がやや曖昧で、JNI binding だと分かりにくい。
- コメントが実装内容をそのまま繰り返している。
- docs の呼び出し例と実装の引数名が揺れている。
- 例外 message の文体が統一されていない。
- test 名から検証条件が少し読み取りづらい。
- import、空行、ファイル配置が既存スタイルと軽くずれている。

Nitpick は、本質的なレビュー結果を埋もれさせないよう最小限にする。

## 指摘フォーマット

各指摘には、必ず次の項目を含める。

```text
### Critical

1. 対象ファイル: path/to/File.kt
   該当箇所: 関数名、行番号、差分の該当ブロックなど
   問題点: 何が問題か
   理由: なぜ問題なのか、どの責務や仕様に反するのか
   修正案: 具体的にどう直すか

### Suggestion

該当なし

### Nitpick

1. 対象ファイル: path/to/File.md
   該当箇所: 見出し名または該当行
   問題点: 何が問題か
   理由: なぜ直す価値があるか
   修正案: 具体的にどう直すか
```

行番号が取得できる場合は行番号を含める。行番号が不明な場合は、関数名、クラス名、見出し名、差分ブロックなど、レビュー対象を特定できる情報を書く。

## 事実確認のルール

- コード、差分、テストログ、ドキュメントで確認できる事実を優先する。
- 推測は推測として書く。
- 外部 crate、Android API、NDK、JNI の仕様に依存する内容は、必要に応じて公式情報や実際のコードで確認する。
- 実行していないテストを「通った」と書かない。
- 見ていないファイルの内容を断定しない。
- 既存設計と異なると指摘する場合は、根拠となる文書または既存コードを示す。

## 重点レビュー観点

### 責務境界

Rust core、JNI bridge、Kotlin wrapper、Android app の責務が混ざっていないか確認する。

- Rust core は QR 生成と PNG エンコードに集中しているか。
- Rust core が Android、JNI、Kotlin、UI に依存していないか。
- JNI bridge が型変換と境界処理だけを担当しているか。
- Kotlin wrapper が利用者向け API と例外変換を担当しているか。
- Android app が SDK wrapper の利用例に留まり、内部 API を呼んでいないか。

### Android SDK 風 API

利用者に Rust/JNI の詳細が漏れていないか確認する。

- 利用者が `QrForge.createBitmap(text)` のように目的ベースで呼べるか。
- `QrForgeNative`、`external fun`、JNI symbol、Rust crate 名が public API や利用例に出ていないか。
- `System.loadLibrary` の扱いが利用者に露出していないか。
- `Bitmap` が必要な利用者と PNG `ByteArray` が必要な利用者の両方に自然な API があるか。

### 公開 API の破壊的変更

public API は互換性維持対象として見る。

- 既存 public API の関数名、引数、戻り値、例外の意味が変わっていないか。
- 新しい設定を追加するために既存 API を壊していないか。
- overload で拡張できるところを既存関数の意味変更で済ませていないか。
- public model に property を追加する場合、default 値があるか。

### 例外設計

失敗原因が利用者に伝わるか確認する。

- 入力不正、生成失敗、PNG decode 失敗、native library load failure を区別できるか。
- `null`、空配列、汎用 `RuntimeException` で失敗を曖昧にしていないか。
- Rust core の error が Kotlin 側で適切な例外へ変換されているか。
- 例外 message が利用者向けに理解できる内容か。

### 入力バリデーション

入力仕様が docs と実装で一致しているか確認する。

- 空文字、blank、長すぎる文字列、日本語など UTF-8 文字列の扱いが明確か。
- Kotlin wrapper と Rust core の検証責務が矛盾していないか。
- SDK 側で勝手に trim して QR 化する内容を変えていないか。
- QR 仕様上扱えない入力で crash せず、利用者向け例外になるか。

### Bitmap / ByteArray 変換

Android 向け戻り値が正しく扱われているか確認する。

- `createPngBytes` が PNG signature を持つ bytes を返すか。
- `createBitmap` が decode 失敗時に `null` を返さず例外化しているか。
- PNG bytes 生成と Bitmap decode の責務が混ざっていないか。
- Android UI component への表示処理が SDK wrapper に入り込んでいないか。

### JNI 境界とメモリ安全性

JNI 境界は重点的に確認する。

- Rust panic が JNI 境界から外へ漏れないか。
- `jbyteArray` 変換、文字列変換、local reference の扱いに問題がないか。
- Rust 側の bytes 所有権が明確か。
- JNI で発生した例外や null を無視して処理を続けていないか。
- 複数回呼び出しや大きな入力で reference leak や native crash が起きる可能性がないか。

## レビュー結果の書き方

レビュー結果は、指摘を先に書く。要約は指摘の後に短く置く。

推奨構成:

```text
### Critical

該当なし

### Suggestion

1. 対象ファイル: ...
   該当箇所: ...
   問題点: ...
   理由: ...
   修正案: ...

### Nitpick

該当なし

### 確認した観点

- 責務境界
- Android SDK 風 API
- JNI 境界のエラー処理

### 残るリスク

- 実行できなかった確認、未検証の環境、外部仕様確認が必要な点
```

指摘がない場合も、全分類を出力し、各分類に `該当なし` と書く。その上で、確認した観点と残るリスクを短く報告する。
