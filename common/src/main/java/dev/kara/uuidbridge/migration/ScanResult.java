package dev.kara.uuidbridge.migration;

import java.util.List;

public record ScanResult(
    MigrationDirection direction,
    int knownPlayers,
    int mappings,
    List<PlanConflict> conflicts,
    List<MissingMapping> missingMappings,
    List<PlannedChange> estimatedChanges
) {
}
