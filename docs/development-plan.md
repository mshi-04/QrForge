# QrForge 開発計画

## 前提

この計画は、Android/Kotlin から SDK 風に呼び出せる Rust 製 QR コード生成ライブラリを段階的に作るためのもの。各 Phase では、責務境界を壊さないこと、実装範囲を広げすぎないこと、確認可能な完了条件を置くことを重視する。

実装に入る前に、対象 Phase、変更ファイル、確認コマンド、やらないことを明示する。

## Phase 1: ドキュメントと設計境界の固定 ✅ 完了

### ゴール

AI が実装時に迷わないよう、リポジトリの目的、責務境界、公開 API 方針、実装順序を文書化する。

### 完了条件（達成済み）

- `AGENTS.md` が薄い入口文書になっている。
- 詳細ルールが `docs/` 配下に分離されている。
- Android app、SDK wrapper、JNI bridge、Rust core の責務が説明されている。
- 最初に提供する Kotlin public API が明記されている。
- Phase 2 以降の実装順序が決まっている。

## Phase 2: Rust core の最小実装 ✅ 完了

### ゴール

Rust 単体で、文字列から QR コード PNG bytes を生成できる core library を作る。

### 完了条件（達成済み）

- `rust/qrforge-core/` に `generate_png` 相当の関数がある。
- Rust 単体テスト（`tests/generate_qr_png.rs`）が通る。
- core に JNI、Android、Kotlin の依存がない。
- 使用 crate: `qrcode 0.14.1` + `image 0.25.10`（PNG feature）

## Phase 3: JNI bridge の最小接続 ✅ 完了

### ゴール

Kotlin から native method を経由して Rust core を呼び出し、PNG `ByteArray` を取得できるようにする。JNI 関数は内部 API として扱う。

### 主な作成・変更ファイル

- `rust/qrforge-jni/Cargo.toml` ✅
- `rust/qrforge-jni/src/lib.rs` ✅
- `app/src/main/java/.../internal/QrForgeNative.kt` ✅
- `app/src/main/jniLibs/arm64-v8a/libqrforge.so` ✅
- `app/build.gradle.kts`（NDK 連携設定）
- native library load を SDK wrapper 内部に隠蔽する Kotlin コード ✅

### 完了条件

- Kotlin internal API から native method を呼べる。
- Rust core の PNG bytes が Kotlin `ByteArray` として返る。
- native library のロード位置が SDK wrapper 内部に隠蔽されている。
- JNI 例外または error code の方針が決まっている。
- Android app が JNI 関数を直接呼ばない。

### テスト観点

- native library が対象 ABI でロードできる。
- Kotlin から正常文字列で PNG bytes を取得できる。
- Rust 側エラーが Kotlin 側の例外に変換される。
- null や不正な JNI state を安全に扱える。
- 複数回呼び出してクラッシュしない。

### この Phase でやらないこと

- UI から直接表示しない。
- `Bitmap` 変換の公開 API を完成扱いにしない。
- 複雑な options を JNI に通さない。
- JNI 関数を public package に置かない。

### 次 Phase へ進む判断基準

- Kotlin internal API で PNG bytes が安定して取得できる。
- JNI bridge に QR 生成ロジックが混ざっていない。
- Rust core のテストと Android 側確認を分けて実行できる。

---

## Phase 4: Kotlin SDK wrapper と Android 表示 ✅ 完了

### ゴール

Android 利用者向けの SDK 風 API を提供し、`QrForge.createBitmap(text)` と `QrForge.createPngBytes(text)` を使って表示できるようにする。

### 主な作成・変更ファイル

- `app/src/main/java/.../QrForge.kt` ✅
- `app/src/main/java/.../QrForgeException.kt` ✅
- `app/src/main/java/.../QrOptions.kt`（Phase 5 で追加）✅
- `app/src/main/java/.../MainActivity.kt` ✅
- `app/src/test/java/.../QrForgeTest.kt` ✅
- `app/src/androidTest/java/.../QrForgeInstrumentedTest.kt` ✅

### 完了条件（達成済み）

- Android から SDK wrapper だけを使って QR 表示できる。
- JNI の存在が app 利用コードに漏れていない。
- `QrForge.createBitmap(text)` と `QrForge.createPngBytes(text)` が利用可能。
- 例外体系（`QrForgeException` sealed class）で障害原因を区別できる。
- Unit テストと Instrumented テストで基本動作を確認済み。

## Phase 5: QrOptions とエラー設計 ✅ 完了

### ゴール

既存 API の互換性を保ったまま、`QrOptions(size, margin)` で QR 画像サイズと余白を指定できる API を追加する。入力不正と native 側失敗を区別し、Android SDK として扱いやすい例外設計に整理する。

### 主な作成・変更ファイル

- `app/src/main/java/.../QrOptions.kt` ✅
- `app/src/main/java/.../QrForge.kt` ✅
- `app/src/main/java/.../internal/QrForgeNative.kt` ✅
- `rust/qrforge-core/src/lib.rs` ✅
- `rust/qrforge-jni/src/lib.rs` ✅
- `app/src/main/jniLibs/arm64-v8a/libqrforge.so` ✅

### 完了条件（達成済み）

- `QrForge.createBitmap(text)` と `QrForge.createPngBytes(text)` の既存 API が残っている。
- `QrForge.createBitmap(text, options)` と `QrForge.createPngBytes(text, options)` が利用可能。
- `QrOptions.size` は `1..4096`、`QrOptions.margin` は `0..64` を受け付ける。
- 入力不正は `IllegalArgumentException`、native 側失敗や PNG decode 失敗は `QrForgeException` として扱う。
- Rust core が size / margin を受け取り、JNI bridge は型変換とエラー変換に留まる。

## Phase 6: ライブラリ化と公開準備

### ゴール

QrForge を Android ライブラリとして切り出しやすい構成に整理する。既存 `:app` を sample app として残すか新規 library module を作るかは Phase 6 の計画時に決める。

### やらないこと

- Maven Central などへの公開を急がない。
- QR 以外のバーコード生成へ広げない。
- API 安定前に過剰な設定項目を増やさない。

## Phase 横断の確認方針

具体的なコマンドは [docs/setup.md](docs/setup.md) を参照。

- Kotlin 変更時: `./gradlew :app:assembleDebug` と `./gradlew :app:testDebugUnitTest`
- Rust 変更時: `cargo test --manifest-path rust/qrforge-core/Cargo.toml`
- JNI 変更時: Android 側から native library load と呼び出しを確認する。
- ドキュメント変更時はコード変更が混ざっていないことを Git 差分で確認する。
- 確認できなかった項目は理由を明記する。
