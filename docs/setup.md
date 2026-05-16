# QrForge 開発環境セットアップ

## 前提ツール

| ツール | 必要バージョン | 備考 |
|--------|--------------|------|
| Android Studio | 最新安定版 | |
| Android NDK | r27 以降 | SDK Manager でインストール |
| Rust toolchain | stable | `rustup` で管理 |
| Rust ターゲット | `aarch64-linux-android` | `rustup target add aarch64-linux-android` |
| cargo-ndk | 最新版 | `cargo install cargo-ndk` |

## Rust ターゲット追加

```bash
rustup target add aarch64-linux-android
```

## Rust テスト

```bash
# qrforge-core の単体テスト
cargo test --manifest-path rust/qrforge-core/Cargo.toml

# qrforge-jni のビルド確認（テストは Android 上）
cargo build --manifest-path rust/qrforge-jni/Cargo.toml
```

## native library のビルド（arm64-v8a）

```bash
cargo ndk -t arm64-v8a -o qrforge/src/main/jniLibs build --release --manifest-path rust/qrforge-jni/Cargo.toml
```

ビルド後の `.so` は `qrforge/src/main/jniLibs/arm64-v8a/libqrforge.so` に生成される。

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

- `arm64-v8a`（実機・エミュレーター arm64）

x86_64（エミュレーター）対応は Phase 4 以降で追加する。追加時は `rustup target add x86_64-linux-android` が必要。
