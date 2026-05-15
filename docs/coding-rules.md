# QrForge コーディングルール

## 基本方針

QrForge は Android SDK のように扱える Kotlin API と、Android から独立した Rust core を組み合わせる。実装時は、呼び出しやすさ、責務境界、将来のライブラリ化を優先する。

AI が実装する場合は、作業前に対象 Phase、変更予定ファイル、確認コマンド、やらないことを提示する。

## Kotlin 側の実装ルール

### 公開 API

- 利用者向け入口は `QrForge` に集約する。
- 最初の public API は `createBitmap(text: String)` と `createPngBytes(text: String)` を基本にする。
- public API 名は利用目的を表す。JNI や Rust の都合を名前に入れない。
- 引数が増える場合は、既存関数の意味を変えず overload を追加する。
- Android 利用者が `ByteArray` と `Bitmap` のどちらも選べるようにする。

### 可視性

- JNI binding は `internal` にする。
- native method を app や外部利用者から直接呼ばせない。
- `System.loadLibrary` の呼び出しは公開 API から隠す。
- public にする型は互換性維持の対象として扱う。

### Bitmap 変換

- PNG bytes から `Bitmap` への変換は Kotlin wrapper の責務とする。
- `BitmapFactory.decodeByteArray` が失敗した場合は、利用者向け例外へ変換する。
- Android UI component への set 処理は SDK wrapper に入れない。表示は app 側の責務にする。

### 入力検証

- `text` は Kotlin wrapper で基本検証する。
- 空文字を許可するか拒否するかは API design に明記し、実装で揺らさない。
- 長すぎる文字列や QR 仕様上扱えない入力は、Rust core のエラーを Kotlin 例外へ変換する。

## Rust 側の実装ルール

### Core の独立性

- Rust core は Android、JNI、Kotlin に依存しない。
- QR 生成と PNG エンコードを core の責務にする。
- JNI 向けの関数名、JNI 型、Android 固有のエラー表現を core に入れない。
- core API は Rust 単体テストで検証できる形にする。

### 型設計

- 入力 options は Rust 側でも明示的な struct にする。
- default 値は `Default` 実装または専用 constructor で管理する。
- 生成結果は `Result<Vec<u8>, QrForgeError>` のように成功と失敗を分ける。
- エラー型は原因を表現し、文字列だけで流さない。

### 画像生成

- 生成する bytes は PNG として妥当であることをテストする。
- size、margin の扱いは core で一貫させる。
- Android の `Bitmap` サイズ都合で Rust core の API を歪めない。

## JNI 実装時の注意点

### 境界の役割

- JNI bridge は Kotlin と Rust core の型変換だけを担当する。
- QR 生成ロジックを JNI bridge に書かない。
- Kotlin wrapper の入力検証を重複させすぎない。ただし安全性に必要な defensive check は行う。

### メモリと例外

- Rust の panic を JNI 境界から外へ漏らさない。
- Rust の error は Kotlin 側で扱える例外または error result に変換する。
- JNI local reference の扱いに注意し、大量生成時に reference leak を起こさない。
- `jbyteArray` へ変換する際は bytes の所有権を明確にする。
- null input や不正な JVM state を想定する。

### 命名

- Kotlin 側 native binding は `QrForgeNative` のように内部実装であることが分かる名前にする。
- JNI exported symbol は JNI 仕様に従うが、利用者向けドキュメントには載せない。
- Rust JNI crate と Rust core crate を分ける場合、`qrforge-core` と `qrforge-jni` のように責務が分かる名前にする。

## エラー処理方針

### Kotlin

- 利用者向けには `QrForgeException` などの SDK 例外を用意する。
- 入力不正は `IllegalArgumentException` または `QrForgeException.InvalidInput` のどちらにするか API design で固定する。
- native library load failure、QR 生成失敗、PNG decode failure を区別できるようにする。
- 例外 message は利用者が原因を判断できる内容にする。

### Rust

- core は `Result` を返す。
- 失敗原因は enum で表現する。
- panic 前提の `unwrap` や `expect` は core logic で避ける。
- 外部 crate の error は QrForge の error 型へ変換する。

### JNI

- Rust error を JNI bridge で握りつぶさない。
- Kotlin 側で安定した例外に変換できる情報を返す。
- panic を catch する必要がある場合は、境界で安全に変換する。

## 命名規則

### Kotlin

- 公開入口: `QrForge`
- option model: `QrOptions`
- SDK 例外: `QrForgeException`
- 内部 native binding: `QrForgeNative`
- package は app UI と SDK wrapper を分ける。

避ける名前:

- `NativeLib`
- `JniHelper`
- `RustBridge` を public API にすること
- `generate` だけの曖昧な public 関数

### Rust

- core crate: `qrforge-core`
- JNI crate: `qrforge-jni`
- core function: `generate_png` など目的が明確な名前
- error type: `QrForgeError`
- option type: `QrOptions` または Rust 側に適した `QrEncodeOptions`

## 公開 API を安定させる方針

- public API は実装都合で頻繁に変えない。
- 既存 API の戻り値や例外の意味を変える場合は、文書を更新して理由を明記する。
- 新しい設定は `QrOptions` に追加し、引数リストを長くしすぎない。
- 破壊的変更が必要な場合は、移行方針を先に書く。
- README や docs に載せた API は互換性維持対象として扱う。

## Android SDK 風 API を保つためのルール

- 利用者の最短コードを短く保つ。
- QR 生成の基本ケースは 1 行で呼び出せるようにする。
- UI component を直接要求しない。
- Android lifecycle に不要な制約を持ち込まない。
- 非同期 API は必要になってから追加し、最初は同期 API の責務を明確にする。
- `Bitmap` が必要な利用者と PNG bytes が必要な利用者の両方を想定する。

## AI がやりがちなアンチパターン

- `MainActivity` に QR 生成ロジックを書く。
- JNI 関数を public API として案内する。
- Kotlin wrapper を飛ばして app から native method を直接呼ぶ。
- Rust core に Android 固有の型や命名を入れる。
- JNI bridge に QR 生成や PNG 加工のロジックを書く。
- 一度に QR options、UI、配布設定、CI まで広げる。
- public API の名前を実装都合で決める。
- `ByteArray?` のような nullable 戻り値で失敗原因を曖昧にする。
- Rust の `unwrap` で不正入力時に panic させる。
- docs の API 例と実装の API をずらす。

## 変更後の確認

- Kotlin を変更したら Gradle の該当 task を実行する。
- Rust を変更したら Cargo の該当 test を実行する。
- JNI を変更したら Android 側から native library load と呼び出しを確認する。
- docs だけを変更した場合も、Git 差分でコード変更が混ざっていないことを確認する。
