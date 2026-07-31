---
name: coding
description: >
  QrForge の実装・修正・リファクタリングを依頼されたときに使うスキル。QR 生成ロジック、
  `QrOptions` の値域、PNG / Bitmap 出力、JNI の型変換と例外変換、`abiFilters` や ABI の追加、
  `libqrforge.so` の再ビルドなど、`rust/` `qrforge/` `app/` のコードに手を入れる依頼で使う。
  どのレイヤーを触るかを決め、必要な docs だけを読み、native library の再ビルド要否と確認
  コマンドを判断して、実行結果と未実施を報告する。テストの追加・修正が主目的なら `unit-test`、
  既存の変更を評価するだけなら `layer-review` を使う。
---

## 手順

1. 依頼から影響を受けるレイヤーを特定する
2. 対象レイヤーに必要な docs だけを読む（下記「読む docs」参照）
3. 対象範囲・責務境界・変更予定ファイル・実行する確認コマンド・やらないことを提示する
4. 実装する
5. `rust/` を変更したなら `docs/setup.md` に従って `.so` 再ビルドの要否を判断する。変更して
   いなくても Android 側の確認結果を報告するなら、コミット済み `.so` が現在の `rust/` と
   一致するかを確かめる
6. 変更レイヤーに対応する確認コマンドを実行する
7. 報告する（下記「報告」参照）

## 読む docs

skill には規則を書かない。判断が必要になった時点で該当の docs を読む。

| 迷っていること | 読む docs |
|---------------|-----------|
| どのレイヤーの仕事か、依存方向は正しいか | `docs/architecture.md` |
| 実装ルール、禁止事項、命名 | `docs/coding-rules.md` |
| public API、例外の分類、`QrOptions` 定数 | `docs/api-design.md` |
| テストを足す・直す | `docs/unit-test.md`（詳しくは `unit-test` skill） |
| 確認コマンド、`.so` 再ビルド、cargo の注意 | `docs/setup.md` |
| docs / AGENTS.md / skill 本文を編集した | `docs/setup.md`「repository の整合性確認」 |

## 報告

1. 変更ファイルと、それぞれどのレイヤーか
2. 実行した確認コマンドと結果
3. 実行しなかった確認と、その理由（環境要因を含む）
4. 残るリスク・未検証の振る舞い

実行していない確認を「通った」と書かない。CI やビルドが緑でも、コミット済み `.so` が古ければ
instrumented test は変更前の native library を検証している。「緑だった」という事実と「今回の
変更が検証された」という主張は別物なので、分けて書く。
