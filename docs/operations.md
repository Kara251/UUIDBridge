# Operations Guide

UUIDBridge is designed for panel-hosted Minecraft servers where administrators
usually have a console, file manager, and restart button, but not a full shell.

## Standard Workflow

1. Upload the matching Fabric, Forge, or NeoForge jar.
2. Start the server once and run `uuidbridge status` in the panel console.
3. Upload a mapping file to the server root when migrating from offline-mode
   back to online-mode.
4. Run `uuidbridge scan <online-to-offline|offline-to-online>`.
5. Run `uuidbridge plan <direction> [--mapping <file>] [--allow-network]`.
6. Run `uuidbridge apply <planId> --confirm`.
7. Restart the server. UUIDBridge applies the pending plan before normal play.

Do not force-stop the server during startup migration unless the panel is
already stuck. If apply fails, UUIDBridge attempts to restore changed files
from the backup manifest before stopping startup.

## Rollback Workflow

To undo a completed migration:

```sh
uuidbridge rollback <planId> --confirm
```

Restart the server after marking rollback pending. UUIDBridge restores files
from `uuidbridge/backups/<planId>/manifest.json`; before overwriting current
files it saves them under `uuidbridge/backups/<planId>/rollback-current/`.

## Mapping Files

CSV:

```csv
name,onlineUuid,offlineUuid
Alice,11111111-2222-3333-4444-555555555555,aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee
```

JSON:

```json
[
  {
    "name": "Alice",
    "onlineUuid": "11111111-2222-3333-4444-555555555555",
    "offlineUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
  }
]
```

`offlineUuid` may be omitted when migrating online-mode to offline-mode because
UUIDBridge can derive it from the player name. For offline-mode to online-mode,
provide a mapping file unless `--allow-network` is intentionally enabled.

## Data Covered

- `playerdata/*.dat`
- `advancements/*.json`
- `stats/*.json`
- `whitelist.json`, `ops.json`, `banned-players.json`, `usercache.json`
- World `region`, `entities`, `data`, and `level.dat` NBT files
- Vanilla identity references in NBT and JSON forms, including entity owners,
  villager gossip targets, custom boss event players, raid saved data, and UUID
  values in scoreboard data
- Optional extra JSON/NBT/binary targets declared in `uuidbridge/targets.json`

Scoreboard player names are not rewritten by default. UUIDBridge migrates UUID
identity references; online/offline UUID migrations do not change player names.

## Optional Targets

Create `uuidbridge/targets.json` when a mod stores claims, teams, permissions,
or backpack ownership outside the standard world files. See
`docs/compat/targets.md` for the schema and examples.

Use `--targets <file>` to test a different targets file:

```sh
uuidbridge scan online-to-offline --targets uuidbridge/targets.json
uuidbridge plan online-to-offline --targets uuidbridge/targets.json
```

SQLite, LevelDB, and other transactional databases are reported as unsupported
for direct writes in this phase. Do not include database files unless a future
adapter explicitly supports that format.

## Singleplayer World Player Tag

For singleplayer worlds being moved to a dedicated server, `level.dat` may
contain `Data.Player` instead of a separate `playerdata/<uuid>.dat` file.

```sh
uuidbridge plan online-to-offline --singleplayer-name Alice
```

When the target `playerdata/<targetUuid>.dat` does not already exist,
UUIDBridge copies the embedded player tag into that file during startup
migration. The original `Data.Player` tag is left in `level.dat` so rollback and
manual inspection remain straightforward.

## Runtime Files

Runtime files are written under `uuidbridge/` in the server root:

- `plans/<planId>.json`
- `pending.json`, containing either an `apply` or `rollback` action
- `migration.lock`, present only while startup recovery work is active
- `reports/<planId>.json`
- `reports/<planId>-rollback.json`
- `backups/<planId>/manifest.json`
- `backups/<planId>/rollback-current/`, created before manual rollback
  overwrites current files

On successful apply or rollback, `pending.json` and `migration.lock` are
removed. If apply fails and automatic rollback succeeds, `pending.json` remains
so the administrator can inspect the report before retrying or canceling; the
lock is removed because the world has been restored. If rollback fails, both
`pending.json` and `migration.lock` remain and startup is stopped.

Use `uuidbridge status` to inspect the pending action, latest report,
`migration.lock`, and backup manifest state.
