# Development Guide

UUIDBridge targets Minecraft 1.21.1 and requires JDK 21.

## Common Commands

```sh
./gradlew test
./gradlew build
./gradlew smokeServerAll
```

The Gradle Wrapper is used so no global Gradle install is required.

## Build Outputs

Installable jars are staged under:

- `build/distributions/mods/fabric/`
- `build/distributions/mods/forge/`
- `build/distributions/mods/neoforge/`

## Smoke Tests

`smokeServerAll` starts real dedicated servers for Fabric, Forge, and NeoForge,
runs `uuidbridge status`, and stops them. The task is intentionally not wired
into the default `build` task because it is slower than unit and file-level
tests.

Forge and NeoForge smoke tests use production server installers and the
remapped UUIDBridge jars. Fabric uses the development `runServer` task with a
temporary smoke server directory.
