package dev.kara.uuidbridge;

import dev.kara.uuidbridge.migration.PendingMigrationRunner;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

public final class UuidBridge {
    public static final String MOD_ID = "uuidbridge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean initialized;
    private static Path gameDir;

    private UuidBridge() {
    }

    public static synchronized void init(Path loaderGameDir) {
        gameDir = loaderGameDir;
        if (initialized) {
            return;
        }
        initialized = true;
        LOGGER.info("UUIDBridge initialized.");
    }

    public static Path gameDir() {
        if (gameDir == null) {
            throw new IllegalStateException("UUIDBridge was not initialized with a game directory.");
        }
        return gameDir;
    }

    public static UuidBridgePaths paths(MinecraftServer server) {
        return UuidBridgePaths.create(gameDir(), server.getWorldPath(LevelResource.ROOT));
    }

    public static void runPendingMigration(MinecraftServer server) {
        try {
            PendingMigrationRunner.runIfPresent(paths(server));
        } catch (Exception exception) {
            LOGGER.error("UUIDBridge failed to apply pending migration.", exception);
            throw new IllegalStateException("UUIDBridge pending migration failed", exception);
        }
    }
}
