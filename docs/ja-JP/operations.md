# 運用ガイド

UUIDBridge は、コンソール、ファイルマネージャー、再起動ボタンだけを使う
パネル型サーバーを想定しています。

## 標準手順

1. 対応する Fabric、Forge、または NeoForge の jar をアップロードします。
2. サーバーを一度起動し、コンソールで `uuidbridge status` を実行します。
3. offline-mode から online-mode へ戻す場合は、マッピングファイルをサーバー
   ルートに置きます。
4. `uuidbridge scan <online-to-offline|offline-to-online>` を実行します。
5. `uuidbridge plan <direction> [--mapping <file>] [--allow-network]` を実行します。
6. `uuidbridge apply <planId> --confirm` を実行します。
7. サーバーを再起動します。UUIDBridge は通常のワールド使用前に pending plan
   を適用します。

起動中の移行処理を強制停止しないでください。apply に失敗した場合、
UUIDBridge は backup manifest に基づいて変更済みファイルの復元を試み、
その後サーバー起動を止めます。

## ロールバック

完了済みの移行を取り消す場合：

```sh
uuidbridge rollback <planId> --confirm
```

rollback pending を設定した後、サーバーを再起動します。UUIDBridge は
`uuidbridge/backups/<planId>/manifest.json` から復元します。現在のファイルを
上書きする前に、`uuidbridge/backups/<planId>/rollback-current/` へ保存します。

## マッピングファイル

CSV：

```csv
name,onlineUuid,offlineUuid
Alice,11111111-2222-3333-4444-555555555555,aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee
```

JSON：

```json
[
  {
    "name": "Alice",
    "onlineUuid": "11111111-2222-3333-4444-555555555555",
    "offlineUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
  }
]
```

online-mode から offline-mode へ移行する場合、`offlineUuid` は省略できます。
offline-mode から online-mode へ戻す場合は、ネットワーク照会ではなく
マッピングファイルを使うことを推奨します。

## 対象データ

- `playerdata/*.dat`
- `advancements/*.json`
- `stats/*.json`
- `whitelist.json`、`ops.json`、`banned-players.json`、`usercache.json`
- ワールドの `region`、`entities`、`data`、`level.dat` NBT ファイル
- entity owner、村人 gossip、BossBar player list、raid saved data、scoreboard
  内の UUID 値
- `uuidbridge/targets.json` で指定した追加 JSON、NBT、region、明示的 binary target

scoreboard のプレイヤー名は既定では書き換えません。online/offline UUID 移行で
変わるのは UUID であり、プレイヤー名ではありません。

## 追加ターゲット

claim、team、permission、backpack などの mod データが標準ワールドファイル外に
ある場合、`uuidbridge/targets.json` を作成します。形式は
[追加移行ターゲット](compat/targets.md) を参照してください。

```sh
uuidbridge scan online-to-offline --targets uuidbridge/targets.json
uuidbridge plan online-to-offline --targets uuidbridge/targets.json
```

SQLite、LevelDB、その他のトランザクション型データベースは、この段階では直接
書き換えません。専用 adapter ができるまで対象に含めないでください。

## シングルプレイヤーの Player タグ

シングルプレイヤーワールドを専用サーバーへ移す場合、`level.dat` に
`Data.Player` があり、`playerdata/<uuid>.dat` が存在しないことがあります。

```sh
uuidbridge plan online-to-offline --singleplayer-name Alice
```

対象の `playerdata/<targetUuid>.dat` が存在しない場合、UUIDBridge は起動時の
移行で埋め込み Player タグをコピーします。元の `Data.Player` は rollback と
手動確認のために残します。

## ランタイムファイル

ランタイムファイルはサーバールートの `uuidbridge/` に書き込まれます。

- `plans/<planId>.json`
- `pending.json`
- `migration.lock`
- `reports/<planId>.json`
- `reports/<planId>-rollback.json`
- `backups/<planId>/manifest.json`
- `backups/<planId>/rollback-current/`

`uuidbridge status` で pending、最新 report、lock、backup manifest を確認できます。
