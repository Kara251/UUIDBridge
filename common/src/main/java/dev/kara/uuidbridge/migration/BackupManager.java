package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.io.JsonCodecs;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class BackupManager {
    private final UuidBridgePaths paths;
    private final String planId;
    private final Path backupRoot;
    private final List<BackupEntry> entries = new ArrayList<>();
    private final String createdAt = Instant.now().toString();

    public BackupManager(UuidBridgePaths paths, String planId) {
        this.paths = paths;
        this.planId = planId;
        this.backupRoot = paths.backupPath(planId);
    }

    public Path root() {
        return backupRoot;
    }

    public void backup(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Path relative = relative(file);
        Path target = backupRoot.resolve(relative);
        if (Files.exists(target)) {
            addEntry(file, target);
            writeManifest(false);
            return;
        }
        Files.createDirectories(target.getParent());
        Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
        addEntry(file, target);
        writeManifest(false);
    }

    public void writeManifest(boolean complete) throws IOException {
        JsonCodecs.write(backupRoot.resolve("manifest.json"), new BackupManifest(
            planId,
            createdAt,
            Instant.now().toString(),
            complete,
            List.copyOf(entries)
        ));
    }

    private void addEntry(Path original, Path backup) throws IOException {
        String originalLabel = MigrationPlanner.label(paths, original);
        String backupLabel = backupRoot.relativize(backup).toString();
        boolean alreadyTracked = entries.stream()
            .anyMatch(entry -> entry.originalPath().equals(originalLabel));
        if (alreadyTracked) {
            return;
        }
        entries.add(new BackupEntry(originalLabel, backupLabel, Files.size(backup), sha256(backup)));
    }

    private Path relative(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        Path game = paths.gameDir().toAbsolutePath().normalize();
        Path world = paths.worldDir().toAbsolutePath().normalize();
        if (normalized.startsWith(world)) {
            return Path.of("world").resolve(world.relativize(normalized));
        }
        if (normalized.startsWith(game)) {
            return Path.of("game").resolve(game.relativize(normalized));
        }
        return Path.of("external").resolve(file.getFileName());
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable", exception);
        }
    }
}
