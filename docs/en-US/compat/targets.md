# Extra Migration Targets

Use `uuidbridge/targets.json` for mod data that lives outside the standard
Minecraft world files. Paths are relative to the server root by default. Prefix
with `world:` to make a path relative to the active world directory.

```json
{
  "include": [
    {
      "path": "config/claims/*.json",
      "format": "json"
    },
    {
      "path": "world:data/mod_backpacks.dat",
      "format": "nbt-gzip"
    }
  ],
  "exclude": [
    "config/claims/archive/**"
  ]
}
```

Supported formats:

- `auto`: choose from the file extension.
- `json`: rewrite exact UUID string values while preserving unknown fields.
- `nbt-gzip`: rewrite compressed NBT, with binary fallback for legacy fixtures.
- `nbt-plain`: rewrite uncompressed NBT.
- `region`: rewrite `.mca` chunks.

Paths may not escape the server or world directory. UUIDBridge excludes
`uuidbridge/backups`, logs, crash reports, `.git`, build outputs, and the `mods`
directory from broad scans.

Binary and database files such as SQLite or LevelDB are intentionally not
rewritten in this phase. Treat them as future adapter work once real fixtures
are available.
