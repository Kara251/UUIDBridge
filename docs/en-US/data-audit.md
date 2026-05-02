# Player Data Migration Audit

UUIDBridge must migrate identity references, not player names. Online-mode and
offline-mode usually keep the same visible username, so name-only data should
be reported but not rewritten by default.

## Covered Now

- Player file names: `playerdata`, `advancements`, and `stats`.
- Server identity lists: `whitelist.json`, `ops.json`, `banned-players.json`,
  and `usercache.json`.
- World NBT and region data: `level.dat`, `data/*.dat`, `region/*.mca`, and
  `entities/*.mca`.
- Common UUID forms: dashed strings, undashed strings, int arrays, long arrays,
  and `UUIDMost` / `UUIDLeast` pairs.
- Common ownership references in entities, block entities, player data, and mod
  NBT: owners, trusted players, anger/love causes, conversion players,
  projectile owner/thrower, leash data, villager gossip targets, custom boss
  event players, raid saved data, scoreboard UUID values, player head profiles,
  1.21 item component profiles, Brain memories, and Touhou Little Maid style
  `owner_uuid`.
- Singleplayer transfer: when exactly one mapping exists, `level.dat`
  `Data.Player` is copied automatically into `playerdata/<targetUuid>.dat`;
  use `--singleplayer-name <name>` when multiple mappings exist.
- Extra mod files declared by `uuidbridge/targets.json` when they are JSON,
  NBT, or region files.

## Important Gaps

- Real mod adapters are still missing for common data stores such as FTB Teams,
  FTB Chunks, Open Parties and Claims, LuckPerms storage, economy mods, quest
  mods, graves, shops, homes, warps, and backpack ownership files.
- Database-backed data such as SQLite, H2, MySQL dumps, LevelDB, and mod-private
  transaction files is not rewritten. It needs format-specific adapters and
  fixtures before it is safe.
- Scoreboard entries keyed by player name are not rewritten. That is correct for
  UUID mode migration, but a future username migration tool would need separate
  rules.
- Generic binary replacement is kept only as an internal fallback for known
  adapters. Public `binary` targets are not accepted.
- Region handling still reads a whole `.mca` file before writing changed chunks.
  The next performance step is a true sector-level region writer.

## Next Data To Add

- Permission, claim, team, economy, quest, grave, shop, home, warp, and backpack
  fixtures from real servers before writing mod-specific adapters.
- Report-only detection for unsupported databases, with clear paths and mod
  names when possible.

## Features To Keep Small Or Remove

- Mojang network lookup has been removed. Offline-to-online migrations require
  an explicit mapping file so the result is deterministic and auditable.
- Public `binary` targets are too risky for alpha and remain unavailable until
  real fixtures prove they are needed.
- Any type or option not connected to a real migration path should be deleted
  instead of kept as a placeholder.
