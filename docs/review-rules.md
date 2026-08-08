# レビュールール

指摘の分類、見る順序、出力フォーマット。判断の根拠となる規則そのものは各文書にあるので、
ここでは繰り返さず参照する。

レビューは、コード・差分・テスト結果・docs で**事実確認できること**に基づいて行う。

## 指摘分類

重要度順に出力する。指摘がない分類には `該当なし` と書く。

| 分類 | 使う場面 |
|------|----------|
| `Critical` | crash、memory safety、公開 API の破壊、責務境界の重大な崩れ、QR 生成の主要な失敗 |
| `Suggestion` | 設計一貫性、保守性、テスト容易性、利用者体験を改善すべき問題 |
| `Nitpick` | 動作影響が小さい命名、文体、軽微な整合性 |

分類に迷ったら「利用者に届く挙動が変わるか」で判断する。変わるなら `Critical` か `Suggestion`、
変わらないなら `Nitpick`。

## 見る順序

上から順に見る。下位の指摘を大量に出す前に、上位が通っているかを確かめる。

### 1. 責務境界

判断基準は [architecture.md](architecture.md)。

- Rust core が Android・JNI・Kotlin に依存していないか。
- JNI bridge が型変換とエラー変換だけを担っているか。生成ロジックが漏れていないか。
- sample app が wrapper 以外の内部 API を呼んでいないか。
- `NativeQrGenerator`、`external fun`、JNI symbol、`System.loadLibrary` が公開 API や利用例に出ていないか。

### 2. public API の互換性

判断基準は [api-design.md](api-design.md)。

- 既存 API の関数名、引数、戻り値型、例外の意味、入力の解釈が変わっていないか。
- 入力不正・生成失敗・decode 失敗・native library load failure の 4 分類を混ぜていないか。
- `null`、空配列、汎用 `RuntimeException` で失敗を曖昧にしていないか。
- `QrOptions` の定数を変えた場合、Kotlin と Rust core の両方が更新されているか。

### 3. 実装ルール

判断基準は [coding-rules.md](coding-rules.md)。

- Rust に `unwrap` / `expect` が入っていないか。
- JNI で panic が越境し得ないか。例外送出後に戻り値を返していないか。pending 例外を上書きしていないか。
- エラー文言が定数から生成されているか。literal 直書きに戻っていないか。
- テスト都合の `internal` にその旨のコメントがあるか。

### 4. 公開・配布

判断基準は [api-design.md](api-design.md) の「互換性」と [setup.md](setup.md) の
「Maven Central への公開」。

- version の変更種別が Semantic Versioning と public API の互換性に合っているか。
- tag 由来の version が `VERSION_NAME` と Maven 座標へ明示的に渡り、未指定や形式違反で
  fail-fast するか。
- release commit が protected `develop` に含まれ、成功済み CI と同じ commit であることを
  workflow が検証するか。
- release source から 3 ABI の `.so` を再ビルドし、その成果物で AAR を作るか。
- Central Portal の namespace、tag ruleset、`release` environment、environment secrets の有効性を
  source code だけで確認済みと断定していないか。外部設定の確認結果か未確認を報告しているか。
- 公開済み version と release tag を上書き・移動・再利用する手順になっていないか。

### 5. 検証の妥当性

**「テストが通った」で止まらない。** その結果が今回の変更を検証できているかまで見る。

- `rust/` に変更があるのに `qr-forge/src/main/jniLibs/arm64-v8a/libqrforge.so`、
  `qr-forge/src/main/jniLibs/armeabi-v7a/libqrforge.so`、
  `qr-forge/src/main/jniLibs/x86_64/libqrforge.so` が差分に無い場合、ローカルと CI の instrumented test は
  **コミット済みの変更前**の native library に対するもの。CI の `rust` job は
  現在の source から 3 ABI を別途ビルドするが、コミット済み `.so` との一致までは検証しないため、
  両 job が緑でも `.so` が古い可能性は残る。
- そのテストが今回の誤りを検出できるかを見る。PNG のヘッダと寸法しか見ないテストは、描画位置や
  塗り潰し範囲の誤りを検出しない。
- 環境要因で実行できなかった確認が、実施済みとして書かれていないか。
- `.so` が更新されているなら、3 ABI すべてが揃っているか。

### 6. テストと確認の十分さ

判断基準は [unit-test.md](unit-test.md) と [setup.md](setup.md)。

- 変更レイヤーに対応するテストがあるか。置き場所は適切か。
- native library の不在を正常系の証拠に使っていないか。
- 変更レイヤーに対応する確認コマンドが実行されているか。

## 事実確認

- 推測は推測として書く。断定と区別する。
- 実行していないテストを「通った」と書かない。
- 見ていないファイルの内容を断定しない。
- 既存設計と異なる指摘には、根拠となる docs か既存コードを示す。
- 指摘を思いつかない領域を「問題なし」と書かない。見ていないなら見ていないと書く。

## 出力フォーマット

```text
### Critical

該当なし

### Suggestion

1. 対象ファイル: path/to/File.kt
   該当箇所: 関数名、行番号、差分ブロックなど
   問題点: 何が問題か
   理由: なぜ問題なのか
   修正案: 具体的にどう直すか

### Nitpick

該当なし

### 確認した観点

- 責務境界
- public API の互換性
- 検証の妥当性

### 残るリスク

- 実行できなかった確認、未検証の環境、外部仕様の確認が必要な点
```
