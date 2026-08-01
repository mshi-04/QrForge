---
name: coding
description: >
  QrForge の実装・修正・リファクタリングを依頼されたときに使うスキル。Rust core / JNI bridge /
  Kotlin wrapper / sample app のどのレイヤーを触るかを決め、必要な docs だけを読み、native
  library の再ビルド要否と確認コマンドを判断して、実行結果と未実施を報告する。
---

## 手順

1. 依頼から影響を受けるレイヤーを特定する
2. 対象レイヤーに必要な docs だけを読む（下記「読む docs」参照）
3. 対象範囲・責務境界・変更予定ファイル・実行する確認コマンド・やらないことを提示する
4. 実装する
5. `rust/` を変更したなら [docs/setup.md](../../../docs/setup.md) に従って `.so` 再ビルドの要否を判断する
6. 変更レイヤーに対応する確認コマンドを実行する
7. 報告する（下記「報告」参照）

## 読む docs

skill には規則を書かない。判断が必要になった時点で該当の docs を読む。

| 迷っていること | 読む docs |
|---------------|-----------|
| どのレイヤーの仕事か、依存方向は正しいか | [docs/architecture.md](../../../docs/architecture.md) |
| 実装ルール、禁止事項、命名 | [docs/coding-rules.md](../../../docs/coding-rules.md) |
| public API、例外の分類、`QrOptions` 定数 | [docs/api-design.md](../../../docs/api-design.md) |
| テストを足す・直す | [docs/unit-test.md](../../../docs/unit-test.md)（詳しくは [unit-test skill](../unit-test/SKILL.md)） |
| 確認コマンド、`.so` 再ビルド、cargo の注意 | [docs/setup.md](../../../docs/setup.md) |

## 報告

1. 変更ファイルと、それぞれどのレイヤーか
2. 実行した確認コマンドと結果
3. 実行しなかった確認と、その理由（環境要因を含む）
4. 残るリスク・未検証の振る舞い

実行していない確認を「通った」と書かない。
