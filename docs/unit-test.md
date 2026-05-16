# UnitTest ルール

QrForge のテストは、Rust core、JNI bridge、Kotlin SDK wrapper の責務境界を崩さずに書く。テストの目的は、実装詳細を固定することではなく、公開 API とレイヤ境界の振る舞いを安定させることである。

## 基本方針

- JVM UnitTest は JUnit Jupiter で書く。
- 1 テスト関数では 1 つの主要な振る舞いだけを検証する。
- 1 テスト関数の主要 assert は 1 つにする。
- 複数の観点を確認したい場合は、テスト関数を分ける。
- 各テストには `Arrange`、`Act`、`Assert` のコメントを記載する。
- 例外検証は `Act & Assert` としてまとめてよい。
- Android Studio が生成した template test は残さない。
- テスト名は英語で、期待する振る舞いが分かる名前にする。

## テスト対象の分け方

| 対象 | 配置 | 役割 |
|------|------|------|
| Rust core | `rust/qrforge-core/tests/` | QR 生成、PNG bytes、option validation、core error を検証する |
| Kotlin UnitTest | `qrforge/src/test/` | JVM 上で検証できる Kotlin wrapper の入力検証、option model、例外変換前の分岐を検証する |
| Instrumented Test | `qrforge/src/androidTest/` | native library load、JNI 呼び出し、Bitmap decode など Android 実行環境が必要な振る舞いを検証する |

Rust core のテストで Android、JNI、Kotlin の事情を検証しない。JNI bridge のテストで QR 生成アルゴリズムの詳細を検証しない。Kotlin wrapper のテストで JNI exported symbol を直接 API として扱わない。

## AAA コメント

通常の成功系は次の形にする。

```kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Test
fun qrOptionsUsesDefaultSize() {
    // Arrange
    val options = QrOptions()

    // Act
    val size = options.size

    // Assert
    assertEquals(QrOptions.DEFAULT_SIZE, size)
}
```

例外系は `Act & Assert` を使ってよい。

```kotlin
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@Test
fun createPngBytesThrowsIllegalArgumentForBlankText() {
    // Arrange
    val text = "   "

    // Act & Assert
    assertThrows(IllegalArgumentException::class.java) {
        QrForge.createPngBytes(text)
    }
}
```

Rust も同じ粒度で書く。

```rust
#[test]
fn returns_error_for_empty_text() {
    // Arrange
    let text = "";
    let options = QrOptions::default();

    // Act
    let error = generate_qr_png(text, &options).expect_err("empty text should be rejected");

    // Assert
    assert!(matches!(error, QrForgeError::BlankInput));
}
```

## 1 関数 1 アサート

次のように複数観点を 1 テストに詰め込まない。

```kotlin
@Test
fun createBitmapReturnsExpectedBitmap() {
    // Arrange
    val text = "Hello QrForge"

    // Act
    val bitmap = QrForge.createBitmap(text)

    // Assert
    assertTrue(bitmap.width >= QrOptions.DEFAULT_SIZE)
    assertTrue(bitmap.height >= QrOptions.DEFAULT_SIZE)
    assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
}
```

width、height、config はそれぞれ別テストに分ける。

```kotlin
@Test
fun createBitmapReturnsAtLeastDefaultWidthBitmap() {
    // Arrange
    val text = "Hello QrForge"

    // Act
    val bitmap = QrForge.createBitmap(text)

    // Assert
    assertTrue(bitmap.width >= QrOptions.DEFAULT_SIZE)
}
```

## Helper の扱い

- helper は値の作成や読み取りに留め、主要 assert を helper に隠さない。
- PNG header や width 取得などの低レベル処理は helper にしてよい。
- helper 内で panic や assert が必要になる場合は、テスト本体の assert と責務が重複していないか見直す。

## 確認コマンド

変更内容に応じて、次を実行する。

```powershell
cargo fmt --all -- --check
cargo test --manifest-path rust/qrforge-core/Cargo.toml
.\gradlew.bat :qrforge:testDebugUnitTest
.\gradlew.bat :qrforge:assembleDebugAndroidTest
.\gradlew.bat :qrforge:connectedDebugAndroidTest
.\gradlew.bat :app:assembleDebug
```

`connectedDebugAndroidTest` は実機またはエミュレーターが必要。実行できない場合は、理由を報告する。
