# QrForge

QrForge は、Android/Kotlin から SDK 風に呼び出せる Rust 製 QR コード生成ライブラリです。

利用者向け API は Kotlin の `QrGenerator` に集約し、Rust・JNI・NDK の詳細はライブラリ内部に閉じ込めます。現在は Android library module `:qrforge` と、動作確認用の sample app `:app` を持つ構成です。

## できること

- 文字列から QR コード PNG bytes を生成する
- QR コードを Android `Bitmap` として生成する
- `QrOptions(size, margin)` で画像サイズと余白を指定する
- 入力不正、native library load failure、QR 生成失敗、PNG decode 失敗を区別する
- Rust core を Android/JNI から独立してテストする

## モジュール構成

```text
QrForge/
├── app/                 # :qrforge を利用する sample app
├── qrforge/             # Android library module
│   ├── QrGenerator           # Kotlin public API
│   ├── QrOptions             # public option model
│   ├── QrGenerationException # public exception type
│   └── internal/        # JNI binding
└── rust/
    ├── qrforge-core/    # QR 生成と PNG エンコード
    └── qrforge-jni/     # Rust 側 JNI bridge
```

依存方向は `app -> qrforge -> JNI bridge -> Rust core` です。`app` や外部利用者から JNI API を直接呼び出さない設計にしています。

## 使い方

### Bitmap を生成する

```kotlin
val bitmap = QrGenerator.createBitmap("https://example.com")
imageView.setImageBitmap(bitmap)
```

### PNG bytes を生成する

```kotlin
val pngBytes = QrGenerator.createPngBytes("Hello QR")
```

### オプションを指定する

```kotlin
val bitmap = QrGenerator.createBitmap(
    text = "https://example.com",
    options = QrOptions(size = 768, margin = 6),
)
```

`QrOptions.size` は `1..4096`、`QrOptions.margin` は `0..64` を受け付けます。範囲外の値や blank text は `IllegalArgumentException` で拒否します。

## 例外

| ケース | 例外 |
|--------|------|
| blank text / options 範囲外 | `IllegalArgumentException` |
| native library がロードできない | `QrGenerationException.NativeLibraryUnavailable` |
| QR 生成に失敗 | `QrGenerationException.GenerationFailed` |
| PNG decode に失敗 / Bitmap 確保がメモリ上限超過・OOM | `QrGenerationException.DecodeFailed` |

## 対応 ABI

同梱している native library は次の ABI に対応します。

| ABI | 用途 |
|-----|------|
| `arm64-v8a` | 64-bit ARM 実機、arm64 emulator |
| `armeabi-v7a` | 32-bit ARM 実機 |
| `x86_64` | 64-bit x86 emulator |

```text
qrforge/src/main/jniLibs/arm64-v8a/libqrforge.so
qrforge/src/main/jniLibs/armeabi-v7a/libqrforge.so
qrforge/src/main/jniLibs/x86_64/libqrforge.so
```

`x86` は 32-bit x86 emulator 向けです。現在の実機・エミュレーター確認用途は `arm64-v8a`、`armeabi-v7a`、`x86_64` で満たせるため既定の同梱対象には含めていません。必要になった場合のみ `i686-linux-android` target と `x86` ABI を追加してください。

Android 側は `qrforge` library module に `.so` を同梱し、SDK 内部で端末 ABI に合う
`libqrforge.so` を読み込みます。sample app 側の `app/src/main/jniLibs` には配置しません。

## 開発環境

必要なツールは [docs/setup.md](docs/setup.md) を参照してください。整合性確認には Python 3.10
以降を使用します。実行名が `python3` の環境では、以下の `python` を読み替えてください。

主な確認コマンド:

```powershell
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add x86_64-linux-android
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace --all-targets
cargo build --manifest-path rust/qrforge-jni/Cargo.toml
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o qrforge/src/main/jniLibs build --release --manifest-path rust/qrforge-jni/Cargo.toml
.\gradlew.bat :qrforge:testDebugUnitTest
.\gradlew.bat :qrforge:assembleDebug
.\gradlew.bat :app:assembleDebug
python scripts/check_repo_consistency.py
```

32-bit x86 emulator も対象にする場合は、追加で次を実行し、`cargo ndk` に `-t x86` を加えます。

```powershell
rustup target add i686-linux-android
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 -o qrforge/src/main/jniLibs build --release --manifest-path rust/qrforge-jni/Cargo.toml
```

`connectedDebugAndroidTest` は実機またはエミュレーターが必要です。

## CI

GitHub Actions で 4 つの job を実行します。workflow は `.github/workflows/ci.yml` にあります。

| Job | 内容 |
|-----|------|
| `consistency` | 文書リンク、Codex / Claude Code の skill 本文、ABI 設定の整合性確認 |
| `rust` | format check、clippy (`-D warnings`)、workspace test、JNI crate build、3 ABI の native library build |
| `android` | library unit test、library debug build、androidTest APK build、sample app debug build |
| `instrumented` | repository にコミット済みの `x86_64` native library を使い、API 34 emulator 上で instrumented test を実行 |

`rust` job は現在の Rust source から 3 ABI を一時出力へビルドできることを確認する。一方、
`instrumented` job は配布対象としてコミットされた `x86_64` の `.so` を確認する。両方が通っても
source とコミット済み `.so` の一致までは証明しないため、Rust 変更時はローカルで 3 ABI を再生成し、
差分を確認する。

ローカルで対応するコマンドは [docs/setup.md](docs/setup.md) の「CI ジョブとローカルコマンドの対応」を参照してください。

## 文書

| 文書 | 内容 |
|------|------|
| [docs/architecture.md](docs/architecture.md) | レイヤ構成・依存方向・生成フロー |
| [docs/api-design.md](docs/api-design.md) | 公開 API の契約・例外分類・値域定数 |
| [docs/coding-rules.md](docs/coding-rules.md) | Kotlin・Rust・JNI の実装ルール |
| [docs/unit-test.md](docs/unit-test.md) | テストの置き場所・境界・書き方 |
| [docs/setup.md](docs/setup.md) | ビルド・確認コマンド・CI との対応 |
| [docs/review-rules.md](docs/review-rules.md) | レビュー分類・見る順序・出力フォーマット |

## 公開状態

QrForge は Android library module として利用できる構成ですが、Maven Central などへの公開設定はまだありません。現時点では、この repository 内の `:qrforge` module を sample app から参照して検証しています。
