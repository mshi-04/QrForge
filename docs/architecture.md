# アーキテクチャ

レイヤの分け方と、どこに何を書くかの正典。実装の書き方は [coding-rules.md](coding-rules.md)、
公開 API の契約は [api-design.md](api-design.md) を見る。

## 何を実現する構造か

利用者には Kotlin の SDK だけを見せ、Rust・JNI・NDK の存在を意識させない。同時に、QR 生成
そのものは Android から独立してテスト・再利用できるようにする。この 2 つを両立させるために
レイヤを 4 つに分ける。

## レイヤと依存方向

```text
app (sample)
  └→ Kotlin wrapper        qr-forge/src/main/java/io/github/lambdarc/qrforge/
       └→ Kotlin JNI binding  qr-forge/src/main/java/io/github/lambdarc/qrforge/internal/
            └→ Rust JNI bridge   rust/qr-forge-jni/
                 └→ Rust core       rust/qr-forge-core/
```

依存は上から下の一方向のみ。Rust core は Android・JNI・Kotlin を知らない。

| レイヤ | 配置 | 成果物 |
|--------|------|--------|
| sample app | `app/` | 動作確認用 APK |
| Kotlin wrapper | `qr-forge/src/main/java/io/github/lambdarc/qrforge/` | public API |
| Kotlin JNI binding | `.../qrforge/internal/` | `internal` な native binding |
| Rust JNI bridge | `rust/qr-forge-jni/` | `libqrforge.so`（cdylib） |
| Rust core | `rust/qr-forge-core/` | rlib（Android 非依存） |

`libqrforge.so` は `qr-forge/src/main/jniLibs/<abi>/` に置く。`app/src/main/jniLibs` には置かない。

## 生成フロー

`QrGenerator.createBitmap(text, options)` が何を通るか。責務境界を判断するときはこの流れを基準にする。

```text
QrGenerator.createBitmap
├─ QrGenerator.createPngBytes
│  ├─ require(text.isNotBlank())      Kotlin wrapper : blank を IllegalArgumentException で拒否
│  └─ NativeQrGenerator.generateQrPng    Kotlin binding : library load と UnsatisfiedLinkError 変換
│     └─ nativeGenerateQrPng         JNI bridge
│        ├─ env.get_string           JString → String
│        ├─ options_from_jni         jint → u32（負値のみ拒否）
│        ├─ catch_unwind
│        │  └─ generate_qr_png       Rust core
│        │     ├─ blank 判定
│        │     ├─ validate_options   値域検証
│        │     ├─ QrCode::new        QR エンコード
│        │     ├─ render_qr_image    RenderedQrImage（side_length + Vec<u8>）へ描画
│        │     └─ encode_png         png::Encoder で grayscale 8-bit PNG bytes へ変換
│        └─ byte_array_from_slice    Vec<u8> → jbyteArray
├─ ensureDecodableWithinBudget       Kotlin wrapper : 寸法だけ先読みしてメモリ予算を検査
└─ BitmapFactory.decodeByteArray     Kotlin wrapper : PNG → Bitmap (ARGB_8888)
```

## 各レイヤの責務

### Rust core

QR エンコード、`RenderedQrImage` が持つ `Vec<u8>` への grayscale pixel 描画、`png` crate による
PNG 生成、option の値域検証。

Android・JNI・Kotlin の型を持ち込まない。この制約があるおかげで、`cargo test` だけで生成
ロジックを検証できる。

値域の定数（`MIN_IMAGE_SIZE` など）はここが正典。Kotlin 側の `QrOptions` は写しであり、
変更時は両方を同時に更新する。

### Rust JNI bridge

型変換とエラー変換だけを行う。QR 生成や PNG 加工のロジックを置かない。

panic を Java 側へ unwind させないための境界でもある（`catch_unwind`）。

### Kotlin JNI binding

`System.loadLibrary` と `external fun` を閉じ込める `internal` 層。利用者向けの例外分類は
行わず、`UnsatisfiedLinkError` を扱いやすい型に変換するところまでを担う。

### Kotlin wrapper

public API、入力検証、`Bitmap` 変換、利用者向け例外の分類。

`Bitmap` のメモリ上限ガードのように「Android 固有の失敗を型付き例外に変える」処理はここに置く。
Rust core には持ち込まない。

### sample app

SDK の利用例と手動確認 UI のみ。JNI や内部 API を直接呼ばない。

## 入力検証が二重にある理由

blank text の拒否と option 値域の検証は、Kotlin wrapper と Rust core の両方にある。重複では
なく、それぞれ理由が違う。

| 層 | 目的 |
|----|------|
| Kotlin wrapper | 利用者に JNI 境界を跨がせずに `IllegalArgumentException` を返す |
| JNI bridge | `jint` → `u32` 変換の安全性のみを保証する（負値を弾く） |
| Rust core | crate 単体で正しく振る舞う。Kotlin を経由しない利用でも壊れない |

どれか 1 つを消すときは、消したあとも各層が単体で正しいかを確認する。

## 境界に迷ったときの判断

- **Android の型・API が必要か** → 必要なら Kotlin wrapper。不要なら Rust core。
- **QR や PNG の中身を触るか** → 触るなら Rust core。JNI bridge には置かない。
- **利用者に見せるか** → 見せないなら `internal` か Rust 側の非 `pub`。
- **どちらにも置ける** → 下位レイヤに置く。上位は薄いほど差し替えやすい。

## 公開範囲

公開するのは `QrGenerator`、`QrOptions`、`QrGenerationException` の 3 つだけ。

`NativeQrGenerator`、`external fun`、Rust の JNI symbol、`System.loadLibrary("qrforge")`、Rust
crate の内部型は、README・sample・利用者向け文書に出さない。

## 拡張するとき

- 新しい設定は `QrOptions` への property 追加で行う。既存 API の意味を変えない。
- エラー訂正レベル、前景色・背景色、出力画像形式などは、それぞれ独立した機能として分ける。
- 追加機能はまず「Rust core に置けるか」を検討する。置けるなら Kotlin 側は受け渡しだけになる。
- ABI を増減する場合は Rust target、`cargo ndk`、Android の `abiFilters`、README、
  [setup.md](setup.md)、CI を同時に見直す。
