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
  NBT: owners, trusted players, anger/love causes, villager gossip targets,
  custom boss event players, raid saved data, scoreboard UUID values, and
  Touhou Little Maid style `owner_uuid`.
- Singleplayer transfer: explicit `--singleplayer-name <name>` can copy
  `level.dat` `Data.Player` into `playerdata/<targetUuid>.dat`.
- Extra mod files declared by `uuidbridge/targets.json` when they are JSON,
  NBT, region, or explicitly included binary files.

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
- Generic binary replacement is dangerous outside explicit targets. It should
  stay opt-in and should not be used as a broad scanner.
- Region handling still reads a whole `.mca` file before writing changed chunks.
  The next performance step is a true sector-level region writer.

## Next Data To Add

- Player profile references in skull owner data and item components.
- Brain memories and entity-specific references such as liked players, angry
  targets, trusted UUID lists, projectile owner/thrower fields, and leash data.
- Map saved data and command storage fixtures, because datapacks and mods may
  store player UUIDs there.
- Permission, claim, team, economy, quest, grave, shop, home, warp, and backpack
  fixtures from real servers before writing mod-specific adapters.
- Report-only detection for unsupported databases, with clear paths and mod
  names when possible.

## Features To Keep Small Or Remove

- Network Mojang lookup is not essential for safe migration. Mapping files are
  safer and reproducible; network lookup should remain disabled by default and
  may be removed from alpha builds.
- Public `binary` targets are risky. Keep them explicit-only or remove them
  until real fixtures prove they are needed.
- Any type or option not connected to a real migration path should be deleted
  instead of kept as a placeholder.
