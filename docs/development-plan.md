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

## Phase 4: Kotlin SDK wrapper と Android 表示 🔄 進行中

### ゴール

Android 利用者向けの SDK 風 API を提供し、`QrForge.createBitmap(text)` と `QrForge.createPngBytes(text)` を使って表示できるようにする。

### 主な作成・変更ファイル（候補）

- `app/src/main/java/.../QrForge.kt`
- `app/src/main/java/.../QrForgeException.kt`
- `app/src/main/java/.../QrOptions.kt`（Phase 4 以降で必要になった時点で追加）
- `app/src/main/java/.../MainActivity.kt`
- `app/src/test/java/.../QrForgeTest.kt`

### やらないこと

- QR options の全機能を完成させない。
- ライブラリ配布設定を完成させない。
- UI を過度に作り込まない。

### 次 Phase へ進む判断基準

- Android から SDK wrapper だけを使って QR 表示できる。
- JNI の存在が app 利用コードに漏れていない。

## Phase 5: ライブラリ化と拡張準備

> Phase 4 完了後に詳細化する。

### ゴール

QrForge を Android ライブラリとして切り出しやすい構成に整理し、`QrOptions` などの拡張を安全に追加できる状態にする。既存 `:app` を sample app として残すか新規 library module を作るかは Phase 5 の計画時に決める。

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
