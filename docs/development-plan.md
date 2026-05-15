# QrForge 開発計画

## 前提

この計画は、Android/Kotlin から SDK 風に呼び出せる Rust 製 QR コード生成ライブラリを段階的に作るためのもの。各 Phase では、責務境界を壊さないこと、実装範囲を広げすぎないこと、確認可能な完了条件を置くことを重視する。

実装に入る前に、対象 Phase、変更ファイル、確認コマンド、やらないことを明示する。

## Phase 1: ドキュメントと設計境界の固定

### ゴール

AI が実装時に迷わないよう、リポジトリの目的、責務境界、公開 API 方針、実装順序を文書化する。

### 主な作成・変更ファイル

- `AGENTS.md`
- `docs/architecture.md`
- `docs/development-plan.md`
- `docs/coding-rules.md`
- `docs/api-design.md`

### 完了条件

- `AGENTS.md` が薄い入口文書になっている。
- 詳細ルールが `docs/` 配下に分離されている。
- Android app、SDK wrapper、JNI bridge、Rust core の責務が説明されている。
- 最初に提供する Kotlin public API が明記されている。
- Phase 2 以降の実装順序が決まっている。

### テスト観点

- 実装コードは変更しないため、ビルドテストは必須ではない。
- Markdown の存在確認と内容確認を行う。
- Git 差分でコード変更が混ざっていないことを確認する。

### この Phase でやらないこと

- Kotlin 実装を書かない。
- Rust crate を追加しない。
- JNI 関数を追加しない。
- Gradle、CMake、Cargo の設定を変更しない。

### 次 Phase へ進む判断基準

- 文書で責務境界が説明できる。
- 初期 API の形に合意できる。
- Rust core と JNI bridge の分離方針に迷いがない。

## Phase 2: Rust core の最小実装

### ゴール

Rust 単体で、文字列から QR コード PNG bytes を生成できる core library を作る。JNI や Android UI にはまだ接続しない。

### 主な作成・変更ファイル

候補:

- `rust/qrforge-core/Cargo.toml`
- `rust/qrforge-core/src/lib.rs`
- `rust/qrforge-core/src/error.rs`
- `rust/qrforge-core/src/options.rs`
- `rust/qrforge-core/tests/generate_png.rs`

実際の配置は、Android/Gradle/NDK 連携方針を確認してから決める。

### 完了条件

- Rust API で `generate_png(text)` 相当の関数を呼び出せる。
- 空文字や不正入力の扱いが決まっている。
- PNG signature を持つ bytes が生成される。
- Rust 単体テストが通る。
- core に JNI、Android、Kotlin の依存がない。

### テスト観点

- 正常な文字列で PNG bytes が生成される。
- 日本語など UTF-8 文字列を扱える。
- 空文字の扱いが仕様通りである。
- 生成結果が PNG として最低限妥当である。
- option default が安定している。

### この Phase でやらないこと

- JNI symbol を公開しない。
- Kotlin wrapper を作らない。
- Android UI から呼び出さない。
- size、margin 以外の高度な見た目調整を入れない。
- QR 生成以外の画像編集機能を入れない。

### 次 Phase へ進む判断基準

- Rust core が Android なしでテスト可能である。
- Rust core のエラー型と戻り値が JNI bridge へ渡せる形に整理されている。
- 最小 PNG 生成の仕様が固定されている。

## Phase 3: JNI bridge の最小接続

### ゴール

Kotlin から native method を経由して Rust core を呼び出し、PNG `ByteArray` を取得できるようにする。JNI 関数は内部 API として扱う。

### 主な作成・変更ファイル

候補:

- `rust/qrforge-jni/Cargo.toml`
- `rust/qrforge-jni/src/lib.rs`
- `app/src/main/java/.../internal/QrForgeNative.kt`
- `app/build.gradle.kts`
- `app/src/main/cpp/` または NDK/Cargo 連携設定

既存の C++ template がある場合は、Rust JNI 方式へ移行するか、段階的に置き換えるかを計画で明示してから変更する。

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

## Phase 4: Kotlin SDK wrapper と Android 表示

### ゴール

Android 利用者向けの SDK 風 API を提供し、`QrForge.createBitmap(text)` と `QrForge.createPngBytes(text)` を使って表示できるようにする。

### 主な作成・変更ファイル

候補:

- `app/src/main/java/.../QrForge.kt`
- `app/src/main/java/.../QrForgeException.kt`
- `app/src/main/java/.../QrOptions.kt`
- `app/src/main/java/.../internal/QrForgeNative.kt`
- `app/src/main/java/.../MainActivity.kt`
- `app/src/main/res/layout/...`
- `app/src/test/java/.../QrForgeTest.kt`

将来的な Android library module 切り出しを見越し、app UI と SDK wrapper の package を分離する。

### 完了条件

- `QrForge.createPngBytes(text)` が PNG `ByteArray` を返す。
- `QrForge.createBitmap(text)` が Android `Bitmap` を返す。
- `AppCompatImageView` や `ImageView` に表示できる。
- JNI internal API が外部利用者から見えない設計になっている。
- 入力エラー、生成エラー、デコードエラーの例外方針が整理されている。

### テスト観点

- `createPngBytes` が PNG signature を持つ bytes を返す。
- `createBitmap` が null ではない `Bitmap` を返す。
- 空文字、不正入力、長すぎる文字列の扱いを確認する。
- UI で生成結果を表示できる。
- public API の呼び出し例がドキュメント通り動く。

### この Phase でやらないこと

- QR options の全機能を完成させない。
- ライブラリ配布設定を完成させない。
- UI を過度に作り込まない。
- Rust core のアルゴリズム改善を同時に行わない。

### 次 Phase へ進む判断基準

- Android から SDK wrapper だけを使って QR 表示できる。
- JNI の存在が app 利用コードに漏れていない。
- public API の最小形が安定している。

## Phase 5: ライブラリ化と拡張準備

### ゴール

QrForge を Android ライブラリとして切り出しやすい構成に整理し、`QrOptions` などの拡張を安全に追加できる状態にする。

### 主な作成・変更ファイル

候補:

- `qrforge-android/build.gradle.kts`
- `qrforge-android/src/main/java/...`
- `sample-app/build.gradle.kts`
- `rust/qrforge-core/...`
- `rust/qrforge-jni/...`
- `docs/api-design.md`
- `docs/coding-rules.md`

既存 `:app` を sample app として残すか、新規 library module を作るかは Phase 5 の計画時に決める。

### 完了条件

- SDK wrapper が app 固有 UI から分離されている。
- public API surface が明確である。
- `QrOptions` の追加方針が実装に反映されている。
- release build に必要な native library packaging が整理されている。
- README や利用例を追加できる状態になっている。

### テスト観点

- library module の unit test が通る。
- sample app から library module を利用できる。
- release variant で native library が含まれる。
- ABI ごとの packaging を確認できる。
- 既存 API の互換性が保たれている。

### この Phase でやらないこと

- Maven Central などへの公開を急がない。
- QR 以外のバーコード生成へ広げない。
- API 安定前に過剰な設定項目を増やさない。
- サンプル UI と SDK 本体の責務を混ぜない。

### 次の判断基準

- ライブラリとして利用する最小導線がある。
- public API と内部 API の境界が保たれている。
- 拡張追加時にどのレイヤを変更するか説明できる。

## Phase 横断の確認方針

- Kotlin 変更時は Gradle の該当 test/build task を実行する。
- Rust 変更時は `cargo test` を実行する。
- JNI 変更時は Android 側の起動または instrumentation 相当の確認を行う。
- ドキュメント変更時はコード変更が混ざっていないことを Git 差分で確認する。
- 確認できなかった項目は理由を明記する。
