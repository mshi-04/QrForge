---
name: layer-review
description: >
  QrForge の変更をレビューするときに使うスキル。差分・ブランチ・PR を「レビューして」「見て」
  と言われたとき、実装したあとに自分の変更を点検するときに使う。レイヤー責務境界、public API
  の互換性、JNI 境界の安全性、検証結果の妥当性を見て、`docs/review-rules.md` の分類と
  フォーマットで指摘する。GitHub PR 一般のレビューではなく、このリポジトリ固有の観点を扱う。
---

## 手順

1. レビュー対象の差分を確定する（下記「対象の確定」参照）
2. [docs/review-rules.md](../../../docs/review-rules.md) を読む
3. 差分を読み、変更レイヤーに関係する docs を読む（下記「読む docs」参照）
4. [docs/review-rules.md](../../../docs/review-rules.md) の「見る順序」に沿って確認する
5. [docs/review-rules.md](../../../docs/review-rules.md) の「事実確認」に照らし、断定と推測を分ける
6. [docs/review-rules.md](../../../docs/review-rules.md) の分類とフォーマットで、重要度順に指摘する
7. 確認した観点と残るリスクを書く

## 対象の確定

対象を取り違えたレビューは、個々の指摘が正しくても出力全体が無意味になる。しかも「確認した
観点」にはそれらしく並ぶため、後から読んで気づけない。だから先に決めて、出力の冒頭に書く。

| 依頼のされ方 | 対象 |
|-------------|------|
| PR 番号の指定がある | その PR の差分 |
| 「このブランチ」「今回の変更」 | `git diff main...HEAD` |
| 「今書いたところ」「作業中の分」 | 未コミットの差分（`git diff` と `git diff --staged`） |

判断がつかないときは確認する。コミット済みと未コミットが混在している場合は、どちらを対象に
含めたかを明記する。

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

冒頭に、確定したレビュー対象（PR 番号、比較対象、コミット範囲）を 1 行で書く。読み手が範囲の
妥当性を検証できるようにするため。
