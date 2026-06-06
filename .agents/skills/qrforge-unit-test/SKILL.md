---
name: qrforge-unit-test
description: QrForge の Rust core、Kotlin UnitTest、Instrumented Test を責務境界に沿って作成・修正し、適切な確認コマンドを選ぶための skill。
---

# QrForge UnitTest

## 手順

1. `docs/unit-test.md` を読み、テスト対象レイヤーに関係する docs も読む。
2. Android / JNI が必要かどうかで、Rust core test、Kotlin UnitTest、Instrumented Test を選ぶ。
3. 1 つのテストは 1 つの振る舞いと 1 つの主要 assert に絞る。
4. `docs/setup.md` から該当する確認コマンドを実行する。
5. 実行した確認と、環境都合で未実施の確認を報告する。

## 読む文書

- `docs/unit-test.md`
- `docs/architecture.md`
- `docs/coding-rules.md`
- `docs/setup.md`

public API の振る舞いや例外をテストする場合は `docs/api-design.md` も読む。

## 配置

| 対象 | 配置 | 役割 |
|------|------|------|
| Rust core | `rust/qrforge-core/tests/` | QR 生成、PNG bytes、option validation、core error |
| Kotlin UnitTest | `qrforge/src/test/` | JVM 上で検証できる Kotlin wrapper の入力検証、option model、例外変換前の分岐 |
| Instrumented Test | `qrforge/src/androidTest/` | native library load、JNI 呼び出し、Bitmap decode |

## ルール

- テスト名は英語にする。
- Kotlin テストには `Arrange`、`Act`、`Assert` コメントを置く。
- 例外テストでは `Act & Assert` を使ってよい。
- 主要 assert を helper に隠さない。
- Android / JNI の振る舞いを JVM UnitTest で検証しない。
- JNI exported symbol を public API として扱わない。

## 確認

`docs/setup.md` に従う。`connectedDebugAndroidTest` は実機またはエミュレーターが必要なので、使えない場合は未実施として報告する。
