package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.UuidBridge;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import java.io.IOException;

public final class PendingMigrationRunner {
    private PendingMigrationRunner() {
    }

    public static void runIfPresent(UuidBridgePaths paths) throws IOException {
        MigrationService service = new MigrationService();
        if (service.pendingPlan(paths).isEmpty()) {
            return;
        }
        UuidBridge.LOGGER.warn("UUIDBridge pending migration found; applying before server is available for play.");
        MigrationReport report = service.executePending(paths);
        UuidBridge.LOGGER.info("UUIDBridge migration {} finished with {} changed files.",
            report.planId(), report.changedFiles().size());
    }
}
