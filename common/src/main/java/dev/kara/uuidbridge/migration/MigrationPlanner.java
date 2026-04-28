package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.io.KnownPlayerScanner;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import dev.kara.uuidbridge.migration.rewrite.SingleplayerPlayerExtractor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MigrationPlanner {
    private final UuidResolver resolver;

    public MigrationPlanner() {
        this(new UuidResolver());
    }

    MigrationPlanner(UuidResolver resolver) {
        this.resolver = resolver;
    }

    public ScanResult scan(
        UuidBridgePaths paths,
        MigrationDirection direction,
        Optional<Path> mappingFile,
        boolean allowNetwork
    ) throws IOException {
        return scan(paths, direction, mappingFile, allowNetwork, Optional.empty(), Optional.empty());
    }

    public ScanResult scan(
        UuidBridgePaths paths,
        MigrationDirection direction,
        Optional<Path> mappingFile,
        boolean allowNetwork,
        Optional<Path> targetsFile,
        Optional<String> singleplayerName
    ) throws IOException {
        List<KnownPlayer> players = KnownPlayerScanner.scan(paths.gameDir(), paths.worldDir());
        UuidResolver.ResolvedMappings resolved = resolver.resolve(direction, players, mappingFile, allowNetwork);
        List<PlanConflict> conflicts = new ArrayList<>(conflicts(resolved.mappings()));
        List<MissingMapping> missing = new ArrayList<>(resolved.missingMappings());
        SingleplayerPlayerCopy singleplayerCopy = singleplayerCopy(paths, resolved.mappings(), singleplayerName,
            conflicts, missing);
        Estimate estimate = estimate(paths, resolved.mappings(), targetsFile, singleplayerCopy);
        return new ScanResult(direction, players.size(), resolved.mappings().size(),
            List.copyOf(conflicts), List.copyOf(missing), estimate.changes(), estimate.coverage());
    }

    public MigrationPlan createPlan(
        UuidBridgePaths paths,
        MigrationDirection direction,
        Optional<Path> mappingFile,
        boolean allowNetwork
    ) throws IOException {
        return createPlan(paths, direction, mappingFile, allowNetwork, Optional.empty(), Optional.empty());
    }

    public MigrationPlan createPlan(
        UuidBridgePaths paths,
        MigrationDirection direction,
        Optional<Path> mappingFile,
        boolean allowNetwork,
        Optional<Path> targetsFile,
        Optional<String> singleplayerName
    ) throws IOException {
        List<KnownPlayer> players = KnownPlayerScanner.scan(paths.gameDir(), paths.worldDir());
        UuidResolver.ResolvedMappings resolved = resolver.resolve(direction, players, mappingFile, allowNetwork);
        List<PlanConflict> conflicts = new ArrayList<>(conflicts(resolved.mappings()));
        List<MissingMapping> missing = new ArrayList<>(resolved.missingMappings());
        SingleplayerPlayerCopy singleplayerCopy = singleplayerCopy(paths, resolved.mappings(), singleplayerName,
            conflicts, missing);
        Estimate estimate = estimate(paths, resolved.mappings(), targetsFile, singleplayerCopy);
        String id = direction.id() + "-" + Instant.now().toString()
            .replace(":", "")
            .replace(".", "");
        return new MigrationPlan(
            id,
            direction,
            resolved.mappings(),
            targetPaths(paths),
            estimate.changes(),
            List.copyOf(conflicts),
            List.copyOf(missing),
            Instant.now().toString(),
            allowNetwork,
            estimate.coverage(),
            singleplayerCopy,
            targetsFile.map(Path::toString).orElse("")
        );
    }

    private static List<PlanConflict> conflicts(List<UuidMapping> mappings) {
        Map<UUID, List<String>> targetToNames = new LinkedHashMap<>();
        for (UuidMapping mapping : mappings) {
            targetToNames.computeIfAbsent(mapping.toUuid(), unused -> new ArrayList<>()).add(mapping.name());
        }
        return targetToNames.entrySet().stream()
            .filter(entry -> entry.getValue().stream().distinct().count() > 1)
            .map(entry -> new PlanConflict(entry.getKey(), entry.getValue(),
                "Multiple players resolve to the same target UUID."))
            .toList();
    }

    private static Estimate estimate(
        UuidBridgePaths paths,
        List<UuidMapping> mappings,
        Optional<Path> targetsFile,
        SingleplayerPlayerCopy singleplayerPlayerCopy
    ) throws IOException {
        if (mappings.isEmpty()) {
            return new Estimate(List.of(), CoverageReport.empty());
        }
        List<PlannedChange> changes = new ArrayList<>();
        List<DiscoveredFile> files = WorldFileScanner.discoverTargets(paths, targetsFile);
        for (DiscoveredFile file : files) {
            if (FileMigrator.shouldSkipLargeUnknown(file)) {
                continue;
            }
            FileChangeResult result = FileMigrator.preview(file, mappings);
            if (result.changed()) {
                changes.add(new PlannedChange(label(paths, file.path()), result.replacements(),
                    "rewrite:" + file.adapter()));
            }
        }
        for (Path file : WorldFileScanner.playerUuidFiles(paths)) {
            String renamed = renamedPlayerFile(file, mappings);
            if (renamed != null) {
                changes.add(new PlannedChange(label(paths, file) + " -> " + renamed, 1, "rename"));
            }
        }
        if (singleplayerPlayerCopy != null) {
            changes.add(new PlannedChange(singleplayerPlayerCopy.sourcePath() + " -> "
                + singleplayerPlayerCopy.targetPath(), 1, "singleplayer-player-copy"));
        }
        long replacements = changes.stream().mapToLong(PlannedChange::replacements).sum();
        return new Estimate(List.copyOf(changes), WorldFileScanner.coverage(paths, files, replacements));
    }

    private static SingleplayerPlayerCopy singleplayerCopy(
        UuidBridgePaths paths,
        List<UuidMapping> mappings,
        Optional<String> singleplayerName,
        List<PlanConflict> conflicts,
        List<MissingMapping> missing
    ) throws IOException {
        if (singleplayerName.isEmpty() || singleplayerName.get().isBlank()) {
            return null;
        }
        String name = singleplayerName.get();
        Optional<UuidMapping> mapping = mappings.stream()
            .filter(value -> value.name().equalsIgnoreCase(name))
            .findFirst();
        if (mapping.isEmpty()) {
            missing.add(new MissingMapping(name, null, "No mapping found for --singleplayer-name."));
            return null;
        }
        Path source = paths.worldDir().resolve("level.dat");
        if (!Files.isRegularFile(source)) {
            return null;
        }
        Optional<byte[]> playerData = SingleplayerPlayerExtractor.extractGzipPlayerData(
            Files.readAllBytes(source), List.of(mapping.get()));
        if (playerData.isEmpty()) {
            return null;
        }
        Path target = paths.worldDir().resolve("playerdata").resolve(mapping.get().toUuid() + ".dat");
        if (Files.exists(target)) {
            conflicts.add(new PlanConflict(mapping.get().toUuid(), List.of(mapping.get().name()),
                "Singleplayer playerdata target already exists: " + label(paths, target)));
            return null;
        }
        return new SingleplayerPlayerCopy(
            mapping.get().name(),
            label(paths, source),
            label(paths, target),
            mapping.get().fromUuid(),
            mapping.get().toUuid()
        );
    }

    static String renamedPlayerFile(Path file, List<UuidMapping> mappings) {
        String fileName = file.getFileName().toString();
        int dot = fileName.indexOf('.');
        if (dot <= 0) {
            return null;
        }
        String stem = fileName.substring(0, dot);
        String extension = fileName.substring(dot);
        for (UuidMapping mapping : mappings) {
            if (mapping.fromUuid().toString().equals(stem)) {
                return mapping.toUuid() + extension;
            }
        }
        return null;
    }

    static String label(UuidBridgePaths paths, Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        Path game = paths.gameDir().toAbsolutePath().normalize();
        Path world = paths.worldDir().toAbsolutePath().normalize();
        if (normalized.startsWith(world)) {
            return "world:" + world.relativize(normalized);
        }
        if (normalized.startsWith(game)) {
            return "game:" + game.relativize(normalized);
        }
        return normalized.toString();
    }

    private static List<String> targetPaths(UuidBridgePaths paths) {
        return List.of(
            paths.gameDir().resolve("whitelist.json").toString(),
            paths.gameDir().resolve("ops.json").toString(),
            paths.gameDir().resolve("banned-players.json").toString(),
            paths.gameDir().resolve("usercache.json").toString(),
            paths.worldDir().toString()
        );
    }

    private record Estimate(List<PlannedChange> changes, CoverageReport coverage) {
    }
}
