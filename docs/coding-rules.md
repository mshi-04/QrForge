# QrForge コーディングルール

この文書は、実装時に迷いやすい禁止事項と責務境界だけをまとめる。
詳しい設計は [architecture.md](architecture.md)、公開 API は [api-design.md](api-design.md)、テスト方針は [unit-test.md](unit-test.md)、確認コマンドは [setup.md](setup.md) を参照する。

## 実装前に確認すること

- 対象範囲、変更予定ファイル、確認コマンド、やらないことを先に整理する。
- Rust core・JNI bridge・Kotlin wrapper・sample app のどこを触る作業か明確にする。
- 複数レイヤーをまたぐ場合も、各レイヤーの責務を混ぜない。

## レイヤ別ルール

### Kotlin public API

- 利用者向け入口は `QrForge` に集約する。
- public API は目的ベースの名前にし、JNI や Rust の都合を名前へ出さない。
- 既存 API の意味を変えず、機能追加は overload や `QrOptions` で行う。
- `QrForgeNative` や `System.loadLibrary` は公開 API から隠す。
- PNG bytes から `Bitmap` への変換と利用者向け例外への変換は Kotlin wrapper の責務にする。

### Rust core

- Android、JNI、Kotlin に依存しない。
- QR 生成、PNG エンコード、options の正規バリデーションを担当する。
- JNI 向けの関数名、JNI 型、Android 固有のエラー表現を入れない。
- core logic で `unwrap` / `expect` を使わない。

### JNI bridge

- Kotlin と Rust core の型変換、エラー変換だけを担当する。
- QR 生成ロジックや PNG 加工ロジックを書かない。
- native binding は `internal` に閉じる。
- Rust panic や不正な JVM state を JNI 境界からそのまま漏らさない。

### Sample app

- SDK の利用例と動作確認用 UI に留める。
- QR 生成ロジックや JNI 直接呼び出しを置かない。
- app からは `QrForge` wrapper 経由で呼び出す。

### Android ABI / native library

- `libqrforge.so` は `qrforge/src/main/jniLibs/<abi>/` に配置する。sample app の `app/src/main/jniLibs` には配置しない。
- 既定対応 ABI は `arm64-v8a`、`armeabi-v7a`、`x86_64` とする。
- `x86` は 32-bit x86 emulator が必要な場合だけ追加する。
- ABI を増減させる場合は、Rust target、`cargo ndk` の `-t`、Android `abiFilters`、README、setup 手順、CI の native build を同時に見直す。
- ABI 追加を理由に Kotlin public API、JNI method signature、Rust core API を変更しない。

## API とエラー

- 入力不正は `IllegalArgumentException` として扱う。
- native library load failure、QR 生成失敗、PNG decode failure は `QrForgeException` で分類する。
- `null`、空配列、nullable 戻り値で失敗原因を表現しない。
- `QrOptions` の定数を変える場合は Kotlin (`QrOptions.kt`) と Rust core (`qrforge-core/src/lib.rs`) を同時に見直す。

## 命名

| 対象 | 名前 |
|------|------|
| 公開入口 | `QrForge` |
| option model | `QrOptions` |
| SDK 例外 | `QrForgeException` |
| 内部 native binding | `QrForgeNative` |
| Rust core crate | `qrforge-core` |
| Rust JNI crate | `qrforge-jni` |

避ける名前:

- `NativeLib`
- `JniHelper`
- public API としての `RustBridge`
- `generate` だけの曖昧な public 関数

## 変更後の確認

- UnitTest の作成ルールは [unit-test.md](unit-test.md) を参照する。
- 実行コマンドは [setup.md](setup.md) の「変更後の確認フロー」に従う。
- docs だけを変更した場合も、Git 差分でコード変更が混ざっていないことを確認する。
