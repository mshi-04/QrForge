# QrForge API 設計

Android 利用者が Rust、JNI、NDK を意識せず QR コードを生成できる API にする。

## Public API

```kotlin
object QrForge {
    @JvmOverloads
    fun createBitmap(text: String, options: QrOptions = QrOptions()): Bitmap

    @JvmOverloads
    fun createPngBytes(text: String, options: QrOptions = QrOptions()): ByteArray
}
```

option を省略した場合は `QrOptions()` を使う。`@JvmOverloads` により Java からは
`createBitmap(String)` / `createBitmap(String, QrOptions)` の 2 つの overload として見える。

## QrOptions

```kotlin
data class QrOptions(
    val size: Int = 512,
    val margin: Int = 4,
)
```

| 項目 | 範囲 | 意味 |
|------|------|------|
| `size` | `1..4096` | 画像全体の最小ピクセルサイズ |
| `margin` | `0..64` | QR module 数単位の quiet zone |

範囲外は `IllegalArgumentException`。`size` が大きいほど `Bitmap` メモリ使用量も増える。`createBitmap` はデコード後の確保メモリに上限を設け、超過時や `OutOfMemoryError` 発生時は `QrForgeException.DecodeFailed` として明示的に失敗する。

## 戻り値

- `createBitmap`: 成功時は non-null `Bitmap`。PNG decode 失敗は例外。
- `createPngBytes`: 成功時は PNG signature を持つ `ByteArray`。空配列で失敗を表現しない。
- `null`、空配列、nullable 戻り値で失敗原因を表現しない。

## 例外

| ケース | 例外 |
|--------|------|
| blank text / options 範囲外 | `IllegalArgumentException` |
| native library load failure | `QrForgeException.NativeLibraryUnavailable` |
| QR 生成失敗 | `QrForgeException.GenerationFailed` |
| PNG decode 失敗 / Bitmap 確保がメモリ上限超過・OOM | `QrForgeException.DecodeFailed` |

native library load failure、生成失敗、decode 失敗は混ぜない。

## 入力

- `text` は blank でない文字列。
- UTF-8 文字列を扱う。
- SDK 側で勝手に trim しない。
- QR 仕様やライブラリ容量を超える場合は生成失敗として扱う。

## 非公開 API

次は README、sample、利用者向け docs に出さない。

```kotlin
internal object QrForgeNative {
    fun generateQrPng(text: String, size: Int, margin: Int): ByteArray
}
```

## 定数同期

`QrOptions` の定数は Kotlin と Rust core の両方にある。Rust core を正典とし、変更時は同時に更新する。

| 定数 | 値 | 定義箇所 |
|------|----|---------|
| `DEFAULT_SIZE` | 512 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |
| `DEFAULT_MARGIN` | 4 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |
| `MIN_SIZE` | 1 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |
| `MAX_SIZE` | 4096 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |
| `MIN_MARGIN` | 0 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |
| `MAX_MARGIN` | 64 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |

Rust core の option 範囲エラー文言はこれらの定数から組み立てるため、値を変えるとメッセージも追従する。

## 互換性

- 既存 public API の戻り値型、例外の意味、入力解釈を変えない。
- public model に property を追加する場合は default 値を用意する。
- 非同期 API は必要性が確認されてから追加する。
