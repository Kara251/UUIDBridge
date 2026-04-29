package dev.kara.uuidbridge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.kara.uuidbridge.UuidBridge;
import dev.kara.uuidbridge.migration.BackupManifest;
import dev.kara.uuidbridge.migration.MigrationLock;
import dev.kara.uuidbridge.migration.MigrationDirection;
import dev.kara.uuidbridge.migration.MigrationPlan;
import dev.kara.uuidbridge.migration.MigrationService;
import dev.kara.uuidbridge.migration.PendingMigration;
import dev.kara.uuidbridge.migration.ScanResult;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class UuidBridgeCommands {
    private static final MigrationService SERVICE = new MigrationService();

    private UuidBridgeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("uuidbridge")
            .requires(source -> source.hasPermission(4))
            .then(literal("scan")
                .then(argument("direction", StringArgumentType.word())
                    .executes(context -> scan(context.getSource(),
                        StringArgumentType.getString(context, "direction"), ""))
                    .then(argument("options", StringArgumentType.greedyString())
                        .executes(context -> scan(context.getSource(),
                            StringArgumentType.getString(context, "direction"),
                            StringArgumentType.getString(context, "options"))))))
            .then(literal("plan")
                .then(argument("direction", StringArgumentType.word())
                    .executes(context -> plan(context.getSource(),
                        StringArgumentType.getString(context, "direction"), ""))
                    .then(argument("options", StringArgumentType.greedyString())
                        .executes(context -> plan(context.getSource(),
                            StringArgumentType.getString(context, "direction"),
                            StringArgumentType.getString(context, "options"))))))
            .then(literal("apply")
                .then(argument("planId", StringArgumentType.word())
                    .then(argument("options", StringArgumentType.greedyString())
                        .executes(context -> apply(context.getSource(),
                            StringArgumentType.getString(context, "planId"),
                            StringArgumentType.getString(context, "options"))))))
            .then(literal("status")
                .executes(context -> status(context.getSource())))
            .then(literal("rollback")
                .then(argument("planId", StringArgumentType.word())
                    .then(argument("options", StringArgumentType.greedyString())
                        .executes(context -> rollback(context.getSource(),
                            StringArgumentType.getString(context, "planId"),
                            StringArgumentType.getString(context, "options"))))))
            .then(literal("cancel")
                .then(argument("planId", StringArgumentType.word())
                    .executes(context -> cancel(context.getSource(),
                        StringArgumentType.getString(context, "planId"))))));
    }

    private static int scan(CommandSourceStack source, String directionValue, String rawOptions) {
        try {
            CommandOptions options = CommandOptions.parse(rawOptions);
            if (rejectUnknownOptions(source, options)) {
                return 0;
            }
            MigrationDirection direction = MigrationDirection.parse(directionValue);
            ScanResult result = SERVICE.scan(paths(source), direction, options.mapping(), options.targets(),
                options.singleplayerName());
            long replacements = result.estimatedChanges().stream()
                .mapToLong(change -> change.replacements())
                .sum();
            send(source, "UUIDBridge scan: players=" + result.knownPlayers()
                + ", mappings=" + result.mappings()
                + ", filesWithChanges=" + result.estimatedChanges().size()
                + ", estimatedReplacements=" + replacements + ".");
            send(source, "Coverage: scanned=" + result.coverage().scannedFiles()
                + ", skipped=" + result.coverage().skippedFiles()
                + ", targets=" + result.coverage().targets().size() + ".");
            if (!result.conflicts().isEmpty()) {
                send(source, "Conflicts: " + result.conflicts().size()
                    + " target UUID collision(s); inspect the generated mapping inputs.");
            }
            if (!result.missingMappings().isEmpty()) {
                send(source, "Missing mappings: " + result.missingMappings().size()
                    + "; provide --mapping <file>.");
            }
            return result.conflicts().isEmpty() && result.missingMappings().isEmpty() ? 1 : 0;
        } catch (Exception exception) {
            fail(source, exception);
            return 0;
        }
    }

    private static int plan(CommandSourceStack source, String directionValue, String rawOptions) {
        try {
            CommandOptions options = CommandOptions.parse(rawOptions);
            if (rejectUnknownOptions(source, options)) {
                return 0;
            }
            MigrationDirection direction = MigrationDirection.parse(directionValue);
            MigrationPlan plan = SERVICE.createPlan(paths(source), direction, options.mapping(), options.targets(),
                options.singleplayerName());
            send(source, "UUIDBridge plan created: " + plan.id());
            long replacements = plan.estimatedChanges().stream()
                .mapToLong(change -> change.replacements())
                .sum();
            send(source, "Mappings: " + plan.mappings().size()
                + ", filesWithChanges: " + plan.estimatedChanges().size()
                + ", estimatedReplacements: " + replacements
                + ", conflicts: " + plan.conflicts().size()
                + ", missing: " + plan.missingMappings().size());
            send(source, "Coverage: scanned=" + plan.coverage().scannedFiles()
                + ", skipped=" + plan.coverage().skippedFiles()
                + ", targets=" + plan.coverage().targets().size());
            if (plan.singleplayerPlayerCopy() != null) {
                send(source, "Singleplayer Player copy planned for " + plan.singleplayerPlayerCopy().name() + ".");
            }
            if (plan.canApply()) {
                send(source, "Use /uuidbridge apply " + plan.id() + " --confirm, then restart the server.");
            } else {
                send(source, "Plan cannot be applied: fix conflicts and missing mappings first.");
            }
            return plan.canApply() ? 1 : 0;
        } catch (Exception exception) {
            fail(source, exception);
            return 0;
        }
    }

    private static int apply(CommandSourceStack source, String planId, String rawOptions) {
        try {
            CommandOptions options = CommandOptions.parse(rawOptions);
            if (rejectUnknownOptions(source, options)) {
                return 0;
            }
            if (!options.confirm()) {
                fail(source, "Refusing to apply without --confirm.");
                return 0;
            }
            SERVICE.markPendingApply(paths(source), planId, source.getTextName());
            send(source, "UUIDBridge plan marked pending: " + planId);
            send(source, "Restart the server to apply it before the world is used.");
            return 1;
        } catch (Exception exception) {
            fail(source, exception);
            return 0;
        }
    }

    private static int status(CommandSourceStack source) {
        try {
            UuidBridgePaths paths = paths(source);
            Optional<PendingMigration> pending = SERVICE.pendingMigration(paths);
            Optional<Path> latestReport = SERVICE.latestReport(paths);
            Optional<MigrationLock> lock = SERVICE.lock(paths);
            send(source, "UUIDBridge pending: " + pending
                .map(value -> value.action().name().toLowerCase(java.util.Locale.ROOT) + " " + value.planId())
                .orElse("none"));
            send(source, "UUIDBridge latest report: " + latestReport.map(Path::toString).orElse("none"));
            send(source, "UUIDBridge migration lock: " + lock
                .map(value -> value.action().name().toLowerCase(java.util.Locale.ROOT) + " " + value.planId())
                .orElse("none"));
            Optional<String> planForManifest = pending.map(PendingMigration::planId)
                .or(() -> latestReport.flatMap(path -> reportPlanId(path.getFileName().toString())));
            if (pending.isPresent()) {
                Optional<MigrationPlan> pendingPlan = SERVICE.plan(paths, pending.get().planId());
                if (pendingPlan.isPresent()) {
                    CoverageLine coverage = coverageLine(pendingPlan.get());
                    send(source, "UUIDBridge coverage: scanned=" + coverage.scanned()
                        + ", skipped=" + coverage.skipped()
                        + ", targets=" + coverage.targets());
                }
            }
            if (planForManifest.isPresent()) {
                Optional<BackupManifest> manifest = SERVICE.backupManifest(paths, planForManifest.get());
                send(source, "UUIDBridge backup manifest: " + manifest
                    .map(value -> value.complete() ? "complete" : "incomplete")
                    .orElse("none"));
            } else {
                send(source, "UUIDBridge backup manifest: none");
            }
            return pending.isPresent() || latestReport.isPresent() ? 1 : 0;
        } catch (Exception exception) {
            fail(source, exception);
            return 0;
        }
    }

    private static int rollback(CommandSourceStack source, String planId, String rawOptions) {
        try {
            CommandOptions options = CommandOptions.parse(rawOptions);
            if (rejectUnknownOptions(source, options)) {
                return 0;
            }
            if (!options.confirm()) {
                fail(source, "Refusing to rollback without --confirm.");
                return 0;
            }
            SERVICE.markPendingRollback(paths(source), planId, source.getTextName());
            send(source, "UUIDBridge rollback marked pending: " + planId);
            send(source, "Restart the server to restore files from the backup manifest.");
            return 1;
        } catch (Exception exception) {
            fail(source, exception);
            return 0;
        }
    }

    private static int cancel(CommandSourceStack source, String planId) {
        try {
            boolean canceled = SERVICE.cancel(paths(source), planId);
            send(source, canceled ? "UUIDBridge canceled pending plan: " + planId : "No matching pending plan.");
            return canceled ? 1 : 0;
        } catch (Exception exception) {
            fail(source, exception);
            return 0;
        }
    }

    private static UuidBridgePaths paths(CommandSourceStack source) {
        return UuidBridge.paths(source.getServer());
    }

    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    private static void fail(CommandSourceStack source, Exception exception) {
        fail(source, exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
    }

    private static void fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal("UUIDBridge: " + message));
    }

    private static boolean rejectUnknownOptions(CommandSourceStack source, CommandOptions options) {
        if (options.unknownOptions().isEmpty()) {
            return false;
        }
        fail(source, "Unknown option(s): " + String.join(", ", options.unknownOptions()));
        return true;
    }

    private static Optional<String> reportPlanId(String fileName) {
        if (!fileName.endsWith(".json")) {
            return Optional.empty();
        }
        String stem = fileName.substring(0, fileName.length() - ".json".length());
        if (stem.endsWith("-rollback")) {
            return Optional.of(stem.substring(0, stem.length() - "-rollback".length()));
        }
        return Optional.of(stem);
    }

    private static CoverageLine coverageLine(MigrationPlan plan) {
        return new CoverageLine(
            plan.coverage().scannedFiles(),
            plan.coverage().skippedFiles(),
            plan.coverage().targets().size()
        );
    }

    private record CoverageLine(long scanned, long skipped, long targets) {
    }
}
