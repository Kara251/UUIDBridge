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

## Data Covered

- `playerdata/*.dat`
- `advancements/*.json`
- `stats/*.json`
- `whitelist.json`, `ops.json`, `banned-players.json`, `usercache.json`
- World `region`, `entities`, `data`, and `level.dat` NBT files

## Development

This project requires JDK 21.

```sh
./gradlew build
./gradlew test
```

The Gradle Wrapper is used so no global Gradle install is required.
