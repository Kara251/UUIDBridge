package dev.kara.uuidbridge.migration.io;

import dev.kara.uuidbridge.migration.PendingAction;
import dev.kara.uuidbridge.migration.PendingMigration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonCodecsTest {
    @TempDir
    Path tempDir;

    @Test
    void failedAtomicWriteLeavesExistingJsonReadable() throws Exception {
        Assumptions.assumeTrue(!"root".equals(System.getProperty("user.name")));
        Assumptions.assumeTrue(tempDir.getFileSystem().supportedFileAttributeViews().contains("posix"));
        Path directory = tempDir.resolve("control");
        Files.createDirectories(directory);
        Path pending = directory.resolve("pending.json");
        PendingMigration oldValue = new PendingMigration(PendingAction.APPLY, "old", "before", "test");
        JsonCodecs.write(pending, oldValue);

        Set<PosixFilePermission> originalPermissions = Files.getPosixFilePermissions(directory);
        try {
            Files.setPosixFilePermissions(directory, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_EXECUTE
            ));
            assertThrows(java.io.IOException.class, () ->
                JsonCodecs.write(pending, new PendingMigration(PendingAction.ROLLBACK, "new", "after", "test")));
        } finally {
            Files.setPosixFilePermissions(directory, originalPermissions);
        }

        PendingMigration readBack = JsonCodecs.read(pending, PendingMigration.class);
        assertEquals("old", readBack.planId());
        assertEquals(PendingAction.APPLY, readBack.action());
    }
}
