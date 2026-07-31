# 実装ルール

コードを書くときの具体的な決まり。どのレイヤに置くかは [architecture.md](architecture.md)、
公開 API の契約は [api-design.md](api-design.md)、確認コマンドは [setup.md](setup.md) を見る。

ここに書くのは「そのレイヤの中でどう書くか」だけ。責務境界は繰り返さない。

## 共通

- 失敗は例外か `Result` で返す。`null`、空配列、`-1` のような番兵値で失敗を表現しない。
- コメントには「なぜそうしたか」を書く。何をしているかはコードで示す。
- 既存コードの命名・コメント密度・書き方に合わせる。周辺と浮くスタイルを持ち込まない。
- マジックナンバーは名前付き定数にする。エラー文言に埋めるときも定数から組み立てる。

## Kotlin wrapper

### 可視性

- 公開するのは `QrForge`、`QrOptions`、`QrForgeException` だけ。それ以外は `internal`。
- テストのためだけに `internal` にする場合は、その旨をコメントで明示する。
  意図せず公開範囲を広げたのか、テスト都合なのかを読み手が区別できるようにする。

Java 利用者向け API に漏らさないため、テスト用の `internal` 関数には `@JvmSynthetic` も付ける。

```kotlin
// BitmapFactory を使わない予算判定を JVM UnitTest から検証するため internal に置いている。
// Java 利用者向けの public API ではない。
@JvmSynthetic
internal fun ensureWithinBitmapBudget(width: Int, height: Int) {
    // ...
}
```

### overload

引数違いの overload を手で並べない。デフォルト引数と `@JvmOverloads` を使う。Java からは
従来どおり複数 overload に見える。

### 例外

- 入力不正は `IllegalArgumentException`。`require` を使い、メッセージに値域を含める。
- ライブラリ内部の失敗は `QrForgeException` のいずれかに分類する。分類の対応は
  [api-design.md](api-design.md) が正典。
- 汎用 `RuntimeException` を投げない。利用者が分岐できなくなる。
- 下位層の例外を包むときは `cause` を必ず渡す。
- 包むときのメッセージ null は既定文言でフォールバックする。空メッセージの例外を作らない。

```kotlin
throw QrForgeException.GenerationFailed(error.message ?: "QR generation failed", error)
```

- `OutOfMemoryError` のように通常は捕捉しない `Error` でも、発生条件を特定できて型付き例外に
  変換できるなら捕捉してよい。`createBitmap` のメモリ上限ガードがその例。

### 数値

大きな値を掛ける前に、除算で比較してオーバーフローを避ける。

```kotlin
// width * height * 4 の乗算前に除算で比較し、Long オーバーフローを回避する。
val maxPixels = MAX_BITMAP_BYTES / BITMAP_BYTES_PER_PIXEL
if (width.toLong() * height.toLong() > maxPixels) { /* ... */ }
```

## Rust core

### 失敗の扱い

crate 先頭で `unwrap` と `expect` を禁止している。

```rust
#![deny(clippy::unwrap_used, clippy::expect_used)]
```

失敗はすべて `Result<_, QrForgeError>` で返す。`QrForgeError` には `Display` と
`Error::source` を実装し、下位のエラーを `source` で辿れるようにする。`From` 実装を用意して
`?` で変換できるようにする。

### 依存

- Android・JNI・Kotlin の型や概念を持ち込まない。この crate は `cargo test` だけで検証できる状態を保つ。
- 依存 crate の feature は必要最小限にする。使っていない feature は削る。
  例: `qrcode` の `image` feature は `render::image` 用で、本 crate は `Vec<u8>` へ直接描画し、
  `png` crate で encode するため不要。有効にすると使わない renderer と依存が増える。

### 値域と文言

値域の定数はこの crate が正典。エラー文言は定数から組み立て、literal を直書きしない。

```rust
return Err(QrForgeError::InvalidOptions(format!(
    "QR image size must be between {MIN_IMAGE_SIZE} and {MAX_IMAGE_SIZE} pixels"
)));
```

定数を変えたら Kotlin 側 `QrOptions` も同時に更新する（[api-design.md](api-design.md) の同期表）。

### 画素操作

ピクセル単位の書き込みループを書かない。L8 は 1 ピクセル 1 バイトなので、`Vec<u8>` の
行スライスをまとめて `fill` する。最大構成では 1 千万回規模の差になる。

```rust
for y in start_y..start_y + module_size {
    let row_start = y * image_width + start_x;
    pixels[row_start..row_start + module_size].fill(0);
}
```

## Rust JNI bridge

QR 生成や PNG 加工のロジックを書かない。型変換とエラー変換だけを担う。

### panic を越境させない

Rust の panic を Java 側へ unwind させない。core の呼び出しは `catch_unwind` で包み、
panic を捕まえたら Java 例外に変換する。

```rust
let result = catch_unwind(|| generate_qr_png(&text_str, &options));
```

### 例外送出の規約

- 例外送出は 1 つのヘルパーに集約する。呼び出しごとに `throw_new` を書かない。
- JNI 例外が既に pending なら上書きしない。元の例外を伝播させる。
- 例外を投げたら戻り値は `std::ptr::null_mut()` を返す。
- `throw_new` 自体が失敗したら `fatal_error` に落とす。握り潰さない。

```rust
fn throw_or_fatal(env: &mut JNIEnv<'_>, class: &str, message: &str) {
    if matches!(env.exception_check(), Ok(true)) {
        return;
    }
    if env.throw_new(class, message).is_err() {
        env.fatal_error(message);
    }
}
```

### 型変換

- `jint` → `u32` は負値を拒否してから変換する。ここで保証するのは変換の安全性だけで、
  値域の妥当性は core の `validate_options` に任せる。二重に値域チェックを書かない。
- Java 例外クラス名は定数にする。文字列を各所に散らさない。

```rust
const ILLEGAL_ARGUMENT_EXCEPTION_CLASS: &str = "java/lang/IllegalArgumentException";
```

## sample app

SDK の利用例と手動確認 UI に留める。JNI の直接呼び出し、`System.loadLibrary`、QR 生成ロジックを
置かない。ここに書きたくなったコードは wrapper 側に足りない API のサインとして扱う。

## 命名

| 対象 | 名前 |
|------|------|
| 公開入口 | `QrForge` |
| option model | `QrOptions` |
| SDK 例外 | `QrForgeException` |
| 内部 native binding | `QrForgeNative` |
| Rust core crate | `qrforge-core` |
| Rust JNI crate | `qrforge-jni` |

避ける名前: `NativeLib`、`JniHelper`、public API としての `RustBridge`、意味の狭まらない `generate`。

## 書いたあと

- テストの方針は [unit-test.md](unit-test.md) に従う。
- 確認コマンドは [setup.md](setup.md) の「変更後の確認フロー」から選ぶ。
- `rust/` を触ったなら `.so` の再ビルド要否を判断する。判断しないまま Android 側の結果を報告しない。
