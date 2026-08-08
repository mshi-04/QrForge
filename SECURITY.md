# Security Policy

## Supported versions

Security fixes are applied to the latest released version of `io.github.lambdarc:qr-forge`.
Older versions are not patched; upgrade to the latest release instead.

## Reporting a vulnerability

Report vulnerabilities through
[GitHub private vulnerability reporting](https://github.com/lambdarc/qr-forge/security/advisories/new).
Do not open a public issue or pull request for a suspected vulnerability.

Please include the affected version, the platform and ABI, and a reproduction — a QR input string
and the `QrOptions` values are usually enough.

You can expect an acknowledgement within 7 days. Once a fix is ready it ships in a new release, and
the advisory is published after that release is available on Maven Central.

## Scope

QrForge decodes untrusted text into a QR image across a JNI boundary, so the following are in scope.

- Memory safety issues in the Rust core or the JNI bridge, including panics that cross the boundary
- Crashes or unbounded allocation triggered by attacker-controlled input text or `QrOptions`
- Incorrect output that misrepresents the encoded payload

Vulnerabilities in the sample app under `app/` are out of scope; it exists only to exercise the
library.

---

# セキュリティポリシー

## サポート対象バージョン

セキュリティ修正は `io.github.lambdarc:qr-forge` の最新公開バージョンへ適用する。
過去のバージョンへは backport せず、最新版への更新を案内する。

## 脆弱性の報告

脆弱性は
[GitHub private vulnerability reporting](https://github.com/lambdarc/qr-forge/security/advisories/new)
から報告する。公開 issue や pull request には書かない。

影響するバージョン、動作環境と ABI、再現手順を添える。QR の入力文字列と `QrOptions` の値が
あれば足りることが多い。

7 日以内に受領を返す。修正は新しい release へ含め、Maven Central で参照できるようになってから
advisory を公開する。

## 対象範囲

QrForge は信頼できない文字列を JNI 境界越しに QR 画像へ変換するため、次を対象とする。

- Rust core と JNI bridge のメモリ安全性の問題。境界を越える panic を含む
- 入力文字列や `QrOptions` によって誘発される crash、上限のないメモリ確保
- 符号化した内容と一致しない出力

`app/` のサンプルアプリはライブラリの動作確認用であり、対象外とする。
