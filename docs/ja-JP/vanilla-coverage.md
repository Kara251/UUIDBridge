# バニラ対応範囲

このページでは「バニラ完全移行」の境界を定義します。UUIDBridge は Minecraft
1.21.1 のバニラデータがディスクへ保存する player UUID identity reference を
移行します。プレイヤー名や外部アカウントサービスのデータは移行しません。

## 対象ファイル

- サーバールート：`whitelist.json`、`ops.json`、`banned-players.json`、
  `usercache.json`。
- プレイヤーファイル：`world/playerdata/*.dat`、
  `world/advancements/*.json`、`world/stats/*.json`。UUID をファイル名にした
  ファイルの rename も含みます。
- ワールドルート：`world/level.dat`、`world/level.dat_old`。
- saved data：`world/data/*.dat`。scoreboard、raid、map、command storage
  などを含みます。
- region file：`region/*.mca`、`entities/*.mca`、`poi/*.mca`、および
  `DIM-1/region/*.mca`、`DIM1/entities/*.mca` など各 dimension 内の同種ファイル。

## 対象 UUID 形式

- dash 付き UUID 文字列。
- dash なし UUID 文字列。
- NBT `int[4]` UUID。
- NBT `long[2]` UUID。
- `UUIDMost` / `UUIDLeast` と同種の `*Most` / `*Least` long pair。

## 対象バニラ意味データ

- entity、block entity、player NBT 内の owner、trusted player、
  anger/love cause、conversion player、projectile owner/thrower、leash UUID。
- wolf、cat、parrot、horse、fox などの tameable / trusted entity 所有データ。
- villager gossip target。
- BossBar player list。
- raid heroes / raid saved data。
- scoreboard 内の UUID 形式の値。
- player head owner profile と 1.21 item component profile の UUID。
- Brain memories に保存された player reference。
- singleplayer `level.dat Data.Player`：mapping が 1 件だけなら
  `playerdata/<targetUuid>.dat` へ自動コピーします。mapping が複数ある場合は
  `--singleplayer-name <name>` で明示します。

## 対象外

- scoreboard team member name、score owner name などのプレイヤー名フィールドは
  書き換えません。online/offline migration は名前変更ではありません。
- `banned-ips.json` は player UUID データではないため変更しません。
- log、crash report、backup、mods、build output、`.git` はスキャンしません。
- SQLite、LevelDB、H2、MySQL などの database は書き込みません。
- 任意の長い文字列に埋め込まれた UUID は曖昧に置換しません。UUIDBridge は
  identity reference として扱える exact value を移行します。
