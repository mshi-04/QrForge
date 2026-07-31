---
name: unit-test
description: >
  QrForge のテストを追加・修正するときに使うスキル。Rust core test / Kotlin UnitTest /
  Instrumented Test のどこに置くかを決め、native library の鮮度と実行可否を確かめてから、
  実行結果と未実施を報告する。
---

## 手順

1. 検証したい振る舞いを 1 文で言語化する
2. 置き場所を決める（下記「どこに書くか」参照）
3. `docs/unit-test.md` の書き方に従って書く
4. 実行前の前提を確認する（下記「実行前に確認すること」参照）
5. 実行する
6. 報告する（下記「報告」参照）

## どこに書くか

配置表と書き方の規則は `docs/unit-test.md` にある。ここでは判断の入口だけ置く。

1. **native library や Android SDK が要るか**
   → 要る（native library load、JNI 呼び出し、`Bitmap` decode）なら `qrforge/src/androidTest/`
2. 要らないとして、**検証対象は QR 生成・PNG bytes・option validation の中身か**
   → そうなら `rust/qrforge-core/tests/`
3. 要らないとして、**検証対象は Kotlin wrapper の入力検証・option model・例外変換前の分岐か**
   → そうなら `qrforge/src/test/`

迷ったら `docs/unit-test.md` の「3 つの置き場所」と「境界」を読む。

特に、**native library が無いことによる例外を正常系の証拠に使わない**（`docs/unit-test.md`
の「境界」参照）。JVM UnitTest で「入力が受理される」ことを確かめたいときは、入力検証そのものを
直接テストする。

## 実行前に確認すること

- 直前に `rust/` を変更しているなら、instrumented test を実行する前に `.so` を再ビルドする。
  していないと変更前の native library を検証することになる（`docs/setup.md`）。
- `connectedDebugAndroidTest` は実機かエミュレーターが要る。無ければ未実施として報告する。
- 特定のテストクラスだけ回す場合のコマンド形式は `docs/setup.md` にある。

## 報告

1. 追加・変更したテストと、その置き場所を選んだ理由
2. 実行したコマンドと結果（件数を含む）
3. 実行しなかった確認と、その理由
4. そのテストで担保**できていない**振る舞い

新しく書いたテストが一度も実行できていない場合は、その旨を明記する。
