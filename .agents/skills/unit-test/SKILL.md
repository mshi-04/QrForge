---
name: unit-test
description: >
  QrForge のテストを追加・修正するときに使うスキル。Rust core test / Kotlin UnitTest /
  Instrumented Test のどこに置くかを決め、native library の鮮度と実行可否を確かめてから、
  実行結果と未実施を報告する。
---

## 手順

1. 検証したい振る舞いを 1 文で言語化する
2. [docs/unit-test.md](../../../docs/unit-test.md) の配置と境界に従って置き場所を決める
3. [docs/unit-test.md](../../../docs/unit-test.md) の書き方に従って書く
4. [docs/setup.md](../../../docs/setup.md) で `.so` の鮮度、端末要否、実行コマンドを確認する
5. 実行する
6. 報告する（下記「報告」参照）

## 報告

1. 追加・変更したテストと、その置き場所を選んだ理由
2. 実行したコマンドと結果（件数を含む）
3. 実行しなかった確認と、その理由
4. そのテストで担保**できていない**振る舞い

新しく書いたテストが一度も実行できていない場合は、その旨を明記する。
