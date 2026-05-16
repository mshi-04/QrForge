# QrForge API 設計

## API 設計の目的

QrForge の Android 向け API は、Rust 製 QR 生成機能を Android SDK のように自然に使える形で提供する。利用者は JNI や Rust の存在を意識せず、Kotlin の通常 API として QR コード画像を作れることを重視する。

public API は最小に保ちつつ、`QrOptions(size, margin)` で画像サイズと余白を指定できるようにする。

## Android 向け API

API は次の 4 つを中心にする。option なし API は `QrOptions()` を使う互換入口として扱う。

```kotlin
object QrForge {
    fun createBitmap(text: String): Bitmap
    fun createBitmap(text: String, options: QrOptions): Bitmap

    fun createPngBytes(text: String): ByteArray
    fun createPngBytes(text: String, options: QrOptions): ByteArray
}
```

### `QrForge.createBitmap(text)`

Android UI でそのまま表示したい利用者向けの API。

想定する責務:

- 入力文字列を検証する。
- 内部的に PNG bytes を生成する。
- PNG bytes を `Bitmap` に変換する。
- 失敗時は利用者向け例外を投げる。

この API は、`ImageView` や `AppCompatImageView` に表示する一般的な Android 利用者を想定する。

### `QrForge.createPngBytes(text)`

PNG bytes を保存、共有、独自デコードしたい利用者向けの API。

想定する責務:

- 入力文字列を検証する。
- JNI bridge 経由で Rust core を呼び出す。
- Rust core が生成した PNG bytes を返す。
- 失敗時は利用者向け例外を投げる。

この API は `Bitmap` に依存しないため、保存や送信にも使いやすい。

## `QrOptions`

画像サイズと余白は `QrOptions` で指定する。

```kotlin
data class QrOptions(
    val size: Int = 512,    // 画像全体の最小ピクセルサイズ
    val margin: Int = 4,    // QR module 数単位のマージン
)
```

`size` は `1..4096`、`margin` は `0..64` を受け付ける。範囲外は `IllegalArgumentException` を投げる。

呼び出し例:

```kotlin
val bitmap = QrForge.createBitmap(
    text = "https://example.com",
    options = QrOptions(size = 768, margin = 6),
)
```

option なし API の挙動は変えない。default 値は Kotlin wrapper と Rust core の間で意味を揃える。

## 呼び出し例

### Bitmap を作成する

```kotlin
val bitmap = QrForge.createBitmap("https://example.com")
imageView.setImageBitmap(bitmap)
```

### PNG bytes を作成する

```kotlin
val pngBytes = QrForge.createPngBytes("Hello QrForge")
```

## AppCompatImageView での表示例

```kotlin
val qrBitmap = QrForge.createBitmap("https://example.com")
findViewById<AppCompatImageView>(R.id.qrImageView).setImageBitmap(qrBitmap)
```

SDK wrapper は `AppCompatImageView` を直接要求しない。表示先の UI component は app 側が選ぶ。

## 戻り値設計

### `createBitmap`

- 成功時は non-null の `Bitmap` を返す。
- 失敗時に `null` を返さない。
- PNG decode に失敗した場合は例外を投げる。

### `createPngBytes`

- 成功時は PNG file signature を持つ `ByteArray` を返す。
- 空配列で失敗を表現しない。
- 生成失敗時は例外を投げる。

nullable 戻り値や sentinel value で失敗を表現しない。呼び出し側が失敗原因を扱えるよう、例外に情報を持たせる。

## 例外設計

利用者向けの基底例外 `QrForgeException` を用意する。

```kotlin
sealed class QrForgeException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    class InvalidInput(message: String) : QrForgeException(message)

    class GenerationFailed(
        message: String,
        cause: Throwable? = null,
    ) : QrForgeException(message, cause)

    class DecodeFailed(
        message: String,
        cause: Throwable? = null,
    ) : QrForgeException(message, cause)

    class NativeLibraryUnavailable(
        message: String,
        cause: Throwable? = null,
    ) : QrForgeException(message, cause)
}
```

次の方針を守る。

- 入力不正は `IllegalArgumentException` を投げる。
- 生成失敗は `QrForgeException.GenerationFailed` を投げる。
- native library load failure を生成失敗に混ぜない。
- PNG decode failure を Rust core の失敗に混ぜない。
- `null` や空配列で失敗を表現しない。

## 入力仕様

- `text` は blank でない文字列を要求する。
- UTF-8 文字列を扱える。
- QR 仕様や選択ライブラリの容量を超える場合は生成失敗として扱う。
- 前後空白を SDK 側で勝手に trim しない。入力された文字列を QR 化する。

空文字・blank は `IllegalArgumentException` で拒否する。

## 非公開にすべき JNI API

次の API は利用者向けに公開しない。

```kotlin
internal object QrForgeNative {
    fun generateQrPng(text: String, size: Int, margin: Int): ByteArray
}
```

JNI binding は internal package に置く。README、サンプル、docs の利用者向け呼び出し例では `QrForgeNative` を案内しない。

Rust 側の exported JNI symbol も公開 API ではない。名前は JNI 仕様に従って必要になるが、互換性保証の対象として利用者に見せない。

## スレッド安全性

`QrForge` は `object`（シングルトン）として定義するが、内部状態は持たない設計にする。

- 同期 API として提供し、呼び出し側が必要に応じてスレッド制御する。
- 非同期 API（`suspend fun` 等）は Phase 4 以降、必要性が確認されてから追加する。
- `QrForge` の関数は副作用を持たず、同一引数で同一結果を返す前提にする。

## API 互換性の考え方

- `QrForge.createBitmap(text)` と `QrForge.createPngBytes(text)` は初期安定 API として扱う。
- options を追加する場合は overload を追加する。
- 既存 API の戻り値型を変えない。
- 例外の大分類を変える場合は docs を更新する。
- public model に property を追加する場合は default 値を用意する。

## QrOptions の定数仕様

`QrOptions` の定数は Kotlin (`QrOptions.kt`) と Rust core (`qrforge-core/src/lib.rs`) の両方に定義されている。
**変更時は必ず両方を同時に更新すること。**

| 定数 | 値 | 定義箇所 |
|------|----|---------|
| `DEFAULT_SIZE` | 512 | `QrOptions.kt`, `lib.rs` (`DEFAULT_IMAGE_SIZE`) |
| `DEFAULT_MARGIN` | 4 | `QrOptions.kt`, `lib.rs` (`QrOptions::default`) |
| `MIN_SIZE` | 1 | `QrOptions.kt`, `lib.rs` (`MIN_IMAGE_SIZE`) |
| `MAX_SIZE` | 4096 | `QrOptions.kt`, `lib.rs` (`MAX_IMAGE_SIZE`) |
| `MIN_MARGIN` | 0 | `QrOptions.kt`（Rust は 0 未満を拒否） |
| `MAX_MARGIN` | 64 | `QrOptions.kt`, `lib.rs` (`MAX_MARGIN`) |

## 仕様決定済み事項

以下は実装着手前に確定済み。

| 項目 | 決定内容 |
|------|---------|
| 空文字・blank の扱い | 拒否。`IllegalArgumentException` を投げる |
| `QrOptions.size` の意味 | 画像の最小ピクセルサイズ。モジュール境界の都合で指定値以上になる場合がある |
| `QrOptions.margin` の単位 | QR module 数。0 で quiet zone 無効、1 以上で指定 module 数の quiet zone を有効化 |
| Rust QR 生成 crate | `qrcode 0.14.1` |
| Rust PNG エンコード crate | `image 0.25.10`（PNG feature） |
| native library 名 | `qrforge`（`libqrforge.so`） |
| Android library module 切り出し | 後続 Phase で対応 |
