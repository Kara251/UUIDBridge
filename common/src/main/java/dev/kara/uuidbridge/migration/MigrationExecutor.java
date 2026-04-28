package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.io.JsonCodecs;
import dev.kara.uuidbridge.migration.io.SafeFileWriter;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class MigrationExecutor {
    public MigrationReport execute(UuidBridgePaths paths, MigrationPlan plan) throws IOException {
        if (!plan.canApply()) {
            throw new IOException("Plan cannot be applied because it has conflicts, missing mappings, or no mappings.");
        }

        Path lock = paths.controlDir().resolve("migration.lock");
        Files.createDirectories(paths.controlDir());
        if (Files.exists(lock)) {
            throw new IOException("Another UUIDBridge migration appears to be running: " + lock);
        }
        Files.writeString(lock, plan.id(), StandardCharsets.UTF_8);

        List<PlannedChange> changed = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        BackupManager backups = new BackupManager(paths, plan.id());

        try {
            applyRewrites(paths, plan, backups, changed, errors);
            applyRenames(paths, plan, backups, changed, skipped, errors);
        } finally {
            Files.deleteIfExists(lock);
        }

        MigrationReport report = new MigrationReport(
            plan.id(),
            plan.direction(),
            List.copyOf(changed),
            List.copyOf(skipped),
            List.copyOf(errors),
            backups.root().toString(),
            checksum(changed, skipped, errors),
            Instant.now().toString()
        );
        JsonCodecs.write(paths.reportPath(plan.id()), report);
        return report;
    }

    private static void applyRewrites(
        UuidBridgePaths paths,
        MigrationPlan plan,
        BackupManager backups,
        List<PlannedChange> changed,
        List<String> errors
    ) throws IOException {
        for (Path file : WorldFileScanner.discover(paths)) {
            try {
                FileChangeResult preview = FileMigrator.preview(file, plan.mappings());
                if (!preview.changed()) {
                    continue;
                }
                backups.backup(file);
                long replacements = FileMigrator.rewrite(file, plan.mappings());
                if (replacements > 0) {
                    changed.add(new PlannedChange(MigrationPlanner.label(paths, file), replacements, "rewrite"));
                }
            } catch (IOException exception) {
                errors.add(MigrationPlanner.label(paths, file) + ": " + exception.getMessage());
            }
        }
    }

    private static void applyRenames(
        UuidBridgePaths paths,
        MigrationPlan plan,
        BackupManager backups,
        List<PlannedChange> changed,
        List<String> skipped,
        List<String> errors
    ) throws IOException {
        for (Path file : WorldFileScanner.playerUuidFiles(paths)) {
            String newName = MigrationPlanner.renamedPlayerFile(file, plan.mappings());
            if (newName == null) {
                continue;
            }
            Path target = file.resolveSibling(newName);
            if (Files.exists(target)) {
                skipped.add(MigrationPlanner.label(paths, file) + " target already exists: " + target.getFileName());
                continue;
            }
            try {
                backups.backup(file);
                SafeFileWriter.moveAtomic(file, target);
                changed.add(new PlannedChange(MigrationPlanner.label(paths, file) + " -> " + target.getFileName(), 1, "rename"));
            } catch (IOException exception) {
                errors.add(MigrationPlanner.label(paths, file) + ": " + exception.getMessage());
            }
        }
    }

    private static String checksum(List<PlannedChange> changed, List<String> skipped, List<String> errors) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (PlannedChange change : changed) {
                digest.update(change.toString().getBytes(StandardCharsets.UTF_8));
            }
            for (String value : skipped) {
                digest.update(value.getBytes(StandardCharsets.UTF_8));
            }
            for (String value : errors) {
                digest.update(value.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable", exception);
        }
    }
}
