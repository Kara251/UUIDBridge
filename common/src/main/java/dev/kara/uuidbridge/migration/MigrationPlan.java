package dev.kara.uuidbridge.migration;

import java.util.List;

public record MigrationPlan(
    String id,
    MigrationDirection direction,
    List<UuidMapping> mappings,
    List<String> targetPaths,
    List<PlannedChange> estimatedChanges,
    List<PlanConflict> conflicts,
    List<MissingMapping> missingMappings,
    String createdAt,
    boolean allowNetwork
) {
    public boolean canApply() {
        return conflicts.isEmpty() && missingMappings.isEmpty() && !mappings.isEmpty();
    }
}
