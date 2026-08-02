# QrForge

QrForge は、Android アプリから Kotlin API で利用できる Rust 製 QR コード生成ライブラリです。

利用者向け API を `QrGenerator` に集約し、QR の生成と PNG エンコードを Rust で処理します。JNI や native library の詳細はライブラリ内部に閉じ込めているため、Android 側では通常の Kotlin ライブラリとして扱えます。

## 主な特徴

- 文字列から QR コードの PNG データを生成
- QR コードを Android `Bitmap` として生成
- 画像の最小サイズと quiet zone の余白を指定可能
- 入力不正、native library の読み込み失敗、生成失敗、画像デコード失敗を例外型で区別
- `arm64-v8a`、`armeabi-v7a`、`x86_64` をサポート
- QR 生成ロジックを Android から分離した Rust core として構成

## 使い方

### Bitmap を生成する

```kotlin
import com.appvoyager.qrforge.QrGenerator

val bitmap = QrGenerator.createBitmap("https://example.com")
imageView.setImageBitmap(bitmap)
```

### PNG データを生成する

```kotlin
import com.appvoyager.qrforge.QrGenerator

val pngBytes: ByteArray = QrGenerator.createPngBytes("Hello QR")
```

### サイズと余白を指定する

```kotlin
import com.appvoyager.qrforge.QrGenerator
import com.appvoyager.qrforge.QrOptions

val bitmap = QrGenerator.createBitmap(
    text = "https://example.com",
    options = QrOptions(
        size = 768,
        margin = 6,
    ),
)
```

## オプション

| プロパティ | 既定値 | 範囲 | 説明 |
|------------|--------|------|------|
| `size` | `512` | `1..4096` | 出力画像の一辺の最小サイズ（pixel） |
| `margin` | `4` | `0..64` | QR コードの周囲に設ける余白（QR module 数） |

QR module の境界に合わせて拡大するため、実際の出力画像は `size` より大きくなる場合があります。`margin = 0` を指定すると quiet zone を無効にできます。

空白だけの文字列、または範囲外のオプションは `IllegalArgumentException` で拒否されます。文字列の前後に含まれる空白は削除せず、そのまま QR コードに埋め込みます。

## エラー処理

| ケース | 例外 |
|--------|------|
| 空白だけの文字列、オプションの値域違反 | `IllegalArgumentException` |
| native library の読み込みまたは JNI entry point の解決に失敗 | `QrGenerationException.NativeLibraryUnavailable` |
| QR または PNG の生成に失敗 | `QrGenerationException.GenerationFailed` |
| PNG のデコードまたは `Bitmap` の確保に失敗 | `QrGenerationException.DecodeFailed` |

`QrGenerationException` は sealed class なので、失敗の種類を `when` で分岐できます。

```kotlin
import com.appvoyager.qrforge.QrGenerationException
import com.appvoyager.qrforge.QrGenerator

try {
    val bitmap = QrGenerator.createBitmap("https://example.com")
} catch (error: QrGenerationException) {
    when (error) {
        is QrGenerationException.NativeLibraryUnavailable -> Unit
        is QrGenerationException.GenerationFailed -> Unit
        is QrGenerationException.DecodeFailed -> Unit
    }
}
```

## 構成

```text
Android app
    ↓
Kotlin public API (QrGenerator, QrOptions, QrGenerationException)
    ↓
Kotlin JNI binding
    ↓
Rust JNI bridge
    ↓
Rust core (QR generation and PNG encoding)
```

Android アプリやライブラリ利用者が JNI API を直接呼び出すことは想定していません。

## 対応環境

- Android API 28 以降
- `arm64-v8a`
- `armeabi-v7a`
- `x86_64`

各 ABI の `libqrforge.so` は Android library module に同梱されます。

## 公開状態

現在は、このリポジトリ内の Android library module `:qrforge` として利用できます。
