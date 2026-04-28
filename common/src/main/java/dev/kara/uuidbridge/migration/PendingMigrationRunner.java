package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.UuidBridge;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import java.io.IOException;

public final class PendingMigrationRunner {
    private PendingMigrationRunner() {
    }

    public static void runIfPresent(UuidBridgePaths paths) throws IOException {
        MigrationService service = new MigrationService();
        var pending = service.pendingMigration(paths);
        if (pending.isEmpty()) {
            return;
        }
        UuidBridge.LOGGER.warn("UUIDBridge pending {} found for {}; running before server is available for play.",
            pending.get().action().name().toLowerCase(java.util.Locale.ROOT), pending.get().planId());
        MigrationReport report = service.executePending(paths);
        UuidBridge.LOGGER.info("UUIDBridge {} {} finished with state {} and {} changed files.",
            report.action().name().toLowerCase(java.util.Locale.ROOT),
            report.planId(),
            report.finalState(),
            report.changedFiles().size());
    }
}
