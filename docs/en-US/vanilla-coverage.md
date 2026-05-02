# Vanilla Coverage

This page defines the acceptance boundary for "complete vanilla migration":
UUIDBridge migrates player UUID identity references persisted by Minecraft
1.21.1 vanilla data. It does not migrate player names or external account
service data.

## Covered Files

- Server root: `whitelist.json`, `ops.json`, `banned-players.json`,
  `usercache.json`.
- Player files: `world/playerdata/*.dat`, `world/advancements/*.json`,
  `world/stats/*.json`, including UUID-based file renames.
- World root: `world/level.dat`, `world/level.dat_old`.
- Saved data: `world/data/*.dat`, including scoreboard, raid, map, and command
  storage data.
- Region files: `region/*.mca`, `entities/*.mca`, `poi/*.mca`, and matching
  files in every dimension directory such as `DIM-1/region/*.mca` and
  `DIM1/entities/*.mca`.

## Covered UUID Shapes

- Dashed UUID strings.
- Undashed UUID strings.
- NBT `int[4]` UUIDs.
- NBT `long[2]` UUIDs.
- `UUIDMost` / `UUIDLeast` and similar `*Most` / `*Least` long pairs.

## Covered Vanilla Semantics

- Entity, block entity, and player NBT owner, trusted player, anger/love cause,
  conversion player, projectile owner/thrower, and leash UUID references.
- Ownership data for tameable or trusted entities such as wolves, cats,
  parrots, horses, and foxes.
- Villager gossip targets.
- BossBar player lists.
- Raid heroes and raid saved data.
- UUID-shaped values in scoreboard data.
- Player head owner profiles and 1.21 item component profile UUIDs.
- Player references stored in Brain memories.
- Singleplayer `level.dat Data.Player`: copied automatically to
  `playerdata/<targetUuid>.dat` when exactly one mapping exists; use
  `--singleplayer-name <name>` when multiple mappings exist.

## Explicitly Out Of Scope

- Scoreboard team member names, score owner names, and other player-name
  fields are not rewritten. Online/offline migration does not change names.
- `banned-ips.json` is not changed because it is not player UUID data.
- Logs, crash reports, backups, mods, build output, and `.git` are not scanned.
- SQLite, LevelDB, H2, MySQL, and other databases are not written.
- UUIDs embedded in arbitrary long text are not fuzzy-rewritten; UUIDBridge
  migrates exact values that can be treated as identity references.
