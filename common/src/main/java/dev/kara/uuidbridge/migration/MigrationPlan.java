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
    boolean allowNetwork,
    CoverageReport coverage,
    SingleplayerPlayerCopy singleplayerPlayerCopy,
    String targetsFile
) {
    public MigrationPlan(
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
        this(id, direction, mappings, targetPaths, estimatedChanges, conflicts, missingMappings,
            createdAt, allowNetwork, CoverageReport.empty(), null, "");
    }

    public MigrationPlan {
        coverage = coverage == null ? CoverageReport.empty() : coverage;
        targetsFile = targetsFile == null ? "" : targetsFile;
    }

    public boolean canApply() {
        return conflicts.isEmpty() && missingMappings.isEmpty() && !mappings.isEmpty();
    }
}
