# QrForge API 設計

## API 設計の目的

QrForge の Android 向け API は、Rust 製 QR 生成機能を Android SDK のように自然に使える形で提供する。利用者は JNI や Rust の存在を意識せず、Kotlin の通常 API として QR コード画像を作れることを重視する。

最初の public API は最小に保ち、将来的な `QrOptions` 追加に備える。

## 最初に提供する Android 向け API

初期 API は次の 2 つを中心にする。

```kotlin
object QrForge {
    fun createBitmap(text: String): Bitmap

    fun createPngBytes(text: String): ByteArray
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

## 将来的な `QrOptions`

将来的には次のような option model を追加する。

```kotlin
data class QrOptions(
    val size: Int = 512,
    val margin: Int = 4,
)
```

追加後の API 候補:

```kotlin
object QrForge {
    fun createBitmap(text: String): Bitmap

    fun createBitmap(
        text: String,
        options: QrOptions,
    ): Bitmap

    fun createPngBytes(text: String): ByteArray

    fun createPngBytes(
        text: String,
        options: QrOptions,
    ): ByteArray
}
```

`QrOptions` を追加しても、option なし API の挙動は変えない。default 値は文書化し、Kotlin wrapper と Rust core の間で意味を揃える。

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

### 将来的な options 指定

```kotlin
val options = QrOptions(
    size = 768,
    margin = 6,
)

val bitmap = QrForge.createBitmap(
    text = "https://example.com",
    options = options,
)
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

初期方針として、利用者向けの基底例外 `QrForgeException` を用意する。

候補:

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

最終的な型構成は実装 Phase で決めるが、次の方針は守る。

- 入力不正と生成失敗を区別する。
- native library load failure を生成失敗に混ぜない。
- PNG decode failure を Rust core の失敗に混ぜない。
- `null` や空配列で失敗を表現しない。

## 入力仕様

初期仕様の候補:

- `text` は blank でない文字列を要求する。
- UTF-8 文字列を扱える。
- QR 仕様や選択ライブラリの容量を超える場合は生成失敗として扱う。
- 前後空白を SDK 側で勝手に trim しない。入力された文字列を QR 化する。

空文字を許可するかどうかは実装前に確定する。迷う場合は Android 利用者にとって誤操作になりやすい blank を拒否する方針を優先する。

## 非公開にすべき JNI API

次の API は利用者向けに公開しない。

```kotlin
internal object QrForgeNative {
    external fun createPngBytes(text: String): ByteArray
}
```

JNI binding は internal package に置く。README、サンプル、docs の利用者向け呼び出し例では `QrForgeNative` を案内しない。

Rust 側の exported JNI symbol も公開 API ではない。名前は JNI 仕様に従って必要になるが、互換性保証の対象として利用者に見せない。

## API 互換性の考え方

- `QrForge.createBitmap(text)` と `QrForge.createPngBytes(text)` は初期安定 API として扱う。
- options を追加する場合は overload を追加する。
- 既存 API の戻り値型を変えない。
- 例外の大分類を変える場合は docs を更新する。
- public model に property を追加する場合は default 値を用意する。

## 実装前の未決事項

実装前に次を確認する。

- 空文字を拒否するか、空文字 QR を許可するか。
- `QrOptions.size` は画像全体の pixel size か、QR module size か。
- `QrOptions.margin` の単位は module 数か pixel 数か。
- 初期 Rust QR 生成 crate と PNG encode crate を何にするか。
- native library の module 名を何にするか。
- Android library module を Phase 4 で作るか、Phase 5 で切り出すか。
