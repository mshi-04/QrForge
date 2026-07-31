# テスト方針

テストをどこに置き、何を検証し、何を検証しないかの決まり。実行コマンドは [setup.md](setup.md)、
レイヤの責務は [architecture.md](architecture.md) を見る。

テストは公開 API とレイヤ境界の振る舞いを固定するために書く。実装詳細を過剰に固定しない。

## 3 つの置き場所

| 置き場所 | 実行環境 | 検証すること |
|---------|---------|-------------|
| `rust/qrforge-core/tests/` | `cargo test` | QR 生成、PNG bytes、描画結果、option 検証、core error |
| `qrforge/src/test/` | JVM（JUnit Jupiter） | 入力検証、option model、例外変換前の分岐 |
| `qrforge/src/androidTest/` | 実機・エミュレーター | native library load、JNI 呼び出し、`Bitmap` decode |

### どこに置くか決める順序

1. **native library か Android SDK が要るか** → 要るなら `androidTest`
2. 要らないとして、**QR・PNG の中身を検証するか** → するなら Rust core test
3. 要らないとして、**Kotlin wrapper の振る舞いか** → そうなら JVM UnitTest

同じ振る舞いを 2 か所で検証しない。下位で検証できるものは下位に置く。

## 境界

- Rust core のテストで Android・JNI・Kotlin を検証しない。
- JNI bridge のテストで QR 生成アルゴリズムの詳細を検証しない。
- Kotlin wrapper のテストで JNI exported symbol を public API として扱わない。
- Android 実行環境が必要な確認を JVM UnitTest に押し込まない。
- **native library が無いことによる例外を、正常系の証拠として使わない。**

最後の項目が最も踏みやすい。JVM UnitTest では `.so` を解決できないため、「入力が受理されたら
`NativeLibraryUnavailable` になる」ことを利用して正常系を書けてしまう。これは native library が
ロードできる状況になった瞬間に落ちる。入力が受理され、そのまま下位層へ渡ることを確かめたいなら、
注入可能な test seam で wrapper 境界を直接テストし、実際の native 経路は Instrumented Test に任せる。

## 書き方

- テスト名は英語で、期待する振る舞いが読み取れる名前にする。
- 1 テスト関数は 1 つの振る舞いだけを検証する。主要 assert は 1 つ。
- Kotlin テストには `Arrange`、`Act`、`Assert` コメントを置く。例外検証は `Act & Assert` にまとめてよい。
- 主要 assert を helper に隠さない。
- 出力寸法は `>=` で検証する。`size` は最小値であって出力寸法ではない（[api-design.md](api-design.md)）。

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

## 各層の作法

### Rust core test

`rust/qrforge-core/tests/` の integration test は別 crate としてコンパイルされるが、package の
`[dependencies]` と `[dev-dependencies]` の両方を利用できる。production code でも使う crate を
`[dev-dependencies]` に重複定義しない。テストだけで使う crate や feature が必要な場合に限り、
`[dev-dependencies]` へ追加する。現在の `image` crate は、production code が `png` crate で
encode した結果を integration test で decode するためだけに置いている。

描画位置や塗り潰し範囲のように、PNG ヘッダと寸法だけでは検出できない性質はここで検証する。
Instrumented Test では担保しにくい。

### JVM UnitTest

native library に依存しない範囲に限る。テストのためだけに `internal` を広げた関数は、その旨が
実装側にコメントされていること（[coding-rules.md](coding-rules.md)）。

### Instrumented Test

共通定数は `qrforge/src/androidTest/java/com/appvoyager/qrforge/QrForgeTestFixtures.kt` に置く。
assert は置かない。

ローカルで実行する前に、直前の `rust/` 変更に対して `.so` を再ビルドしたか確認する。していなければ
検証対象は変更前の native library になる。CI の instrumented job は意図的に repository に
コミット済みの `x86_64` `.so` を使うため、Rust source のビルド確認は別の `rust` job が担う
（[setup.md](setup.md)）。

## 報告

- 実行したコマンドと結果（件数を含む）を書く。
- 実行していないテストを「通った」と書かない。
- 環境要因で実行できなかったものは、理由とともに未実施として書く。
- 新しく書いたテストが一度も実行できていない場合は、その旨を明記する。
