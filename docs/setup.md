# QrForge 開発環境セットアップ

## 前提ツール

| ツール | 必要バージョン | 備考 |
|--------|--------------|------|
| Android Studio | 最新安定版 | |
| Android NDK | r27 以降 | SDK Manager でインストール |
| Rust toolchain | stable | `rustup` で管理 |
| Rust ターゲット | `aarch64-linux-android`, `armv7-linux-androideabi`, `x86_64-linux-android` | 対応 ABI 分を `rustup target add` で追加 |
| cargo-ndk | 最新版 | `cargo install cargo-ndk` |

## Rust ターゲット追加

```bash
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add x86_64-linux-android
```

## Rust テスト

```bash
# qrforge-core の単体テスト
cargo test --manifest-path rust/qrforge-core/Cargo.toml

# qrforge-jni のビルド確認（テストは Android 上）
cargo build --manifest-path rust/qrforge-jni/Cargo.toml
```

## native library のビルド

```bash
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o qrforge/src/main/jniLibs build --release --manifest-path rust/qrforge-jni/Cargo.toml
```

ビルド後の `.so` は次の場所に生成される。

```text
qrforge/src/main/jniLibs/arm64-v8a/libqrforge.so
qrforge/src/main/jniLibs/armeabi-v7a/libqrforge.so
qrforge/src/main/jniLibs/x86_64/libqrforge.so
```

`x86` は 32-bit x86 emulator が必要な場合のみ追加する。対応する場合は `rustup target add i686-linux-android` を実行し、`cargo ndk` に `-t x86` を加える。

## Android ビルド・テスト

```bash
# library debug ビルド
./gradlew :qrforge:assembleDebug

# library unit test（JVM 上）
./gradlew :qrforge:testDebugUnitTest

# sample app は :qrforge を利用する検証用アプリ
./gradlew :app:assembleDebug

# instrumented test（実機またはエミュレーター必要）
./gradlew :qrforge:connectedDebugAndroidTest
```

## 変更後の確認フロー

| 変更箇所 | 実行コマンド |
|---------|------------|
| Rust core | `cargo test --manifest-path rust/qrforge-core/Cargo.toml` |
| Rust JNI | `cargo build --manifest-path rust/qrforge-jni/Cargo.toml` + .so 再ビルド |
| Kotlin wrapper | `./gradlew :qrforge:assembleDebug` + `./gradlew :qrforge:testDebugUnitTest` |
| Sample app | `./gradlew :app:assembleDebug` |
| docs のみ | `git diff --stat` でコード変更が混ざっていないことを確認 |

## 現在対応 ABI

- `arm64-v8a`（64-bit ARM 実機、arm64 エミュレーター）
- `armeabi-v7a`（32-bit ARM 実機）
- `x86_64`（64-bit x86 エミュレーター）

`x86` は既定の対応 ABI には含めない。32-bit x86 emulator が必要になった場合だけ `i686-linux-android` target と `x86` ABI を追加する。
