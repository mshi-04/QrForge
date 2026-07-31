# 公開 API 設計

利用者に見せる API の契約。レイヤの分け方は [architecture.md](architecture.md)、実装の書き方は
[coding-rules.md](coding-rules.md) を見る。

利用者は Rust・JNI・NDK を意識しない。この文書に書かれていないものは公開 API ではない。

## 公開するもの

```kotlin
object QrForge {
    @JvmOverloads
    fun createBitmap(text: String, options: QrOptions = QrOptions()): Bitmap

    @JvmOverloads
    fun createPngBytes(text: String, options: QrOptions = QrOptions()): ByteArray
}

data class QrOptions(
    val size: Int = 512,
    val margin: Int = 4,
)

sealed class QrForgeException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
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

option は Kotlin のデフォルト引数で省略できる。`@JvmOverloads` により、Java からは
`createBitmap(String)` と `createBitmap(String, QrOptions)` の 2 つの overload に見える。

## 公開しないもの

`QrForgeNative`、`external fun`、Rust の JNI symbol、`System.loadLibrary("qrforge")`、
Rust crate の内部型と PNG エンコード実装。README・sample・利用者向け文書に出さない。

## 入力

| 項目 | 契約 |
|------|------|
| `text` | blank でない文字列。UTF-8 を扱う。SDK 側で trim しない |
| `text` の長さ | QR 仕様と library の容量を超える場合は生成失敗として扱う（入力不正ではない） |
| `size` | `1..4096` |
| `margin` | `0..64`（QR module 数単位の quiet zone。`0` で無効） |

`text` が空白のみ（`" "`、`"\t\n"` など）は blank として拒否する。前後に空白を含むだけの文字列は
受理し、そのまま QR に埋め込む。

## 出力

### `createPngBytes`

PNG signature (`89 50 4E 47 0D 0A 1A 0A`) で始まる `ByteArray`。グレースケール（L8）の正方形画像。

### `createBitmap`

non-null な `Bitmap`。`BitmapFactory` の既定構成で decode するため `ARGB_8888` になる。

### size は「最小値」であって出力寸法ではない

出力は QR module 境界に合わせて切り上げられるため、`size` ちょうどにはならない。

```text
module_size = ceil(size / (qr_width + margin * 2))
出力の一辺  = (qr_width + margin * 2) * module_size   >= size
```

たとえば `size = 512`、margin 4、21 module の QR なら出力は 522x522 になる。テストでは
`>= size` を検証し、等値を期待しない。

`size = 4096`、`margin = 64` のとき出力は最大 4368x4368 になる。`createBitmap` は
`ARGB_8888` で 1 ピクセル 4 バイトなので、この上限付近ではおよそ 73MiB を確保する。

### 失敗を戻り値で表現しない

`null`、空配列、nullable 戻り値で失敗を伝えない。失敗はすべて例外で返す。

## 例外

| ケース | 例外 |
|--------|------|
| blank text、`QrOptions` の値域違反 | `IllegalArgumentException` |
| native library をロードできない、JNI entry point を解決できない | `QrForgeException.NativeLibraryUnavailable` |
| QR エンコード失敗、PNG エンコード失敗、JNI 側の変換失敗 | `QrForgeException.GenerationFailed` |
| PNG decode 失敗、Bitmap 確保がメモリ上限超過、`OutOfMemoryError` | `QrForgeException.DecodeFailed` |

4 つを混ぜない。利用者が「入力を直せばよいのか」「端末の問題なのか」「ライブラリの同梱漏れなのか」
を区別できることが、この分類の目的。

`QrForgeException` は `sealed class` なので、`when` で網羅的に分岐できる。

### メモリ上限ガード

`createBitmap` はデコード前に画像寸法だけを読み取り、`ARGB_8888` で必要になるバイト数が
上限（128MiB）を超えるなら `DecodeFailed` を投げる。捕捉しにくい `OutOfMemoryError` になる前に、
型付き例外で明示的に失敗させるため。デコード中に `OutOfMemoryError` が起きた場合も
`DecodeFailed` に変換する。

## 値域定数の同期

`QrOptions` の値域は Rust core を正典とし、Kotlin 側はその写し。片方だけ変更しない。

| 定数 | 値 | 定義箇所 |
|------|----|---------|
| `DEFAULT_SIZE` | 512 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |
| `DEFAULT_MARGIN` | 4 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |
| `MIN_SIZE` | 1 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |
| `MAX_SIZE` | 4096 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |
| `MIN_MARGIN` | 0 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |
| `MAX_MARGIN` | 64 | `QrOptions.kt`, `qrforge-core/src/lib.rs` |

Rust core の値域エラー文言はこれらの定数から組み立てるため、値を変えればメッセージも追従する。
Kotlin 側の `require` メッセージも同様に定数を埋め込む。

## 互換性

- 既存 public API の関数名、引数、戻り値型、例外の意味、入力の解釈を変えない。
- public model に property を追加するときは default 値を用意する。
- 新しい設定は既存 API の破壊ではなく `QrOptions` への追加で行う。
- 非同期 API は必要性が確認されてから追加する。今は同期のみ。
- 値域を広げる変更は互換だが、狭める変更は非互換として扱う。
