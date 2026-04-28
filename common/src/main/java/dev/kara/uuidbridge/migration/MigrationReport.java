package dev.kara.uuidbridge.migration;

import java.util.List;

public record MigrationReport(
    String planId,
    MigrationDirection direction,
    List<PlannedChange> changedFiles,
    List<String> skipped,
    List<String> errors,
    String backupPath,
    String checksum,
    String createdAt
) {
    public boolean successful() {
        return errors.isEmpty();
    }
}
