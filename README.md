# UUIDBridge

UUIDBridge helps Minecraft server administrators migrate between online-mode
and offline-mode UUIDs without losing player-bound data.

The first version targets Minecraft 1.21.1 and provides Fabric, Forge, and
NeoForge builds from one shared codebase. It is server-side only and has no
client UI.

## Quick Start

1. Upload the matching loader jar.
2. Run `uuidbridge scan <online-to-offline|offline-to-online>`.
3. Run `uuidbridge plan <direction> [--mapping <file>] [--allow-network]`.
4. Run `uuidbridge apply <planId> --confirm`.
5. Restart the server so UUIDBridge can apply the pending plan before play.

To undo a completed migration, run `uuidbridge rollback <planId> --confirm`
and restart again.

## Documentation

- [Operations guide](docs/operations.md)
- [Extra migration targets](docs/compat/targets.md)
- [Development guide](docs/development.md)
- [Changelog](CHANGELOG.md)

## License

MIT
