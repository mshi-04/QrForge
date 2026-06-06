# QrForge コーディングルール

実装時に迷いやすい責務境界と禁止事項をまとめる。詳細な設計は [architecture.md](architecture.md)、API は [api-design.md](api-design.md)、確認コマンドは [setup.md](setup.md) を参照する。

## 実装前チェック

- 対象範囲、変更予定ファイル、確認コマンド、やらないことを先に整理する。
- Rust core、JNI bridge、Kotlin wrapper、sample app のどこを触るか明確にする。
- 複数レイヤーを触る場合も、各レイヤーの責務を混ぜない。

## レイヤ別ルール

| レイヤ | やること | やらないこと |
|--------|----------|--------------|
| Kotlin public API | `QrForge`、`QrOptions`、`QrForgeException` を利用者向け入口にする | JNI / Rust の都合を名前や利用例に出す |
| Rust core | QR 生成、PNG エンコード、option validation | Android / JNI / Kotlin に依存する |
| JNI bridge | 型変換、エラー変換、native binding | QR 生成ロジックや PNG 加工を書く |
| Sample app | SDK 利用例と動作確認 UI | JNI 直接呼び出しやライブラリ内部処理 |

## API / エラー

- 入力不正は `IllegalArgumentException`。
- native library load failure、生成失敗、PNG decode failure は `QrForgeException` で分類する。
- `null`、空配列、nullable 戻り値で失敗を表現しない。
- `QrOptions` の定数変更時は Kotlin と Rust core を同時に更新する。

## Native library

- `libqrforge.so` は `qrforge/src/main/jniLibs/<abi>/` に置く。
- `app/src/main/jniLibs` には置かない。
- 既定 ABI は `arm64-v8a`、`armeabi-v7a`、`x86_64`。
- `x86` は 32-bit x86 emulator が必要な場合だけ追加する。
- ABI 変更時は Rust target、`cargo ndk`、Android `abiFilters`、README、setup、CI を同時に確認する。

## 命名

| 対象 | 名前 |
|------|------|
| 公開入口 | `QrForge` |
| option model | `QrOptions` |
| SDK 例外 | `QrForgeException` |
| 内部 native binding | `QrForgeNative` |
| Rust core crate | `qrforge-core` |
| Rust JNI crate | `qrforge-jni` |

避ける名前: `NativeLib`、`JniHelper`、public API としての `RustBridge`、曖昧な `generate`。

## 変更後

- テスト方針は [unit-test.md](unit-test.md) に従う。
- 確認コマンドは [setup.md](setup.md) の「変更後の確認フロー」に従う。
- docs のみ変更でも、コード変更が混ざっていないことを Git 差分で確認する。
