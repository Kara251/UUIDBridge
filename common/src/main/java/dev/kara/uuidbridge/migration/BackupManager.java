package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class BackupManager {
    private final UuidBridgePaths paths;
    private final Path backupRoot;

    public BackupManager(UuidBridgePaths paths, String planId) {
        this.paths = paths;
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
            return;
        }
        Files.createDirectories(target.getParent());
        Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
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
}
