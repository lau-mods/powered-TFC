# Powered TFC 開発ガイド

製品仕様と Acceptance Criteria の正本は `PROJECT.md`。
計画は `.harness-state/plan.json`、進捗は `.harness-state/progress.json` に保存する。
Mod ID は `poweredtfc`、Java package は `net.claustra01.poweredtfc`。
Create 6.0.10、TFC 4.2.12、Patchouli 1.21.1-93-NEOFORGE を必須依存とし、Powered TFC はこれらの後に読み込む。
Blaze Burner の server tick は直上のTFC受熱設備へ Smouldering 80℃、Fading 1100℃、Kindled 1300℃、Seething 1800℃を供給する。
水平に送風する Encased Fan は20 tickごとにBellows互換位置へ `100 + |RPM|` の空気を供給し、停止中と垂直送風時は供給しない。
Converter は `FACING` 側を Create 接続、反対側を TFC 接続として `RPM × 2π / 1200` で双方向変換し、Redstone 通電中は停止する。
両 Converter の通常・通電モデルは `minecraft:cutout_mipped` で描画し、筐体の軸周囲の透明部分が設置時に黒くならないようにする。
Sequenced Assembly のBloom加工は921.00006℃以上で開始し、3 Pressing工程を3周する。TFC温度を持つ主入力は中間品へ温度と熱容量を継承し、完成時はTFC加熱定義を持つ完成品だけへ現在温度を継承する。非加熱完成品には熱情報を付与しない。
Display Link の情報源 `poweredtfc:tfc_crucible` は温度数値とTFC温度色、`poweredtfc:tfc_blast_furnace` は温度数値、TFC温度色、投入物数、触媒数、燃料数、Compactを提供し、20 tickごとに設備状態を読み直す。
Basin の液体入力は独立した4槽（各1000 mB）とし、Basin、Mixing、Compacting レシピは最大4種類の液体入力を受け付ける。液体出力は従来どおり2槽（各1000 mB）、最大2種類とし、既存の2槽分の入力を保存した Basin は追加槽を空で読み込む。
Create 6.0.10 の699 item registry IDとConverter 2 IDは、v0.4.3の値に基づき、`data/poweredtfc/tfc/item_size/create_<size>_<weight>.json` から対応する `poweredtfc:item_size/create_<size>_<weight>` tagを参照して、それぞれ厳密に1つのTFC Size／Weightへ分類する。
COMMON config の8項目は `blaze_burner.*`、`encased_fan.*`、`mechanical_converters.*` に置き、値は動作時に読み直す。

## Harness runtime

- Minecraft 1.21.1、NeoForge 21.1.264、Java 21 を使用する。
  `PROJECT.md` と `gradle.properties` の NeoForge バージョンを揃える。
- runtime は Git 管理外の `.harness-state/runtime/` に置く。
  MC Pilot CLI 0.20.2 は `pilot/node_modules/.bin/mct`、専用サーバーは `server/`。
  CLI の `data/variants.json` の `1.21.1-neoforge` は 21.1.264 に合わせる。
- MC Pilot の環境変数は `MCT_HOME=$PWD/.harness-state/runtime/mct-home`、
  `MCT_CACHE_DIR=$PWD/.harness-state/runtime/mct-cache`、`MCT_SKILL_TARGETS=none`。
  クライアント名は `poweredtfc-a`、offline account は `poweredtfc_a`。
- サーバーの接続先は `127.0.0.1:25581`。`server-ip=127.0.0.1` でローカル接続に限定する。
  検証ワールドは `harness-world`。Harness は E2E ごとにこのワールドを再作成する。
- Create 6.0.10、TFC 4.2.12、Patchouli 1.21.1-93-NEOFORGE を
  サーバーとクライアントの `mods/` に配置する。
  Create は Flywheel と Ponder を内包した配布 JAR を使用する。
- macOS のクライアントには CPU architecture に合う Java 21 を使用する。
  クライアント生成時の `mct client create` に `--java` で指定する。
- TFC の初回接続に備え、サーバーの `user_jvm_args.txt` と
  クライアントの `instance.json` の `env.JAVA_TOOL_OPTIONS` に
  `-Dneoforge.readTimeout=120` を指定する。両者の最大 heap は 3 GiB。
- `npm --prefix .harness run build` で Harness をビルドする。
  `node .harness/dist/cli/main.js doctor` と `validate` で構成を確認する。
  CLI 認証を確認できる環境で `plan` を実行し、計画と初期進捗を commit する。
- runtime の確認は専用サーバーとクライアントを起動し、ワールド参加まで確認して停止する。
  製品機能の GameTest / E2E 成功とは区別する。

## 実装と検証

- 依存 Mod の仕様は対象バージョンのソースまたは JAR/data で確認する。
- 解析物や補助スクリプトは Git 管理外の `.tmp/` に置く。
  依存 JAR、runtime、ビルド生成物を commit しない。
- 製品コードの途中確認は `./gradlew compileJava`、完了確認は `./gradlew build`。
- GameTest は `./gradlew runGameTestServer` で実行する。最大heapは3 GiBで、JUnit XMLは `run/gametest-results.xml` に出力する。
  JSON を変更した場合は構文と参照先の整合性も確認する。
- ユーザーに見える挙動、依存関係、ID、検証手順を変更した場合は本書も更新する。
