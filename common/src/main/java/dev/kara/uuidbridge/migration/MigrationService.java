package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.io.JsonCodecs;
import dev.kara.uuidbridge.migration.io.PathSecurity;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

public final class MigrationService {
    private final MigrationPlanner planner = new MigrationPlanner();
    private final MigrationExecutor executor = new MigrationExecutor();

    public ScanResult scan(
        UuidBridgePaths paths,
        MigrationDirection direction,
        Optional<String> mapping,
        boolean allowNetwork
    ) throws IOException {
        Optional<Path> mappingFile = resolveMapping(paths, mapping);
        return planner.scan(paths, direction, mappingFile, allowNetwork);
    }

    public MigrationPlan createPlan(
        UuidBridgePaths paths,
        MigrationDirection direction,
        Optional<String> mapping,
        boolean allowNetwork
    ) throws IOException {
        Optional<Path> mappingFile = resolveMapping(paths, mapping);
        MigrationPlan plan = planner.createPlan(paths, direction, mappingFile, allowNetwork);
        JsonCodecs.write(paths.planPath(plan.id()), plan);
        return plan;
    }

    public void markPending(UuidBridgePaths paths, String planId) throws IOException {
        Path planPath = paths.planPath(planId);
        if (!Files.isRegularFile(planPath)) {
            throw new IOException("Plan not found: " + planId);
        }
        PendingPlan pending = new PendingPlan(planId);
        JsonCodecs.write(paths.pendingFile(), pending);
    }

    public Optional<String> pendingPlan(UuidBridgePaths paths) throws IOException {
        if (!Files.isRegularFile(paths.pendingFile())) {
            return Optional.empty();
        }
        return Optional.of(JsonCodecs.read(paths.pendingFile(), PendingPlan.class).planId());
    }

    public Optional<Path> latestReport(UuidBridgePaths paths) throws IOException {
        if (!Files.isDirectory(paths.reportsDir())) {
            return Optional.empty();
        }
        try (var stream = Files.list(paths.reportsDir())) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                .max(Comparator.comparing(path -> path.toFile().lastModified()));
        }
    }

    public boolean cancel(UuidBridgePaths paths, String planId) throws IOException {
        Optional<String> pending = pendingPlan(paths);
        if (pending.isPresent() && pending.get().equals(planId)) {
            Files.deleteIfExists(paths.pendingFile());
            return true;
        }
        return false;
    }

    public MigrationReport executePending(UuidBridgePaths paths) throws IOException {
        String planId = pendingPlan(paths).orElseThrow(() -> new IOException("No pending migration plan."));
        MigrationPlan plan = JsonCodecs.read(paths.planPath(planId), MigrationPlan.class);
        MigrationReport report = executor.execute(paths, plan);
        Files.deleteIfExists(paths.pendingFile());
        if (!report.successful()) {
            throw new IOException("UUIDBridge migration completed with errors. See " + paths.reportPath(planId));
        }
        return report;
    }

    private static Optional<Path> resolveMapping(UuidBridgePaths paths, Optional<String> mapping) throws IOException {
        if (mapping.isEmpty() || mapping.get().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(PathSecurity.resolveInside(paths.gameDir(), mapping.get()));
    }

    private record PendingPlan(String planId) {
    }
}
