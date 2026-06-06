# UnitTest ルール

テストは公開 API とレイヤ境界の振る舞いを安定させるために書く。実装詳細を過剰に固定しない。

## 基本方針

- JVM UnitTest は JUnit Jupiter。
- 1 テスト関数は 1 つの主要な振る舞いだけを検証する。
- 主要 assert は 1 つにする。
- Kotlin テストには `Arrange`、`Act`、`Assert` コメントを置く。
- 例外検証は `Act & Assert` としてまとめてよい。
- テスト名は英語で、期待する振る舞いが分かる名前にする。
- helper に主要 assert を隠さない。

## 配置

| 対象 | 配置 | 検証すること |
|------|------|--------------|
| Rust core | `rust/qrforge-core/tests/` | QR 生成、PNG bytes、option validation、core error |
| Kotlin UnitTest | `qrforge/src/test/` | JVM 上で可能な入力検証、option model、例外変換前の分岐 |
| Instrumented Test | `qrforge/src/androidTest/` | native library load、JNI 呼び出し、Bitmap decode |

## 境界

- Rust core のテストで Android / JNI / Kotlin を検証しない。
- JNI bridge のテストで QR 生成アルゴリズムの詳細を検証しない。
- Kotlin wrapper のテストで JNI exported symbol を public API として扱わない。
- Android 実行環境が必要な確認を JVM UnitTest に押し込まない。

## Kotlin 例

```kotlin
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

## 確認コマンド

```powershell
cargo fmt --all -- --check
cargo test --manifest-path rust/qrforge-core/Cargo.toml
.\gradlew.bat :qrforge:testDebugUnitTest
.\gradlew.bat :qrforge:assembleDebugAndroidTest
.\gradlew.bat :qrforge:connectedDebugAndroidTest
.\gradlew.bat :app:assembleDebug
```

`connectedDebugAndroidTest` は実機またはエミュレーターが必要。実行できない場合は理由を報告する。
