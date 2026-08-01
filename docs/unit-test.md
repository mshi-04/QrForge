# テスト方針

テストをどこに置き、何を検証し、何を検証しないかの決まり。実行コマンドは [setup.md](setup.md)、
レイヤの責務は [architecture.md](architecture.md) を見る。

テストは公開 API とレイヤ境界の振る舞いを固定するために書く。実装詳細を過剰に固定しない。

## テストの置き場所

| 置き場所 | 実行環境 | 検証すること |
|---------|---------|-------------|
| `rust/qrforge-core/src/` の対象モジュール内 | `cargo test` | 公開 API 経由では条件を作れない private 実装（現状 0 件） |
| `rust/qrforge-core/tests/` | `cargo test` | 公開 API 経由の QR 生成、PNG bytes、描画結果、option 検証、core error |
| Rust 公開 API の rustdoc | `cargo test --doc` | 利用例がコンパイル・実行できること |
| `qrforge/src/test/` | JVM（JUnit Jupiter） | 入力検証、option model、例外変換前の分岐 |
| `qrforge/src/androidTest/` | 実機・エミュレーター | native library load、JNI 呼び出し、`Bitmap` decode |

### どこに置くか決める順序

1. **native library か Android SDK が要るか** → 要るなら `androidTest`
2. 要らないとして、**QR・PNG の中身を公開 API 経由で検証するか** → するなら Rust integration test
3. **公開 API 経由では条件を作れない private 実装か** → そうなら対象モジュール内の Rust Unit Test
4. **Rust 公開 API の利用例か** → そうなら Documentation Test
5. **Kotlin wrapper の振る舞いか** → そうなら JVM UnitTest

同じ振る舞いを複数の場所で重複して検証しない。Documentation Test は利用例が実際に動くこと、
integration test は公開 API の詳細な契約を担う。下位で検証できるものは下位に置く。

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

規則は出典で分けて書く。公式文書に根拠があるものと、公式に規定がなくここで決めたものとでは、
後から見直すときに動かしてよい範囲が違う。

- テスト名は英語で、期待する振る舞いが読み取れる名前にする。
- 出力寸法は `>=` で検証する。`size` は最小値であって出力寸法ではない（[api-design.md](api-design.md)）。

### Rust（公式に根拠がある）

出典は The Rust Programming Language ch.11 と Rust API Guidelines。後者は「mandate ではない」と
自ら書いている推奨チェックリストなので、Book より弱い根拠として扱う。

- setup、検証対象の実行、結果の assert という流れで構成する（Book ch.11-01）。
- テストは並列・順不同で実行できるようにし、共有可変状態や実行順へ依存させない（Book ch.11-02）。
- 等値検証には `assert_eq!` / `assert_ne!` を使う。失敗時に両辺の値を印字するため、
  `assert!(a == b)` より原因が分かる（Book ch.11-01）。
- テスト関数は `Result<T, E>` を返せるため、正常系では `?` でエラーを伝播できる
  （Book ch.11-01）。エラーになること自体を検証するテストは `?` にできないので `expect_err` を使う。
- 利用例は公開 API の rustdoc に `# Examples` として書き、`unwrap` / `expect` ではなく `?` を使う。
  例のコードはそのままコピーされるため（API Guidelines C-EXAMPLE / C-QUESTION-MARK）。
- 返し得るエラーは `# Errors` に書く（API Guidelines C-FAILURE）。
- format は rustfmt のデフォルト（公式 Style Guide）に従う。

`matches!` は真偽値を返すだけなので、`assert!(matches!(...))` の失敗出力に実際の値は出ない。
error variant を検証するときは失敗メッセージを添える。Rust 1.96 以降では標準の
`assert_matches!` も利用できる。CI は未固定の stable toolchain を使うため利用可能。

```rust
assert!(
    matches!(error, QrGenerationError::BlankInput),
    "unexpected error: {error}"
);
```

### Rust（本リポジトリの判断）

- qrforge-core の正常系テストは `-> Result<(), QrGenerationError>` を返し、`?` で生成エラーを
  伝播させる。
- `Arrange`、`Act`、`Assert` の定型ラベルは置かない。必要なら空行で処理のまとまりを示す。
- 境界値の根拠や依存ライブラリとの契約差など、「なぜ」を説明するコメントだけを残す。
- 1 テスト関数は 1 つの振る舞いだけを検証する。同じ振る舞いを十分に表すためなら複数の
  assert を許可する。
- `test_` のような定型的な接頭辞に頼らず、期待する振る舞いをテスト名で表す。Book と std の例も
  接頭辞を使っていない。
- 独自の `rustfmt.toml` は追加しない。

### Kotlin / JUnit（本リポジトリの判断）

`Arrange` / `Act` / `Assert` は JUnit の公式推奨ではない。JUnit User Guide は annotation と実行
モデルの reference で、テスト本体の構造や命名規約を定めていない。3A パターン自体は xUnit 系の
一般的なテスト設計論で、ここでの採用は本リポジトリの判断。

- 1 テスト関数は 1 つの振る舞いだけを検証する。主要 assert は 1 つ。
- `Arrange`、`Act`、`Assert` コメントを置く。例外検証は `Act & Assert` にまとめてよい。
- 主要 assert を helper に隠さない。

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

公開 API は crate 外の利用者と同じ条件で検証するため、`rust/qrforge-core/tests/` の integration test
に置く（Book ch.11-03）。利用例は公開 API の rustdoc に `# Examples` として記述し、
Documentation Test にする。

private 実装を直接検証する Unit Test は、対象 source file 内の `#[cfg(test)] mod tests` に置く
（Book ch.11-03）。ただしここに置いてよいのは、**公開 API 経由では入力条件を作れない場合に限る**。
`validate_options` のように公開 API から到達できるものを個別にテストすると、実装詳細を固定して
リファクタリングを妨げる。現状の `qrforge-core` に該当する private 実装はなく、in-src Unit Test は
0 件。

`qrforge-core` は crate 先頭で `unwrap` / `expect` を deny している（[coding-rules.md](coding-rules.md)）。
この attribute は同じ crate に属する `#[cfg(test)] mod tests` にも効くため、repository root の
`clippy.toml` で `allow-unwrap-in-tests` と `allow-expect-in-tests` を有効にしている。production code
での禁止はそのまま維持される。`tests/` の integration test は別 crate なので元から対象外。

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

共通定数は `qrforge/src/androidTest/java/com/appvoyager/qrforge/QrTestFixtures.kt` に置く。
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
