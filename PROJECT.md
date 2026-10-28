# Project

Status: active  
Project ID: poweredtfc
Mod ID: poweredtfc

## Platform

Minecraft: 1.21.1  
NeoForge: 21.1.264  
Java: 21

## Purpose

Minecraft 1.21.1において、TerraFirmaCraft（TFC）の熱・送風・回転動力・アイテムのサイズ／重量の仕組みと、Createの設備・加工・表示の仕組みを接続する互換Modを構築する。

外観およびデータ資産にはPowered TFC v0.4.3の既存アセットを使用する。一方、既存Modの内部実装は不明とし、既存のクラス構造、実装方法、処理順序を再現要件にしない。本書はプレイヤーから観察できる振る舞い、入出力、データ上の互換性および受入基準を定義する。

対象は、熱源、送風、双方向の機械動力変換、温度を保持するSequenced Assembly、製鉄加工、Display Link、Basinの液体入力拡張、およびTFCのアイテムサイズ・重量への対応とする。

## Features

### F-001: Blaze BurnerによるTFC設備の加熱

#### Description

CreateのBlaze BurnerをTFCの加熱源として使用する。Blaze Burnerの直上にある受熱可能なTFC設備に対し、現在の加熱段階に応じた熱を供給する。標準設定の供給温度はSmoulderingが80℃、Fadingが1100℃、Kindledが1300℃、Seethingが1800℃とする。加熱段階が無効である場合は熱を供給しない。

#### Acceptance Criteria

##### AC-F001-001: 加熱段階に応じた温度
Preconditions: 受熱可能なTFC設備がBlaze Burnerの直上にあり、加熱段階の変更および設備の温度確認が行える。  
Action: Blaze BurnerをSmouldering、Fading、Kindled、Seethingに順次設定し、各状態で十分な時間動作させる。  
Expected Result: 各段階で標準設定値80℃、1100℃、1300℃、1800℃に対応する熱供給が行われ、TFC設備の加熱挙動に反映される。実際の設備温度はTFC側の加熱・冷却条件に従う。

##### AC-F001-002: 受熱対象と停止状態
Preconditions: Blaze Burnerの上方および上方以外にTFC設備を配置でき、熱源を停止できる。  
Action: 加熱中と停止中の挙動を比較する。  
Expected Result: 熱供給対象は直上の受熱設備であり、停止中は追加の熱供給が行われない。無関係な位置にある設備は加熱されない。

### F-002: Encased FanによるTFC設備への送風

#### Description

回転中のCreate Encased Fanから、TFCで送風を受け付ける設備へ空気を供給する。対象となるのは水平方向に送風しているFanだけとし、TFCのBellowsに対応する配置関係で設備へ送風する。標準送風量は `100 + |回転速度（RPM）| × 1.0` とし、最終値は整数値で扱う。

#### Acceptance Criteria

##### AC-F002-001: 水平送風と回転速度
Preconditions: Fanを水平方向へ向け、TFCの送風受入設備を適切な位置へ設置している。  
Action: Fanを回転速度64 RPMおよび128 RPMで作動させる。  
Expected Result: 設備は送風を受け、標準設定ではそれぞれ164および228に相当する送風量が適用される。回転速度が大きいほど送風量も増える。

##### AC-F002-002: 送風対象外の状態
Preconditions: 同じ設備とFanを使用し、Fanの停止および向きの変更が行える。  
Action: 回転を停止し、続いて上下方向へ送風するようFanを配置する。  
Expected Result: 停止中および上下方向への送風中はTFC設備への送風効果が発生しない。

### F-003: CreateからTFCへの機械動力変換

#### Description

`poweredtfc:create_to_tfc_converter` を追加し、Createの回転動力をTFCの機械動力設備へ供給する。Create側の回転速度とTFC側の角速度は同じ物理的な回転速度に対応し、 `rad/tick = RPM × 2π / 1200` の関係を満たす。ブロックの向きによってCreate入力面とその反対のTFC出力面を定める。標準のCreate Stress Impactは64 SUとする。

外観、アイテム表示、設置状態、ドロップおよびレシピはv0.4.3の既存アセットを基準とする。クラフト材料はCreate Clutch 1個、Create Cogwheel 1個、`#tfc:axles` 1個とする。

#### Acceptance Criteria

##### AC-F003-001: 速度と接続方向
Preconditions: ConverterのCreate入力側へ安定した回転動力を接続し、反対側へTFCの回転設備を接続している。  
Action: 入力を120 RPMとして稼働させ、方向と出力を測定する。  
Expected Result: TFC側へ毎tick `π/5` radに相当する角速度が供給され、回転方向は設置向きに対して整合する。入力面と出力面を逆にした誤接続では変換しない。

##### AC-F003-002: 動力負荷とクラフト
Preconditions: CreateのStressを確認できる回転ネットワークと、指定材料を所持している。  
Action: Converterを接続して稼働させ、続いて指定材料でクラフトする。  
Expected Result: 標準設定で64 SUのStress Impactを持ち、shapeless recipeからConverterを1個作成できる。

##### AC-F003-003: TFCネットワークとの共存
Preconditions: 正常なTFC回転ネットワークおよびTFC標準規則に違反する接続構成を用意している。  
Action: 各構成へConverterを接続する。  
Expected Result: 正常な構成で動力が伝達され、不正な回転ネットワーク構成はTFCの既存の接続・競合規則に従って処理される。

### F-004: TFCからCreateへの機械動力変換

#### Description

`poweredtfc:tfc_to_create_converter` を追加し、直接隣接するTFC回転動力設備をCreateの動力源として使用する。変換式は `RPM = (rad/tick) × 1200 / 2π` とする。Createへの出力面はTFC入力面の反対側とし、標準のCreate Stress Capacityは64 SUとする。

外観、アイテム表示、設置状態、ドロップおよびレシピはv0.4.3の既存アセットを基準とする。クラフト材料はCreate Clutch 1個、Create Cogwheel 1個、Create Shaft 1個とする。

#### Acceptance Criteria

##### AC-F004-001: TFC動力の取込み
Preconditions: ConverterのTFC入力面に、該当面へ回転動力を出力するTFC設備が直接隣接し、Create出力面に回転ネットワークを接続している。  
Action: TFC設備を毎tick `π/5` radに相当する速度で回転させる。  
Expected Result: Create側が120 RPMに相当する速度で回転し、TFC側の回転方向と設置向きに整合する。

##### AC-F004-002: 応力容量とクラフト
Preconditions: TFCから有効な回転入力があり、CreateのStress Capacityを確認できる。指定材料を所持している。  
Action: Converterを動作させ、指定材料によるクラフトを行う。  
Expected Result: 動作中のCreate側Stress Capacityは標準設定で64 SUとなり、shapeless recipeからConverterを1個作成できる。

##### AC-F004-003: 無入力と直接循環の防止
Preconditions: TFC入力を取り外すか、Create→TFC Converterを直接入力面へ配置できる。  
Action: 両方の接続構成を試す。  
Expected Result: 有効なTFC動力入力がない場合はCreate側へ動力を生成せず、2種類のConverterだけを直接接続して持続的な動力生成を行う循環は成立しない。

### F-005: 動力ConverterのRedstone制御

#### Description

2種類のConverterはRedstone信号を受けた状態で変換を停止し、信号解除後には有効な機械動力の接続状況に応じて再開する。設置向きと通電状態を既存のblockstateアセットに合わせて表示する。

#### Acceptance Criteria

##### AC-F005-001: 通電中の停止
Preconditions: 各Converterが正常に回転動力を変換している。  
Action: 各ConverterへRedstone信号を入力する。  
Expected Result: 双方とも出力側の回転動力供給が停止し、Create側のStress消費または供給も停止状態に整合する。表示状態も通電状態と一致する。

##### AC-F005-002: 信号解除後の再開
Preconditions: Redstone信号によってConverterが停止しており、元の入力源は稼働している。  
Action: Redstone信号を解除する。  
Expected Result: 再設置を要さず変換が再開し、設置方向および入力速度に対応する出力を維持する。

### F-006: Sequenced AssemblyにおけるTFCの温度条件と温度継承

#### Description

CreateのSequenced Assemblyで、TFCの温度条件を満たすアイテムを主入力として受け付ける。温度条件の判定は工程開始時に実施する。工程中の中間アイテムは開始時の温度と熱容量を引き継ぎ、TFCの時間経過に従って自然冷却する。完成時はその時点の温度を、加熱可能な完成品へ引き継ぐ。完成品の以後の冷却は完成品自身の特性に従う。

#### Acceptance Criteria

##### AC-F006-001: 主入力の温度判定
Preconditions: TFC温度条件が設定されたSequenced Assemblyレシピと、その条件を満たす入力および満たさない入力を用意している。  
Action: 両方の入力で工程開始を試みる。  
Expected Result: 条件を満たす入力のみ工程を開始し、条件未満の入力は当該レシピの開始対象にならない。

##### AC-F006-002: 加工中の自然冷却と続行
Preconditions: 温度条件を満たす加熱済み入力でSequenced Assemblyを開始している。  
Action: 中間アイテムを工程間で保持し、経過時間によって開始条件の温度未満まで冷却させた後に処理を継続する。  
Expected Result: 中間アイテムは温度を保持しながら自然冷却し、開始済みの工程は途中の温度低下のみを理由として中断されない。

##### AC-F006-003: 完成品への温度引き継ぎ
Preconditions: 入力と完成品の双方がTFCの加熱に対応し、加工中に温度が変化している。  
Action: Sequenced Assemblyを完了して完成品の温度を確認する。  
Expected Result: 完成直前の中間アイテム温度が完成品へ引き継がれ、その後は完成品自身の冷却特性に従う。

##### AC-F006-004: 非加熱アイテムへの対応
Preconditions: 温度を持つ入力から、加熱に対応しない完成品を得るSequenced Assemblyレシピがある。  
Action: レシピを最後まで実行する。  
Expected Result: 正常に完成品が生成され、非対応の完成品に不正な温度情報は付与されない。

### F-007: Sequenced AssemblyによるBloomの精製

#### Description

既存のSequenced Assemblyレシピアセットを使用して、TFCの製鉄工程をCreate Mechanical Pressで処理する。両レシピとも主入力の最低温度は921.00006℃とし、3回のPressing工程を1組として3回繰り返す。

対象レシピは以下の2種類とする。

- `tfc:raw_iron_bloom` → `tfc:refined_iron_bloom`
- `tfc:refined_iron_bloom` → `tfc:metal/ingot/wrought_iron`

#### Acceptance Criteria

##### AC-F007-001: Raw Iron Bloomの精製
Preconditions: 921.00006℃以上の `tfc:raw_iron_bloom` と、必要なMechanical Press設備を用意している。  
Action: 対応するSequenced Assemblyを3工程×3周で完了する。  
Expected Result: `tfc:refined_iron_bloom` が1個生成され、途中のアイテム温度にはF-006の仕様が適用される。

##### AC-F007-002: Wrought Iron Ingotの製造
Preconditions: 921.00006℃以上の `tfc:refined_iron_bloom` と、必要なMechanical Press設備を用意している。  
Action: 対応するSequenced Assemblyを3工程×3周で完了する。  
Expected Result: `tfc:metal/ingot/wrought_iron` が1個生成され、途中のアイテム温度にはF-006の仕様が適用される。

##### AC-F007-003: 温度不足時の開始拒否
Preconditions: 2種類のレシピの入力アイテムをそれぞれ920℃まで冷却している。  
Action: Sequenced Assemblyを開始する。  
Expected Result: いずれも温度条件を満たさず、該当レシピが開始しない。

### F-008: CrucibleのDisplay Link出力

#### Description

Create Display Linkの情報源としてTFC Crucibleを使用する。現在温度を℃の数値またはTFCの温度色表現で表示する。十分な熱がない場合は `No heat` 相当の情報を出力する。情報源の識別IDは `poweredtfc:tfc_crucible` とする。

#### Acceptance Criteria

##### AC-F008-001: 温度表示モード
Preconditions: Crucible、Display Linkおよび表示先を接続している。  
Action: Crucibleを加熱し、数値表示と温度色表示を切り替える。  
Expected Result: 数値表示では℃表記の温度、温度色表示では現在温度に対応するTFCの色表現が表示される。

##### AC-F008-002: 無熱状態と更新
Preconditions: 温度の低いCrucibleが表示先に接続されている。  
Action: 温度を変化させ、再び無熱状態になるまで観察する。  
Expected Result: 表示内容が現在の状態に追従し、無熱状態では `No heat` 相当となる。

### F-009: Blast FurnaceのDisplay Link出力

#### Description

TFC Blast FurnaceをCreate Display Linkの情報源として使用する。情報源の識別IDは `poweredtfc:tfc_blast_furnace` とする。表示モードは温度数値、温度色、Input Count、Catalyst Count、Fuel Count、およびCompactの6種類とする。Compactでは温度と3種類の個数を1行で表す。

#### Acceptance Criteria

##### AC-F009-001: 6種類の表示モード
Preconditions: Blast Furnace、Display Link、表示先を接続し、投入物・触媒・燃料を確認できる。  
Action: 6種類の表示モードを順次選択する。  
Expected Result: 温度数値、温度色、投入物数、触媒数、燃料数がそれぞれ正しく表示され、Compactでは `温度℃ | 投入物数 | 触媒数 | 燃料数` の順で表示される。

##### AC-F009-002: 設備状態の更新
Preconditions: Blast FurnaceがDisplay Linkへ接続されている。  
Action: 燃料や投入物の追加、消費、温度変化を発生させる。  
Expected Result: 表示が現在の設備状態に更新され、手動でDisplay Linkを再設置する必要がない。

### F-010: Basinの液体入力拡張

#### Description

Create Basinの液体入力を独立した4区画へ拡張し、1区画あたり1000 mB、合計4000 mBを保持する。最大4種類のFluid Ingredientを要求するBasin加工レシピを認識する。変更対象は入力側のみとし、既存の液体出力制限や通常レシピの振る舞いは維持する。

#### Acceptance Criteria

##### AC-F010-001: 4系統の液体保持
Preconditions: 空のBasinと4種類の相互に異なる液体がある。  
Action: それぞれを1000 mBずつBasinへ供給する。  
Expected Result: 4種類が独立して保持され、各区画は1000 mBを超えて保持しない。

##### AC-F010-002: 3～4種類の液体レシピ
Preconditions: 3種類および4種類のFluid Ingredientを使用する有効なBasinレシピがある。  
Action: 必要な材料を投入してMixingまたはCompactingを行う。  
Expected Result: 液体入力数だけを理由としてレシピが拒否されず、条件を満たせば正常に加工される。

##### AC-F010-003: 既存加工との互換性
Preconditions: 通常の1～2種類の液体を使用するCreateレシピがある。  
Action: レシピを実行し、液体出力も確認する。  
Expected Result: 既存レシピが従来どおり実行され、液体出力の区画数・容量制限は変更されない。

### F-011: CreateアイテムのTFCサイズ・重量対応

#### Description

Powered TFC v0.4.3に含まれるTFC用のサイズ・重量定義資産を使用し、対象版Createのアイテム・ブロックアイテムおよび本ModのConverter 2種類をTFCのSize／Weight体系へ組み込む。対象版Createの740 IDとConverter 2 IDを、重複なく明示的に分類する。分類値は既存アセットを基準とし、TFC側の表示・収納・重量制限へ反映する。

#### Acceptance Criteria

##### AC-F011-001: 分類の網羅と一意性
Preconditions: Create 6.0.10および本Modが導入され、対象ID一覧と既存サイズ・重量定義資産を利用できる。  
Action: 対象IDの分類を検査する。  
Expected Result: Create由来の740 IDとConverter 2 IDに対してSizeとWeightが定義され、未分類・重複分類がない。

##### AC-F011-002: TFCでの反映
Preconditions: 既存アセットで異なるSize／Weightに分類された代表的なCreateアイテムを所持している。  
Action: TFCのサイズ・重量表示と、サイズ制限を持つ収納設備への出し入れを確認する。  
Expected Result: 各アイテムが定義された分類に従って表示・収納判定され、すべてが一律の初期分類として扱われない。

### F-012: 設定可能な連携パラメータ

#### Description

次の8項目を共通設定として提供する。項目名、初期値、設定範囲は既存の設定互換性を考慮して維持する。

| 設定項目 | 初期値 | 設定可能範囲 |
| --- | ---: | ---: |
| `blaze_burner.smoulderingHeat` | 80 | 0～10000 |
| `blaze_burner.fadingHeat` | 1100 | 0～10000 |
| `blaze_burner.kindledHeat` | 1300 | 0～10000 |
| `blaze_burner.seethingHeat` | 1800 | 0～10000 |
| `encased_fan.baseAir` | 100 | 0～10000 |
| `encased_fan.speedMultiplier` | 1.0 | 0～100 |
| `mechanical_converters.createToTfcStressImpact` | 64 | 0～16384 |
| `mechanical_converters.tfcToCreateStressCapacity` | 64 | 0～16384 |

#### Acceptance Criteria

##### AC-F012-001: 設定値の反映
Preconditions: 既定値で熱源、Fanおよび両Converterが動作している。  
Action: 8項目を設定可能範囲内で変更して再読み込みする。  
Expected Result: 各機能が指定値に従って動作し、設定値は再起動後も保持される。

##### AC-F012-002: 範囲外設定の防止
Preconditions: 設定ファイルを編集できる。  
Action: 各項目に許容範囲外の値を指定する。  
Expected Result: 設定が安全に検証・補正または拒否され、値の不正によってゲームデータが破損しない。

## Global Requirements

- **GR-001: 必須依存関係。** Minecraft 1.21.1のNeoForge環境で、Create 6.0.10、TerraFirmaCraft 4.2.12、Patchouli 1.21.1-93-NEOFORGEを必須構成とする。PatchouliはTFCに必要な依存Modである。必須ではない外部Modに関する要件は設けない。
- **GR-002: バージョン基準。** 記載したバージョンは2026-10-28時点で確認したMinecraft 1.21.1対応の最新版である。実装開始時とリリース時に各配布元を再確認し、新版を採用する場合は必須構成全体で互換性を検証する。確認先は[NeoForge Maven](https://maven.neoforged.net/releases/net/neoforged/neoforge)、[Create](https://www.curseforge.com/minecraft/mc-mods/create/files/all?version=1.21.1)、[TFC](https://www.curseforge.com/minecraft/mc-mods/terrafirmacraft/files/all?version=1.21.1)、[Patchouli](https://www.curseforge.com/minecraft/mc-mods/patchouli/files/all?version=1.21.1)とする。
- **GR-003: アセット再利用。** [Powered TFC v0.4.3](https://github.com/lau-mods/powered-TFC/releases/tag/v0.4.3)を再利用元とし、必要な `assets/poweredtfc/` 内のblockstates、models、langおよび `data/poweredtfc/` 内のrecipe、loot_table、TFC item_size、item_size tagsを利用する。資産に含まれる名称、モデル参照、レシピ、翻訳キー、識別IDは原則維持する。不要な資産の採用は必須としない。
- **GR-004: IDの継続性。** Mod IDは `poweredtfc` とし、既存のConverter 2種類とDisplay Link情報源2種類のIDを維持する。既存のワールド・データ・リソースとの整合性を優先する。
- **GR-005: 実装からの独立。** 本書の達成判定は外部から観察できる振る舞いと公開されたデータ資産に基づく。既存版のソースコード、内部クラス、Mixin、メソッド名、イベント構成、内部ストレージ設計等の知識を前提にしない。
- **GR-006: 一般的な互換性。** TFCとCreateが元来持つ動力網の制限、熱と自然冷却、レシピ管理、液体の入出力、ワールド保存・復元の規則を尊重する。追加機能が関係しない既存動作を変更しない。
- **GR-007: 動作環境。** シングルプレイと専用サーバーを対象とし、設定、設備状態、加工中アイテムの温度および回転・液体状態について、再ログイン、チャンク再読み込み、ワールド保存・再開後も整合性を保つ。
- **GR-008: 品質確認。** 全Acceptance Criteriaを基本構成のみで実施し、使用するJSON資産の構文・参照先、存在しないIDの扱い、レシピ登録、専用サーバー起動を検証する。基準版の更新時は回帰テストを再実施する。
- **GR-009: 資産と権利。** 既存アセットの再利用に当たり、配布元のライセンスと必要な権利表示を遵守する。Create本体のモデル等を参照する場合は、その参照を維持し、無断で複製して同梱しない。

## Constraints

- 対象のMinecraftバージョンは1.21.1に固定し、他バージョンへの移植を本プロジェクトの要件としない。
- 依存Modの構成はGR-001に示したものだけを前提とする。依存関係を増やして機能を成立させることはしない。
- エンチャントテーブル、ラピスラズリ、エンチャント素材に関する互換機能は対象外とする。
- 新規登録する通常のブロックは原則としてConverter 2種類のみとし、独自の鉱石、金属、鉱脈、作物、Mob、世界生成要素等を追加しない。
- 既存v0.4.3のアセットを参照するが、元実装のバイナリや内部ロジックへの依存、内部仕様の推測による再現を要求しない。
- 温度、回転速度、Stress、液体量はTFCとCreateの公開仕様・ゲーム内単位に整合させる。精度差の受入判定では、浮動小数点の丸め誤差を考慮する。
- 対象最新版同士の実動作互換性はリリース済みという事実だけでは保証されない。構築後の結合試験で確認する。

## Open Questions

None.
