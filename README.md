# UUIDBridge

UUIDBridge helps Minecraft server administrators migrate between online-mode
and offline-mode UUIDs without losing player-bound data.

The first version targets Minecraft 1.21.1 and provides Fabric, Forge, and
NeoForge builds from one shared codebase. It is server-side only and has no
client UI.

## Workflow

1. Run `/uuidbridge scan <online-to-offline|offline-to-online>` to inspect the
   server.
2. Run `/uuidbridge plan <direction> [--mapping <file>] [--allow-network]` to
   create a migration plan.
3. Run `/uuidbridge apply <planId> --confirm`.
4. Restart the server. UUIDBridge applies the pending plan before normal play,
   writes a report, and keeps backups.
5. To undo a completed migration, run `/uuidbridge rollback <planId> --confirm`
   and restart again.

## Panel Server Workflow

UUIDBridge is designed for panel-hosted servers where administrators usually
have a console, file manager, and restart button, but not a full shell.

1. Upload the matching Fabric, Forge, or NeoForge jar.
2. Start the server once and run `uuidbridge status` in the panel console.
3. Upload a mapping file to the server root when migrating from offline-mode
   back to online-mode.
4. Run `uuidbridge scan <direction>` and check the reported conflicts or
   missing mappings.
5. Run `uuidbridge plan <direction> --mapping mapping.csv` when needed.
6. Run `uuidbridge apply <planId> --confirm`.
7. Restart the server from the panel. UUIDBridge applies the pending plan
   during startup before normal play.

Do not force-stop the server during the startup migration unless the panel is
already stuck. If an apply fails, UUIDBridge attempts to restore changed files
from the backup manifest before stopping startup. If recovery also fails, it
keeps the pending file, lock, report, and backup manifest for diagnosis.

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

## Reports, Backups, and Recovery

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

Manual rollback is also available after a successful migration:

```sh
uuidbridge rollback <planId> --confirm
```

Restart the server after marking rollback pending. UUIDBridge restores files
from `manifest.json`; before overwriting current files it saves them under
`rollback-current/`.

Use `uuidbridge status` to inspect the pending action, latest report,
`migration.lock`, and backup manifest state.

## Development

This project requires JDK 21.

```sh
./gradlew build
./gradlew test
./gradlew smokeServerAll
```

The Gradle Wrapper is used so no global Gradle install is required.

The smoke tasks start real dedicated servers for Fabric, Forge, and NeoForge,
run `uuidbridge status`, and stop them. They are intentionally not wired into
the default `build` task because they are slower than unit and file-level tests.
