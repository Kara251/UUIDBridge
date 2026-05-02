# Changelog

## Unreleased

- Scaffold UUIDBridge as a Minecraft 1.21.1 multi-loader migration mod.
- Add migration integration fixtures, backup manifests, failure diagnostics,
  and dedicated server smoke tasks.
- Add pending apply/rollback recovery workflow with atomic control writes,
  automatic rollback on failed apply, and manual rollback reports.
- Add identity coverage reporting, semantic JSON/NBT adapters, optional mod
  target files, and singleplayer `Data.Player` extraction.
- Remove agent-only instructions from the public repository.
- Move documentation into Chinese, English, and Japanese language directories
  and move the changelog under `docs/`.
- Remove the unused `IdentityReference` placeholder type.
- Remove public `binary` entries from `targets.json` support.
- Remove Mojang network lookup; offline-to-online migration now requires an
  explicit mapping file.
- Add vanilla coverage fixtures and docs for map/command storage, cross-dimension
  regions, player head profiles, Brain memories, projectile, leash, and related
  UUID references; plan single-mapping singleplayer `Data.Player` copies
  automatically.
