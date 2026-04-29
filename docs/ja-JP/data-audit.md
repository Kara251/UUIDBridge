# プレイヤーデータ移行監査

UUIDBridge が移行するべきものは identity reference であり、プレイヤー名では
ありません。online-mode と offline-mode の切り替えでは通常プレイヤー名は
変わらないため、名前だけを key にしたデータは既定では書き換えません。

## 現在対応済み

- プレイヤーファイル名：`playerdata`、`advancements`、`stats`。
- サーバー identity list：`whitelist.json`、`ops.json`、`banned-players.json`、
  `usercache.json`。
- ワールド NBT と region data：`level.dat`、`data/*.dat`、`region/*.mca`、
  `entities/*.mca`。
- UUID 形式：dash 付き文字列、dash なし文字列、int array、long array、
  `UUIDMost` / `UUIDLeast`。
- entity、block entity、player data、mod NBT 内の owner、trusted players、
  anger/love cause、villager gossip、BossBar players、raid saved data、
  scoreboard UUID values、Touhou Little Maid 形式の `owner_uuid`。
- singleplayer transfer：明示的な `--singleplayer-name <name>` により、
  `level.dat` の `Data.Player` を `playerdata/<targetUuid>.dat` にコピーできます。
- `uuidbridge/targets.json` で指定した JSON、NBT、region、明示的 binary file。

## 重要な不足

- FTB Teams、FTB Chunks、Open Parties and Claims、LuckPerms storage、経済、
  クエスト、墓、ショップ、ホーム、ワープ、バックパック所有権などの実 mod adapter
  はまだありません。
- SQLite、H2、MySQL dump、LevelDB、mod 独自の transaction file は汎用的に
  書き換えられません。専用 adapter と fixture が必要です。
- scoreboard の名前 key は既定では書き換えません。UUID モード移行では正しい
  動作です。名前変更移行は別機能です。
- 汎用 binary replacement は広範囲スキャンに使うべきではありません。明示的
  opt-in に限定します。
- region は現在 `.mca` 全体を読んだ上で変更 chunk を書き戻します。次の性能改善は
  sector-level writer です。

## 次に追加すべきデータ

- player head owner profile と item component 内の player UUID。
- Brain memories と entity 固有参照：liked player、angry target、trusted UUID
  list、projectile owner/thrower、leash data。
- `data/map_*.dat` と command storage fixture。datapack や mod がそこへ UUID を
  保存する場合があります。
- permission、claim、team、economy、quest、grave、shop、home、warp、backpack の
  実サーバー fixture。
- 未対応 database の report-only detection。可能なら path と推定 mod 名も出します。

## 小さく保つ、または削除すべき機能

- Mojang network lookup は安全な移行に必須ではありません。mapping file の方が
  再現性があります。既定無効を維持し、alpha では削除も検討できます。
- 公開 `binary` target はリスクが高いです。実 fixture が必要性を証明するまで
  explicit-only に留めるか、公開文書から外します。
- 実際の移行経路につながっていない placeholder type や option は削除します。
