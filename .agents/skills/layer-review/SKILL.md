---
name: layer-review
description: >
  QrForge の変更をレビューするときに使うスキル。レイヤー責務境界、public API の互換性、
  JNI 境界の安全性、検証結果の妥当性を見て、docs/review-rules.md の分類とフォーマットで
  指摘する。GitHub PR 一般のレビューではなく、このリポジトリ固有の観点を扱う。
---

## 手順

1. [docs/review-rules.md](../../../docs/review-rules.md) を読む
2. 差分を読み、変更レイヤーに関係する docs を読む（下記「読む docs」参照）
3. [docs/review-rules.md](../../../docs/review-rules.md) の「見る順序」に沿って確認する
4. [docs/review-rules.md](../../../docs/review-rules.md) の分類とフォーマットで、重要度順に指摘する
5. 確認した観点と残るリスクを書く

## 読む docs

| 状況 | 読む docs |
|------|-----------|
| 常に | [docs/review-rules.md](../../../docs/review-rules.md) |
| レイヤー責務・依存方向を判断する | [docs/architecture.md](../../../docs/architecture.md) |
| 実装ルール違反かを判断する | [docs/coding-rules.md](../../../docs/coding-rules.md) |
| public API・例外の変更がある | [docs/api-design.md](../../../docs/api-design.md) |
| テストの追加・変更がある | [docs/unit-test.md](../../../docs/unit-test.md) |
| 確認コマンドの結果が妥当かを判断する | [docs/setup.md](../../../docs/setup.md) |

## 出力

[docs/review-rules.md](../../../docs/review-rules.md) の「出力フォーマット」に従う。指摘がない分類には `該当なし` と書く。

推測は推測として書き、見ていないファイルの内容を断定しない。
