# ビルド・検証手順

この文書は「何をインストールし、何を変更したらどのコマンドを実行するか」だけを扱う。レイヤ構成は [architecture.md](architecture.md)、実装ルールは [coding-rules.md](coding-rules.md)、テストの置き場所と方針は [unit-test.md](unit-test.md) を参照する。

## 前提ツール

| ツール | 条件 | 備考 |
|--------|------|------|
| Android Studio | 最新安定版 | `compileSdk 36.1` / `minSdk 28` をビルドできること |
| JDK | 17 | Gradle build daemon・CI は Adoptium 17 を使用する |
| Android NDK | r27 以降 | `ANDROID_NDK_HOME` / `ANDROID_NDK_ROOT` を設定する |
| Rust toolchain | stable | `rustfmt`・`clippy` component 込み |
| cargo-ndk | 任意 | `cargo install cargo-ndk` |
| Python | 3.10 以降 | repository 整合性 checker と skill 同期に使用 |

Rust target を追加する。

```powershell
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add x86_64-linux-android
```

32-bit x86 emulator を対象にする場合のみ `rustup target add i686-linux-android` を追加する。

## 実行環境の禁止事項

この 2 点は環境を壊すため、例外なく守る。

- **cargo は PowerShell から実行する。** POSIX shell 経由だと Git 同梱の GNU `link` を MSVC の `link.exe` より先に解決し、リンクに失敗する。
- **`cargo clean` を実行しない。** host 用ビルドスクリプトのキャッシュが消え、MSVC Build Tools が無い環境では以後 `cargo build`・`cargo clippy`・`cargo ndk` がすべて失敗し、復旧できない。

## Rust の検証コマンド

```powershell
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace --all-targets
cargo build --manifest-path rust/qrforge-jni/Cargo.toml
```

`clippy` は CI で `-D warnings` 付きで強制されるため、warning が 1 件でも残っていると落ちる。ローカルでも必ず通す。

## native library のビルド

`ANDROID_NDK_HOME` / `ANDROID_NDK_ROOT` を設定したうえで実行する。

```powershell
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o qrforge/src/main/jniLibs build --release --manifest-path rust/qrforge-jni/Cargo.toml
```

出力先は Android library module 配下で、repository にコミットする。

```text
qrforge/src/main/jniLibs/arm64-v8a/libqrforge.so
qrforge/src/main/jniLibs/armeabi-v7a/libqrforge.so
qrforge/src/main/jniLibs/x86_64/libqrforge.so
```

既定 ABI は `arm64-v8a`・`armeabi-v7a`・`x86_64` の 3 つ。`x86` は 32-bit x86 emulator が必要になった場合のみ `-t x86` を足して追加する。sample app 側の `app/src/main/jniLibs` には配置しない。

## `.so` の鮮度（最も踏みやすい落とし穴）

`rust/` 配下を変更した時点で、`qrforge/src/main/jniLibs/<abi>/libqrforge.so` は**変更前のビルドのまま**になる。Gradle は `.so` を自動生成しないため、この状態は静かに見逃される。

- 再ビルドせずに `connectedDebugAndroidTest` を通しても、検証しているのは変更前の native library であり、変更内容は一切確認できていない。
- 再ビルド後は差分で 3 ABI すべてが更新されたことを確認する。

```powershell
git diff --stat qrforge/src/main/jniLibs
```

- 再ビルドしていない場合は、Android 側のテストが緑でも「native 側は未検証」として報告する。

CI の `rust` job は現在の source から 3 ABI を一時出力へビルドし、`instrumented` job は repository に
コミット済みの `x86_64` `.so` を使う。両方が通っても、source とコミット済み `.so` の一致は
証明されない。Rust 変更時はローカルで 3 ABI を再生成し、コミット対象の差分を確認する。

## Android の検証コマンド

```powershell
.\gradlew.bat :qrforge:assembleDebug
.\gradlew.bat :qrforge:testDebugUnitTest
.\gradlew.bat :qrforge:assembleDebugAndroidTest
.\gradlew.bat :qrforge:connectedDebugAndroidTest
.\gradlew.bat :app:assembleDebug
```

| タスク | 内容 |
|--------|------|
| `:qrforge:assembleDebug` | library module のビルド（AAR への `.so` 同梱を含む） |
| `:qrforge:testDebugUnitTest` | JVM 上の UnitTest（JUnit5） |
| `:qrforge:assembleDebugAndroidTest` | instrumented test の APK ビルドのみ。端末不要 |
| `:qrforge:connectedDebugAndroidTest` | 実機・エミュレーター上で instrumented test 実行 |
| `:app:assembleDebug` | sample app のビルド |

`connectedDebugAndroidTest` は実機またはエミュレーターが必須。接続先が無い場合は黙って飛ばさず、「未実行」と理由を報告する。

## instrumented test のクラス絞り込み

`--tests` は `connectedDebugAndroidTest` では効かない。instrumentation runner の引数を使い、PowerShell では引数全体を quote する。

```powershell
.\gradlew.bat :qrforge:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.appvoyager.qrforge.QrForgeInstrumentedTest"
```

## 変更後の確認フロー

変更したレイヤーに対応する行をすべて実行する。複数レイヤーに跨る変更は該当行を積み上げる。

| 変更箇所 | 実行コマンド |
|---------|------------|
| Rust core（`rust/qrforge-core/`） | `cargo fmt --all -- --check`、`cargo clippy --workspace --all-targets -- -D warnings`、`cargo test --workspace --all-targets` |
| Rust JNI bridge（`rust/qrforge-jni/`） | 上記に加えて `cargo build --manifest-path rust/qrforge-jni/Cargo.toml` |
| `rust/` を変更して Android 側も確認する | 上記に加えて `.so` 再ビルドと `git diff --stat qrforge/src/main/jniLibs`（「`.so` の鮮度」参照） |
| Kotlin wrapper（`qrforge/`） | `.\gradlew.bat :qrforge:assembleDebug`、`.\gradlew.bat :qrforge:testDebugUnitTest` |
| Instrumented test | `.\gradlew.bat :qrforge:assembleDebugAndroidTest`、可能なら `.\gradlew.bat :qrforge:connectedDebugAndroidTest` |
| Sample app（`app/`） | `.\gradlew.bat :app:assembleDebug` |
| ABI 追加・削除 | `.so` 再ビルド、`abiFilters`、README・本文書・CI をまとめて更新 |
| docs のみ | `git diff --stat` でコード変更が混ざっていないことを確認 |

## repository の整合性確認

`.agents/skills/` が skill 本文の正典。更新後は Claude Code 用の copy を同期し、repository 全体の
整合性を確認する。

```powershell
python scripts/sync_skills.py --sync
python scripts/check_repo_consistency.py
```

この文書では Python 3 の実行名を `python` と表記する。環境で `python3` として導入されている
場合は読み替える（CI は `python3` を使用する）。

整合性確認は Markdown の相対リンク、Codex / Claude Code の skill 本文、`abiFilters` と
`qrforge/src/main/jniLibs/` の ABI directory を検証する。CI でも同じ checker を実行する。

## CI ジョブとローカルコマンドの対応

workflow は `.github/workflows/ci.yml`。ローカルで先に潰しておくべき対応は次のとおり。

| CI job | ローカルで相当するコマンド |
|--------|--------------------------|
| `consistency` | `python scripts/check_repo_consistency.py` |
| `rust` | `cargo fmt --all -- --check`、`cargo clippy --workspace --all-targets -- -D warnings`、`cargo test --workspace --all-targets`、`cargo build --manifest-path rust/qrforge-jni/Cargo.toml`、`cargo ndk`（3 ABI の `.so` 生成確認まで） |
| `android` | `.\gradlew.bat :qrforge:testDebugUnitTest :qrforge:assembleDebug :qrforge:assembleDebugAndroidTest :app:assembleDebug` |
| `instrumented` | repository にコミット済みの `x86_64` `.so` を使う `.\gradlew.bat :qrforge:connectedDebugAndroidTest`（CI は API 34 emulator） |

`rust` job と `instrumented` job は別の成果物を検証する。コミット済みの `.so` が古いままでも
両方が通り得るため、ローカルでの再ビルドと差分確認を省略しない。
