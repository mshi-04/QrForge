# QrForge アーキテクチャ

## 目的

QrForge は、Android アプリから Kotlin API として自然に呼び出せる Rust 製 QR コード生成ライブラリである。利用者は Rust、JNI、NDK の詳細を意識せず、`QrForge.createBitmap(text)` または `QrForge.createPngBytes(text)` を呼ぶだけで QR コード画像を得られる。

現在は文字列を QR コード PNG 画像データに変換し、Android 側で `Bitmap` として表示できる。`QrOptions(size, margin)` によるサイズと余白の指定にも対応している。エラー訂正レベル、色指定、複数画像形式への拡張はまだ提供していない。

## レイヤ構成

QrForge は次の責務境界で設計する。

```text
Android app
  ↓
Android SDK wrapper (Kotlin public API)
  ↓
JNI bridge (Kotlin internal native binding + native exported symbols)
  ↓
Rust core (QR generation and PNG encoding)
```

依存方向は常に上から下に向ける。Rust core は Android UI、Kotlin、JNI の事情を知らない。Kotlin wrapper は JNI の詳細を隠蔽し、Android app は SDK wrapper のみを利用する。

## ディレクトリとレイヤの対応

```text
QrForge/
├── app/src/main/java/com/appvoyager/qrforge/
│   └── sample/
│       └── MainActivity.kt     # Android app (sample UI)
├── qrforge/src/main/java/com/appvoyager/qrforge/
│   ├── QrForge.kt              # SDK wrapper (public API)
│   ├── QrForgeException.kt     # public 例外型
│   ├── QrOptions.kt            # public option model
│   └── internal/
│       └── QrForgeNative.kt    # JNI bridge (internal)
├── qrforge/src/main/jniLibs/
│   └── arm64-v8a/
│       └── libqrforge.so       # プリビルド native library
├── rust/
│   ├── qrforge-core/           # Rust core (QR 生成 + PNG エンコード)
│   │   ├── Cargo.toml          #   依存: qrcode 0.14.1, image 0.25.10
│   │   ├── src/lib.rs
│   │   └── tests/              #   Rust 単体テスト
│   └── qrforge-jni/            # JNI bridge (Rust 側)
│       ├── Cargo.toml          #   lib name: qrforge (→ libqrforge.so)
│       └── src/lib.rs
```

## Android app の責務

Android app (`:app`) は QrForge を利用するサンプルまたは検証用 UI として扱う。SDK 本体は Android library module `:qrforge` に置く。

- ユーザー入力を受け取り、SDK wrapper を呼び出す。
- 返却された `Bitmap` を `ImageView` や `AppCompatImageView` に表示する。
- ライブラリ内部の JNI 関数、Rust の型、PNG エンコード処理を直接扱わない。
- エラー表示や入力バリデーションなど、アプリ固有の UI 判断だけを持つ。

Android app に QR 生成ロジックを置かない。サンプル UI が便利でも、ライブラリ責務を app 層へ漏らさない。

## Android SDK wrapper の責務

SDK wrapper は Android 利用者に公開する Kotlin API である。

- `QrForge.createBitmap(text: String): Bitmap`
- `QrForge.createPngBytes(text: String): ByteArray`
- `QrOptions` 付き overload
- 入力値の基本検証
- JNI bridge の呼び出し
- PNG bytes から `Bitmap` への変換
- 利用者向け例外への変換

SDK wrapper は、Android SDK らしい呼び出しやすさを最優先する。利用者が `nativeCreatePngBytes` のような関数名や JNI シグネチャを意識する設計にしない。

## JNI bridge の責務

JNI bridge は Kotlin と Rust の境界を接続する内部実装である。

- Kotlin internal/native binding と Rust exported JNI symbol を対応させる。
- Kotlin の `String` を Rust 側で扱える文字列に変換する。
- Rust core の戻り値を `ByteArray` として Kotlin に返す。
- Rust core のエラーを JNI 境界で表現可能な形に変換する。
- JNI の例外、null、メモリ所有権、スレッド境界に注意する。

JNI bridge は公開 API ではない。パッケージ、可視性、命名で内部実装であることを明確にする。Android app や外部利用者が JNI bridge を直接呼び出す前提にしない。

## Rust core の責務

Rust core は QR コード生成と PNG エンコードの純粋な中核である。

- 入力文字列を QR コードに変換する。
- QR コードを PNG bytes にエンコードする。
- size、margin などの options を受け取れる構造を持つ。
- Android、JNI、Kotlin、UI に依存しない。
- Rust 単体テストで検証できる API を持つ。

Rust core の関数は JNI を前提にした名前や型にしない。JNI 向けのラッパーは別レイヤに置き、core は通常の Rust ライブラリとして再利用できる形を保つ。

## 公開 API と内部 API の境界

公開 API は Kotlin の SDK wrapper に限定する。利用者が安定して依存してよい API は `QrForge` と関連する public model だけである。

公開 API の候補:

- `QrForge`
- `QrOptions`
- `QrForgeException`
- 必要に応じた `QrError` または例外 subtype

内部 API の候補:

- JNI native method
- Rust exported JNI symbol
- Rust core の内部モジュール
- PNG エンコード実装の詳細
- `System.loadLibrary` の配置や native library 名

内部 API は変更可能であることを前提にし、外部利用者の呼び出し例や README で案内しない。

## Rust core と JNI bridge を分離する理由

Rust core と JNI bridge を分離する理由は次の通り。

- Rust core を Android 以外でも再利用できる。
- Rust core のテストを Android emulator や JNI なしで実行できる。
- JNI 固有のメモリ管理、例外変換、型変換が QR 生成ロジックに混ざらない。
- Android 側 API の変更と QR 生成アルゴリズムの変更を独立して扱える。
- 将来的に CLI、iOS、server side などへ展開しやすい。

JNI は境界接続のための技術であり、ドメインロジックの置き場所ではない。

## Android 側に Rust/JNI の詳細を漏らさない方針

Android 利用者は次のような知識を要求されない設計にする。

- JNI 関数名
- `external fun` の存在
- Rust crate の関数名
- PNG エンコードライブラリの種類
- native library の ABI やロード手順
- Rust 側のエラー型

これらは SDK wrapper 内で隠蔽する。Android app には `QrForge.createBitmap(text)` のような目的ベースの API だけを見せる。

## 拡張方針

拡張は Kotlin public API、JNI bridge、Rust core の順に責務を確認してから進める。

- `QrOptions(size, margin)` を追加する場合、Kotlin model と Rust core option model の対応を明確にする。
- 既存 overload を壊さず、新しい overload を追加する。
- オプション未指定時のデフォルト値を SDK wrapper に定義し、Rust core にも安全な default を用意する。
- エラー訂正レベル、色、背景色、画像形式などは一度に混ぜず、変更単位を分けて追加する。
- Android ライブラリモジュール `:qrforge` に SDK wrapper を置き、app 固有コードと SDK wrapper を分離する。

## 守るべき設計判断

- Android app は利用者側、SDK wrapper は公開 API、JNI bridge は内部接続、Rust core は生成ロジックと考える。
- JNI bridge を便利関数置き場にしない。
- Kotlin wrapper に Rust の都合を漏らさない。
- Rust core に Android の都合を漏らさない。
- 公開 API の変更は慎重に扱い、実装都合で名前や戻り値を揺らさない。
