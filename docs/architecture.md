# QrForge アーキテクチャ

QrForge は、Android/Kotlin から SDK 風に呼び出せる Rust 製 QR コード生成ライブラリ。
利用者には Kotlin public API だけを見せ、Rust / JNI / NDK の詳細は内部へ閉じる。

## レイヤ構成

```text
Android app
  -> Kotlin SDK wrapper
  -> JNI bridge
  -> Rust core
```

依存方向は上から下だけ。Rust core は Android、JNI、Kotlin、UI を知らない。

## ディレクトリ

| レイヤ | 配置 | 責務 |
|--------|------|------|
| Android app | `app/` | sample UI、SDK 利用例、動作確認 |
| Kotlin wrapper | `qrforge/src/main/java/com/appvoyager/qrforge/` | public API、入力検証、Bitmap 変換、利用者向け例外 |
| Kotlin JNI binding | `qrforge/src/main/java/com/appvoyager/qrforge/internal/` | internal native binding |
| Rust JNI bridge | `rust/qrforge-jni/` | JNI symbol、型変換、エラー変換 |
| Rust core | `rust/qrforge-core/` | QR 生成、PNG エンコード、option validation |
| Native library | `qrforge/src/main/jniLibs/<abi>/libqrforge.so` | Android library module に同梱 |

## 公開 API

公開 API は Kotlin wrapper に限定する。

- `QrForge`
- `QrOptions`
- `QrForgeException`

公開しないもの:

- `QrForgeNative`
- `external fun`
- Rust exported JNI symbol
- `System.loadLibrary("qrforge")`
- Rust crate の内部型や PNG エンコード実装

## 境界ルール

- Android app に QR 生成ロジックや JNI 直接呼び出しを置かない。
- Kotlin wrapper に Rust / JNI の都合を漏らさない。
- JNI bridge に QR 生成ロジックや PNG 加工ロジックを置かない。
- Rust core に Android / JNI / Kotlin 依存を入れない。
- public API の変更は互換性を優先し、既存 API の意味を変えない。

## 拡張方針

- 追加機能は Kotlin public API、JNI bridge、Rust core の責務を先に分ける。
- 新しい設定は既存 API の破壊ではなく overload や `QrOptions` で追加する。
- エラー訂正レベル、色、背景色、画像形式などは機能単位を分ける。
- ABI を増減する場合は `docs/setup.md`、README、CI、Android 設定も同時に確認する。
