# QrForge 開発環境セットアップ

## 前提ツール

| ツール | 備考 |
|--------|------|
| Android Studio | 最新安定版 |
| Android NDK | r27 以降 |
| Rust toolchain | stable |
| cargo-ndk | `cargo install cargo-ndk` |

## Rust target

```powershell
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add x86_64-linux-android
```

`x86` を追加する場合のみ `rustup target add i686-linux-android` も実行する。

## Rust

```powershell
cargo fmt --all -- --check
cargo test --workspace --all-targets
cargo build --manifest-path rust/qrforge-jni/Cargo.toml
```

## Native library build

```powershell
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o qrforge/src/main/jniLibs build --release --manifest-path rust/qrforge-jni/Cargo.toml
```

出力先:

```text
qrforge/src/main/jniLibs/arm64-v8a/libqrforge.so
qrforge/src/main/jniLibs/armeabi-v7a/libqrforge.so
qrforge/src/main/jniLibs/x86_64/libqrforge.so
```

## Android

```powershell
.\gradlew.bat :qrforge:assembleDebug
.\gradlew.bat :qrforge:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :qrforge:assembleDebugAndroidTest
.\gradlew.bat :qrforge:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` は実機またはエミュレーターが必要。

## 変更後の確認フロー

| 変更箇所 | 実行コマンド |
|---------|------------|
| Rust core | `cargo fmt --all -- --check`、`cargo test --workspace --all-targets` |
| Rust JNI | `cargo build --manifest-path rust/qrforge-jni/Cargo.toml`、必要なら `.so` 再ビルド |
| Kotlin wrapper | `.\gradlew.bat :qrforge:assembleDebug`、`.\gradlew.bat :qrforge:testDebugUnitTest` |
| Instrumented test | `.\gradlew.bat :qrforge:assembleDebugAndroidTest`、可能なら `.\gradlew.bat :qrforge:connectedDebugAndroidTest` |
| Sample app | `.\gradlew.bat :app:assembleDebug` |
| docs のみ | `git diff --stat` でコード変更が混ざっていないことを確認 |

## ABI

既定対応 ABI は `arm64-v8a`、`armeabi-v7a`、`x86_64`。`x86` は必要になった場合だけ追加する。
