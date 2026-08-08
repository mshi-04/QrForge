# ビルド・検証手順

この文書は「何をインストールし、何を変更したらどのコマンドを実行するか」だけを扱う。レイヤ構成は [architecture.md](architecture.md)、実装ルールは [coding-rules.md](coding-rules.md)、テストの置き場所と方針は [unit-test.md](unit-test.md) を参照する。

## 前提ツール

| ツール | 条件 | 備考 |
|--------|------|------|
| Android Studio | 最新安定版 | `compileSdk 37.1` / `minSdk 28` をビルドできること |
| JDK | 17 | Gradle build daemon・CI は Adoptium 17 を使用する |
| Android NDK | r27 以降 | `ANDROID_NDK_HOME` / `ANDROID_NDK_ROOT` を設定する |
| Rust toolchain | `rust-toolchain.toml` で固定 | `rustup toolchain install` で channel・component・target が揃う |
| cargo-ndk | native library ビルド時は必須 | `cargo install cargo-ndk --version 4.1.2 --locked` |
| cargo-deny | 依存監査をローカルで再現する場合 | `cargo install cargo-deny --locked` |
| Python | 3.10 以降 | repository 整合性 checker、公開 API checker に使用 |

ktlint は Gradle plugin として導入済みのため、個別インストールは不要。

Rust target を追加する。

```powershell
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add x86_64-linux-android
```

32-bit x86 emulator を対象にする場合のみ `rustup target add i686-linux-android` を追加する。

## 実行環境の禁止事項

この 2 点は環境を壊すため、例外なく守る。

- **cargo は PowerShell から実行する。** POSIX shell 経由だと Git 同梱の GNU `link` を MSVC の `link.exe` より先に解決し、リンクに失敗する。
- **`cargo clean` を実行しない。** host 用ビルドスクリプトのキャッシュが消え、MSVC Build Tools が無い環境では以後 `cargo build`・`cargo clippy`・`cargo ndk` がすべて失敗し、復旧できない。

## Rust の検証コマンド

```powershell
cargo fmt --all -- --check
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace --all-targets
cargo test -p qr-forge-core --doc
cargo build --manifest-path rust/qr-forge-jni/Cargo.toml
```

`clippy` は CI で `-D warnings` 付きで強制されるため、warning が 1 件でも残っていると落ちる。ローカルでも必ず通す。

## native library のビルド

`ANDROID_NDK_HOME` / `ANDROID_NDK_ROOT` を設定したうえで実行する。

```powershell
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o qr-forge/src/main/jniLibs build --release --manifest-path rust/qr-forge-jni/Cargo.toml
```

出力先は Android library module 配下で、repository にコミットする。

```text
qr-forge/src/main/jniLibs/arm64-v8a/libqrforge.so
qr-forge/src/main/jniLibs/armeabi-v7a/libqrforge.so
qr-forge/src/main/jniLibs/x86_64/libqrforge.so
```

既定 ABI は `arm64-v8a`・`armeabi-v7a`・`x86_64` の 3 つ。`x86` は 32-bit x86 emulator が必要になった場合のみ `-t x86` を足して追加する。sample app 側の `app/src/main/jniLibs` には配置しない。

## `.so` の鮮度（最も踏みやすい落とし穴）

`rust/` 配下を変更した時点で、`qr-forge/src/main/jniLibs/<abi>/libqrforge.so` は**変更前のビルドのまま**になる。Gradle は `.so` を自動生成しないため、この状態は静かに見逃される。

- 再ビルドせずに `connectedDebugAndroidTest` を通しても、検証しているのは変更前の native library であり、変更内容は一切確認できていない。
- 再ビルド後は差分で 3 ABI すべてが更新されたことを確認する。

```powershell
git diff --stat qr-forge/src/main/jniLibs
```

- AAR と instrumented test APK を生成し、両方に 3 ABI の `.so` が同梱されていることを確認する。

```powershell
.\gradlew.bat :qr-forge:assembleDebug :qr-forge:assembleDebugAndroidTest
jar tf qr-forge/build/outputs/aar/qr-forge-debug.aar `
  | Select-String '^jni/(arm64-v8a|armeabi-v7a|x86_64)/libqrforge\.so$'
jar tf qr-forge/build/outputs/apk/androidTest/debug/qr-forge-debug-androidTest.apk `
  | Select-String '^lib/(arm64-v8a|armeabi-v7a|x86_64)/libqrforge\.so$'
```

各コマンドで 3 ABI が 1 行ずつ表示されることを確認する。欠けている ABI があれば再生成・同梱は
未完了として扱う。

- 64bit ABI は 16 KB page size の端末で読み込めるよう、LOAD segment が 16 KB 境界に整列していなければ
  ならない。`armeabi-v7a` は 32bit のため対象外。

```powershell
python scripts/check_native_alignment.py
```

- 再ビルドしていない場合は、Android 側のテストが緑でも「native 側は未検証」として報告する。

CI の `rust` job は現在の source から 3 ABI を一時出力へビルドし、`instrumented` job は repository に
コミット済みの `x86_64` `.so` を使う。両方が通っても、source とコミット済み `.so` の一致は
証明されない。Rust 変更時はローカルで 3 ABI を再生成し、コミット対象の差分を確認する。

## Android の検証コマンド

```powershell
.\gradlew.bat :qr-forge:assembleDebug
.\gradlew.bat :qr-forge:testDebugUnitTest
.\gradlew.bat :qr-forge:assembleDebugAndroidTest
.\gradlew.bat :qr-forge:connectedDebugAndroidTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :qr-forge:ktlintCheck :app:ktlintCheck ktlintKotlinScriptCheck
```

| タスク | 内容 |
|--------|------|
| `:qr-forge:assembleDebug` | library module のビルド（AAR への `.so` 同梱を含む） |
| `:qr-forge:testDebugUnitTest` | JVM 上の UnitTest（JUnit5） |
| `:qr-forge:assembleDebugAndroidTest` | instrumented test の APK ビルドのみ。端末不要 |
| `:qr-forge:connectedDebugAndroidTest` | 実機・エミュレーター上で instrumented test 実行 |
| `:app:assembleDebug` | sample app のビルド |
| `ktlintCheck` / `ktlintKotlinScriptCheck` | Kotlin source と build script の format / style 検査 |

## 公開 API の互換性確認

`docs/api-design.md` の互換性契約を機械的に検査する。release AAR の bytecode から公開 API を
抽出し、コミット済みの `qr-forge/api/qrforge.api` と比較する。

```powershell
.\gradlew.bat :qr-forge:assembleRelease
python scripts/check_public_api.py
```

`internal` package と、Kotlin が name mangling する `internal` member・`$default` overload は
比較対象から外している。内部リファクタでは発火せず、公開 API の descriptor が変わったときだけ
落ちる。意図した変更なら snapshot を更新してコミットする。

```powershell
python scripts/check_public_api.py --update
```

更新前に、その変更が `api-design.md` の「互換性」節に照らして許容されるかを必ず判断する。
snapshot の更新は互換性を壊してよい理由にはならない。

`connectedDebugAndroidTest` は実機またはエミュレーターが必須。接続先が無い場合は黙って飛ばさず、「未実行」と理由を報告する。

## 利用側 R8 との互換性確認

Rust JNI bridge は `NativeQrGenerator` と `NativeQrGenerator$GenerationFailed` を literal な名前で
解決する。利用側アプリが R8 を有効にするとこれらは rename され、`throw_new` の失敗から
`fatal_error` に落ちて process abort する。これを防ぐため `qr-forge/consumer-rules.pro` を
公開 AAR へ同梱し、sample app を minify 有効の consumer として実際に縮小したうえで検査する。

```powershell
.\gradlew.bat :app:assembleRelease :qr-forge:assembleRelease
python scripts/check_consumer_proguard.py
```

検査は release APK の DEX から `class_defs` と `class_data_item` を解析し、次を確認する。
`type_ids` と `method_ids` には参照も含まれ、R8 が定義を消しても他所からの参照だけで残るため、
判定には定義側だけを使う。

- `NativeQrGenerator.nativeGenerateQrPng` が descriptor `(Ljava/lang/String;II)[B` で存在する
- `NativeQrGenerator$GenerationFailed` の `<init>(Ljava/lang/String;)V` が存在する
- `io.github.lambdarc.qrforge` 配下で DEX に残る型が、上記 2 つ**だけ**である
- release AAR の `proguard.txt` が `consumer-rules.pro` と一致する

3 番目は縮小が効いているかと keep 範囲の広さを同時に見る。縮小を切れば他の型が残り、keep を
広げすぎても他の型が残るため、どちらも失敗する。所有クラスと descriptor まで見るのは、
文字列の部分一致では `access$nativeGenerateQrPng` のような別シンボルでも通過してしまうため。

`app` の `isMinifyEnabled` は、この検査を成立させるために有効にしている。無効へ戻さない。

## 公開座標からの consumer 検証

`app` は `:qr-forge` を project dependency として使うため、公開メタデータ経由の解決は通らない。
`consumer-smoke/` はルートの build に含めない独立した Gradle build で、`mavenLocal()` から
`io.github.lambdarc:qr-forge` を座標で解決し、minify 有効で組み立てる。POM と Gradle Module
Metadata の不備、AAR 同梱 `proguard.txt` が利用側 R8 へ効かない状態は、この経路でしか検出できない。

独立した build のため Android SDK の位置は `ANDROID_HOME` から解決する。設定していない環境では
`consumer-smoke/local.properties` に `sdk.dir` を置く。

```powershell
.\gradlew.bat :qr-forge:publishToMavenLocal "-PVERSION_NAME=0.0.0" --no-configuration-cache
.\gradlew.bat -p consumer-smoke assembleRelease "-PQR_FORGE_VERSION=0.0.0"
python scripts/check_consumer_proguard.py consumer-smoke/build/outputs/apk/release/consumer-smoke-release-unsigned.apk
```

`check_consumer_proguard.py` は APK を引数で受け取ると、その APK だけを検査する。引数なしの
呼び出しは `app` の release APK と release AAR の `proguard.txt` を検査する。

`consumer-smoke/proguard-rules.pro` は空のまま維持する。keep rule を足すと、AAR 同梱の
consumer rule だけで JNI 契約が保たれているかを確かめられなくなる。

## instrumented test のクラス絞り込み

`--tests` は `connectedDebugAndroidTest` では効かない。instrumentation runner の引数を使い、PowerShell では引数全体を quote する。

```powershell
.\gradlew.bat :qr-forge:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=io.github.lambdarc.qrforge.QrGeneratorInstrumentedTest"
```

## 変更後の確認フロー

変更したレイヤーに対応する行をすべて実行する。複数レイヤーに跨る変更は該当行を積み上げる。

| 変更箇所 | 実行コマンド |
|---------|------------|
| Rust core（`rust/qr-forge-core/`） | `cargo fmt --all -- --check`、`cargo clippy --workspace --all-targets -- -D warnings`、`cargo test --workspace --all-targets`、`cargo test -p qr-forge-core --doc` |
| Rust JNI bridge（`rust/qr-forge-jni/`） | 上記に加えて `cargo build --manifest-path rust/qr-forge-jni/Cargo.toml` |
| `rust/` を変更して Android 側も確認する | 上記に加えて `.so` 再ビルドと `git diff --stat qr-forge/src/main/jniLibs`（「`.so` の鮮度」参照） |
| Kotlin wrapper（`qr-forge/`） | `.\gradlew.bat :qr-forge:assembleDebug`、`.\gradlew.bat :qr-forge:testDebugUnitTest` |
| Kotlin を変更した（レイヤ問わず） | `.\gradlew.bat :qr-forge:ktlintCheck :app:ktlintCheck ktlintKotlinScriptCheck` |
| 公開 API（`QrGenerator`・`QrOptions`・`QrGenerationException`） | 上記に加えて「公開 API の互換性確認」の 2 コマンド |
| JNI が名前で解決する class・method（`NativeQrGenerator`、`GenerationFailed`、`nativeGenerateQrPng`） | 上記に加えて「利用側 R8 との互換性確認」の 2 コマンド。`consumer-rules.pro` の keep 対象も合わせて更新する |
| Rust の依存を追加・更新した | `cargo deny check`、`python scripts/generate_third_party_notices.py` |
| Instrumented test | `.\gradlew.bat :qr-forge:assembleDebugAndroidTest`、可能なら `.\gradlew.bat :qr-forge:connectedDebugAndroidTest` |
| Sample app（`app/`） | `.\gradlew.bat :app:assembleDebug` |
| ABI 追加・削除 | `.so` 再ビルド、`abiFilters`、README・本文書・CI をまとめて更新 |
| docs のみ | `git diff --stat` でコード変更が混ざっていないことを確認 |

## Maven Central への公開

公開座標は `io.github.lambdarc:qr-forge:<version>`。release AAR、sources JAR、空の javadoc JAR、
POM、Gradle Module Metadata を `:qr-forge` から公開する。公開 API と native library は release AAR に
含まれるものを正典とし、sample app や Rust crate は Maven Central へ公開しない。

artifact ID、Gradle project/module、Rust package などの機械識別子には `qr-forge` を使う。POM の
`name` は利用者向けのブランド表示なので `QrForge`、sample app の表示名は `QrForge Sample` とする。

### 初回だけ必要な設定

1. Central Portal でアカウントを作成し、`io.github.lambdarc` namespace を登録・検証する。
2. artifact 署名用の GPG key pair を作成し、public key server へ公開する。
3. Central Portal の user token を作成する。
4. `v*` を対象にする active な tag ruleset を作り、tag の作成・更新・削除を制限する。release 担当者
   だけに bypass を許可する。
5. GitHub Actions に `release` environment を作り、required reviewer と `v*` tag だけを許可する
   deployment rule を設定する。可能なら workflow 実行者自身による承認と管理者 bypass を禁止する。
   承認できる維持者が 1 人しかいない間は self-review 禁止を有効にしない。environment の required
   reviewer には bypass の仕組みがなく、有効にすると tag を push した本人が承認できずリリースが
   止まる。2 人目の維持者が加わった時点で有効にする。
6. `release` environment に次の secrets を登録する。

| Secret | 内容 |
|--------|------|
| `MAVEN_CENTRAL_USERNAME` | Central Portal user token の username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user token の password |
| `SIGNING_IN_MEMORY_KEY` | ASCII armor 形式で export した GPG private key 全体 |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | GPG private key の passphrase。passphrase なしなら空文字列 |

secret は repository secrets、project の `gradle.properties`、workflow 本文へ直接書かない。

初回設定は tag を作る前にすべて完了させる。特に Central Portal の namespace、active な tag ruleset、
required reviewer 付きの `release` environment、environment secrets は workflow の source だけでは
有効性を確認できないため、GitHub と Central Portal の設定画面で確認する。

### リリース前チェック

公開 version は [api-design.md](api-design.md) のバージョニング方針に従って決める。開発は `develop` で
進め、公開する内容が揃った時点で `develop` を `main` へマージしてから tag を打つ。release commit は
最新の `origin/main` と一致し、作業ツリーが clean で、その commit の CI がすべて成功していること。

```powershell
$version = "1.0.0"
git fetch origin
git switch main
git pull --ff-only origin main
git status --short
git rev-parse HEAD
git rev-parse origin/main
git rev-list --count origin/main..origin/develop
```

`git status --short` は何も出力せず、2 つの commit ID は一致しなければならない。最後の commit 数が
`0` でなければ `develop` の内容が `main` へ届いておらず、公開しようとしているのは古い source になる。

publication は tag を作る前に、実際に公開する version で Maven Local へ出力し、成果物一式を確認する。

```powershell
.\gradlew.bat :qr-forge:assembleRelease :qr-forge:publishToMavenLocal `
  "-PVERSION_NAME=$version" `
  --no-configuration-cache
$artifacts = "$env:USERPROFILE/.m2/repository/io/github/lambdarc/qr-forge/$version"
Get-ChildItem $artifacts
Get-Content "$artifacts/qr-forge-$version.pom"
jar tf "$artifacts/qr-forge-$version.aar" `
  | Select-String '^jni/(arm64-v8a|armeabi-v7a|x86_64)/libqrforge\.so$'
python scripts/check_public_api.py
```

Maven Local に次の 5 つが揃っていることを確認する。

| 成果物 | 確認内容 |
|--------|----------|
| `qr-forge-$version.pom` | 座標、version、MIT license、SCM、developer 情報 |
| `qr-forge-$version.aar` | 3 ABI の `.so` が 1 行ずつ表示されること |
| `qr-forge-$version-sources.jar` | 生成されていること |
| `qr-forge-$version.module` | Gradle Module Metadata が生成されていること |
| `qr-forge-$version-javadoc.jar` | 生成されていること。`JavadocJar.Empty()` のため中身は空 |

release AAR の public API は `qr-forge/api/qrforge.api` と一致しなければならない。CI の `Verify Maven
publication` は `0.0.0` で POM と AAR の存在だけを確認するため、公開する version での確認はこの手順で行う。

### 公開手順

`develop` を `main` へマージして CI が成功し、公開する commit が確定したら `vMAJOR.MINOR.PATCH` 形式の
tag を push する。Release workflow が tag の先頭 `v` を除いた値を Maven version として渡し、署名後に
Central Portal へ upload・release する。Central は同じ座標・version の上書きを許可しないため、
公開済み tag は再利用しない。

workflow は公開前に次を検証する。

- tag が `vMAJOR.MINOR.PATCH` 形式であること
- tag の指す commit が remote から取得した `main` の最新 commit と一致すること
- その commit のすべての CI check run が success で終わっていること
- 公開 API が `qr-forge/api/qrforge.api` と一致すること

`main` の最新かどうかは、workflow が `main` を fetch した時点で判定する。その後の native build から
upload までの間に `main` が進んでも再判定しない。この検証が保証するのは「tag が古い commit を
指していないこと」であって、公開の瞬間まで `main` が動かないことではない。公開中は `main` への
merge を控える。

`.so` は commit 済みのものをそのまま公開せず、tag の source から 3 ABI を再ビルドして
`qr-forge/src/main/jniLibs/` を上書きしてから AAR を組み立てる。これにより、commit 済み `.so` が
古いまま Maven Central へ流れることはない（「`.so` の鮮度」参照）。

release tag は注釈付きで作成し、作成後に別の commit へ移動しない。

```powershell
git tag -a "v$version" -m "QrForge $version"
git push origin "v$version"
```

tag push 後は GitHub Actions の Release workflow を開き、`release` environment の承認対象、tag、commit
を照合してから承認する。workflow が success になるまでは GitHub Release を作成しない。

### 公開後チェック

1. Central Portal の deployment が `PUBLISHED` になったことを確認する。
2. Maven Central で `io.github.lambdarc:qr-forge:<version>` の POM、AAR、sources JAR、javadoc JAR、
   Gradle Module Metadata、署名と checksum が参照できることを確認する。
3. 既存 tag から GitHub Release の draft を作り、生成された release notes を確認して公開する。

```powershell
gh release create "v$version" `
  --verify-tag `
  --generate-notes `
  --title "QrForge $version" `
  --draft

gh release edit "v$version" --draft=false
```

Maven Central への反映には時間差があり得る。Release workflow の success だけで完了とせず、利用者が
公開座標を解決できるところまで確認する。

### 公開失敗時

- release tag を force push で移動しない。削除して同じ tag を作り直さない。
- Central Portal で一度でも deployment が作成された version は再利用しない。
- 原因を修正して `develop` から `main` へマージし直し、`main` の CI を通してから新しい patch version の
  tag で再実行する。
- GitHub Release を先に作っていた場合は公開を取り下げ、Maven Central で参照可能になってから再公開する。

資格情報と署名 key を設定した環境から手動公開する場合は、release workflow が自動で行う検証を手で行う。
公開する tag の commit を checkout し、作業ツリーが clean であることを確認してから 3 ABI を再生成する。

```powershell
$version = "1.0.1" # 例: 公開失敗後に選んだ未使用の patch version
git fetch --tags origin
git switch --detach "v$version"
git status --short
git rev-parse HEAD
git rev-parse "v$version^{commit}"
```

`git status --short` は何も出力せず、2 つの commit ID は一致しなければならない。tag の source から
`.so` を再生成し、公開する version で成果物を確認する。

```powershell
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -o qr-forge/src/main/jniLibs build --release `
  --manifest-path rust/qr-forge-jni/Cargo.toml
.\gradlew.bat :qr-forge:assembleRelease :qr-forge:publishToMavenLocal `
  "-PVERSION_NAME=$version" `
  --no-configuration-cache
python scripts/check_public_api.py
```

再生成した `.so` はコミット済みのものと差分が出得る。この差分は commit せず、「リリース前チェック」と
同じ 5 つの成果物を確認してから公開する。

```powershell
.\gradlew.bat :qr-forge:publishAndReleaseToMavenCentral `
  "-PVERSION_NAME=$version" `
  "-PsignAllPublications=true"
```

`$version` は release tag の `v$version` と生成 POM の version にも同じ値を使う。失敗した公開で使った
version や、Central Portal に deployment が存在する version は再利用しない。

`VERSION_NAME` を省略したローカル build は `1.0.0-SNAPSHOT` として扱う。`publish` で始まる task は
実行時に `VERSION_NAME` を必須とし、未指定なら artifact を送信する前に失敗する。`VERSION_NAME` を
指定した場合は task を問わず `MAJOR.MINOR.PATCH` 形式を要求し、空文字列や形式違反は configuration で
失敗する。公開前には生成された POM の座標、MIT license、SCM、developer 情報と、release AAR の
3 ABI を確認する。

## repository の整合性確認

Codex 用の `.agents/skills/` と Claude Code 用の `.claude/skills/` は、それぞれの仕組みに合わせて
独立して管理する。skill を変更しても他方へ機械的に同期しない。

```powershell
python scripts/check_repo_consistency.py
```

この文書では Python 3 の実行名を `python` と表記する。環境で `python3` として導入されている
場合は読み替える（CI は `python3` を使用する）。

整合性確認は Markdown の相対リンク、`abiFilters` と `qr-forge/src/main/jniLibs/` の ABI directory を
検証する。CI でも同じ checker を実行する。

## CI ジョブとローカルコマンドの対応

workflow は `.github/workflows/ci.yml`。`main`・`develop`・`feature/**` を base にする pull request と、
`main`・`develop` への push で実行する。stacked PR を CI 対象にするために `feature/**` を含める一方、
それ以外の base を除外して emulator job の実行を抑える。Java・Gradle の準備は
`.github/actions/setup-gradle-build` に集約し、release workflow と共有する。

ローカルで先に潰しておくべき対応は次のとおり。

| CI job | ローカルで相当するコマンド |
|--------|--------------------------|
| `consistency` | `python scripts/check_repo_consistency.py` |
| `rust` | `cargo fmt --all -- --check`、`cargo clippy --workspace --all-targets -- -D warnings`、`cargo test --workspace --all-targets`、`cargo test -p qr-forge-core --doc`、`cargo build --manifest-path rust/qr-forge-jni/Cargo.toml`、`cargo ndk`（3 ABI の `.so` 生成確認まで）、`python scripts/check_native_alignment.py` |
| `rust-audit` | `cargo deny check` |
| `kotlin-lint` | `.\gradlew.bat :qr-forge:ktlintCheck :app:ktlintCheck ktlintKotlinScriptCheck` |
| `android` | `.\gradlew.bat :qr-forge:assembleRelease` と `python scripts/check_public_api.py`、`.\gradlew.bat :qr-forge:publishToMavenLocal "-PVERSION_NAME=0.0.0"`、「公開座標からの consumer 検証」の 3 コマンド、`.\gradlew.bat :app:assembleRelease :qr-forge:assembleRelease` と `python scripts/check_consumer_proguard.py`、`.\gradlew.bat :qr-forge:testDebugUnitTest :qr-forge:assembleDebug :qr-forge:assembleDebugAndroidTest :app:assembleDebug` |
| `instrumented` | repository にコミット済みの `x86_64` `.so` を使う `.\gradlew.bat :qr-forge:connectedDebugAndroidTest`（CI は API 28 / 34 emulator） |

`kotlin-lint` の指摘は `.\gradlew.bat ktlintFormat` で自動修正できる。code style と適用しない
rule は `.editorconfig` に置く。

`rust-audit` は push・PR に加えて週次でも実行する。新しい advisory はコード変更なしに公開
されるため。GitHub は repository が 60 日間 inactive だと scheduled workflow を自動停止する。

`rust` job と `instrumented` job は別の成果物を検証する。コミット済みの `.so` が古いままでも
両方が通り得るため、ローカルでの再ビルドと差分確認を省略しない。
