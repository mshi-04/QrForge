---
name: unit-test
description: >
  QrForge のテストを追加・修正するときに使うスキル。テストを書き足したい、落ちているテストを
  直したい、`cargo test` / `testDebugUnitTest` / `connectedDebugAndroidTest` のどれで確かめるか
  迷う、Rust core test と JVM UnitTest と Instrumented Test のどこに置くか迷う、といった依頼で
  使う。置き場所を決め、native library の鮮度と実行可否を確かめてから、実行結果と未実施を
  報告する。テストではなく実装側を直すのが主目的なら `coding` を使う。
---

## 手順

1. 検証したい振る舞いを 1 文で言語化する
2. [docs/unit-test.md](../../../docs/unit-test.md) の配置と境界に従って置き場所を決める
3. [docs/unit-test.md](../../../docs/unit-test.md) の書き方に従って書く
4. [docs/setup.md](../../../docs/setup.md) で `.so` の鮮度、端末要否、実行コマンドを確認する
5. 実行する
6. 報告する（下記「報告」参照）

途中で、落ちている原因がテストではなく実装側だと分かったら [coding skill](../coding/SKILL.md) の
手順に切り替える。[docs/coding-rules.md](../../../docs/coding-rules.md) を読まずに実装へ手を入れると、
テストを通すためだけの修正になりやすい。

## 読む docs

| 迷っていること | 読む docs |
|---------------|-----------|
| 置き場所、境界、書き方 | [docs/unit-test.md](../../../docs/unit-test.md) |
| その振る舞いがどのレイヤーの責務か | [docs/architecture.md](../../../docs/architecture.md) |
| 期待値が公開 API の契約に沿っているか（`size` は最小値であって出力寸法ではない、など） | [docs/api-design.md](../../../docs/api-design.md) |
| テスト都合の `internal` や可視性の扱い | [docs/coding-rules.md](../../../docs/coding-rules.md) |
| `.so` の鮮度、端末要否、実行コマンド | [docs/setup.md](../../../docs/setup.md) |

## 報告

1. 追加・変更したテストと、その置き場所を選んだ理由
2. 実行したコマンドと結果（件数を含む）
3. 実行しなかった確認と、その理由
4. そのテストで担保**できていない**振る舞い

新しく書いたテストが一度も実行できていない場合は、その旨を明記する。実行できていないテストは
「書いた」だけで、まだ何の振る舞いも固定していない。JVM UnitTest が緑でも `.so` を解決しない
実行なので、native 経路が動く証拠にはならない。
