package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.adapter.DataAdapters;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldFileScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsTargetsFileWithGlobExcludeAndReportsLargeBinarySkip() throws Exception {
        Path gameDir = tempDir.resolve("server");
        Path worldDir = gameDir.resolve("world");
        Files.createDirectories(gameDir.resolve("config/claims"));
        Files.createDirectories(gameDir.resolve("config/backups"));
        Files.createDirectories(worldDir);
        Files.writeString(gameDir.resolve("config/claims/alice.json"), "{\"owner\":\"x\"}");
        Files.writeString(gameDir.resolve("config/backups/old.json"), "{\"owner\":\"x\"}");
        Path large = gameDir.resolve("config/claims/blob.bin");
        try (var channel = Files.newByteChannel(large, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            channel.position(DataAdapters.DEFAULT_BINARY_SIZE_LIMIT);
            channel.write(ByteBuffer.wrap(new byte[] {0}));
        }
        Path targets = gameDir.resolve("uuidbridge/targets.json");
        Files.createDirectories(targets.getParent());
        Files.writeString(targets, """
            {
              "include": [
                {"path": "config/claims/*.json", "format": "json"}
              ],
              "exclude": ["config/backups/**"]
            }
            """);

        UuidBridgePaths paths = UuidBridgePaths.create(gameDir, worldDir);
        var files = WorldFileScanner.discoverTargets(paths, Optional.of(targets));
        CoverageReport coverage = WorldFileScanner.coverage(paths, List.of(
            new DiscoveredFile(large, DataAdapters.BINARY, DataAdapters.BINARY, "test", false)
        ), 0);

        assertTrue(files.stream().anyMatch(file -> file.path().endsWith("alice.json")));
        assertTrue(files.stream().noneMatch(file -> file.path().endsWith("old.json")));
        assertEquals(1, coverage.skippedFiles());
    }

    @Test
    void rejectsTargetPathTraversal() throws Exception {
        Path gameDir = tempDir.resolve("server");
        Path worldDir = gameDir.resolve("world");
        Files.createDirectories(gameDir.resolve("uuidbridge"));
        Path targets = gameDir.resolve("uuidbridge/targets.json");
        Files.writeString(targets, """
            {
              "include": [
                {"path": "../outside.json", "format": "json"}
              ]
            }
            """);

        UuidBridgePaths paths = UuidBridgePaths.create(gameDir, worldDir);

        assertThrows(SecurityException.class, () -> WorldFileScanner.discoverTargets(paths, Optional.of(targets)));
    }
}
