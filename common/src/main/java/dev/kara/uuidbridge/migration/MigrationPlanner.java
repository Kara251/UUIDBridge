package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.io.KnownPlayerScanner;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import java.io.IOException;
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
        List<KnownPlayer> players = KnownPlayerScanner.scan(paths.gameDir(), paths.worldDir());
        UuidResolver.ResolvedMappings resolved = resolver.resolve(direction, players, mappingFile, allowNetwork);
        List<PlanConflict> conflicts = conflicts(resolved.mappings());
        List<PlannedChange> estimated = estimate(paths, resolved.mappings());
        return new ScanResult(direction, players.size(), resolved.mappings().size(),
            conflicts, resolved.missingMappings(), estimated);
    }

    public MigrationPlan createPlan(
        UuidBridgePaths paths,
        MigrationDirection direction,
        Optional<Path> mappingFile,
        boolean allowNetwork
    ) throws IOException {
        List<KnownPlayer> players = KnownPlayerScanner.scan(paths.gameDir(), paths.worldDir());
        UuidResolver.ResolvedMappings resolved = resolver.resolve(direction, players, mappingFile, allowNetwork);
        List<PlanConflict> conflicts = conflicts(resolved.mappings());
        List<PlannedChange> estimated = estimate(paths, resolved.mappings());
        String id = direction.id() + "-" + Instant.now().toString()
            .replace(":", "")
            .replace(".", "");
        return new MigrationPlan(
            id,
            direction,
            resolved.mappings(),
            targetPaths(paths),
            estimated,
            conflicts,
            resolved.missingMappings(),
            Instant.now().toString(),
            allowNetwork
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

    private static List<PlannedChange> estimate(UuidBridgePaths paths, List<UuidMapping> mappings) throws IOException {
        if (mappings.isEmpty()) {
            return List.of();
        }
        List<PlannedChange> changes = new ArrayList<>();
        for (Path file : WorldFileScanner.discover(paths)) {
            FileChangeResult result = FileMigrator.preview(file, mappings);
            if (result.changed()) {
                changes.add(new PlannedChange(label(paths, file), result.replacements(), "rewrite"));
            }
        }
        for (Path file : WorldFileScanner.playerUuidFiles(paths)) {
            String renamed = renamedPlayerFile(file, mappings);
            if (renamed != null) {
                changes.add(new PlannedChange(label(paths, file) + " -> " + renamed, 1, "rename"));
            }
        }
        return changes;
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
}
