package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class WorldFileScanner {
    private static final Set<String> SERVER_JSON_FILES = Set.of(
        "whitelist.json",
        "ops.json",
        "banned-players.json",
        "usercache.json"
    );

    private WorldFileScanner() {
    }

    public static List<Path> discover(UuidBridgePaths paths) throws IOException {
        List<Path> result = new ArrayList<>();
        for (String file : SERVER_JSON_FILES) {
            Path path = paths.gameDir().resolve(file);
            if (Files.isRegularFile(path)) {
                result.add(path);
            }
        }
        addIfExists(result, paths.worldDir().resolve("level.dat"));
        addDirectoryFiles(result, paths.worldDir().resolve("data"), Set.of(".dat"));
        addDirectoryFiles(result, paths.worldDir().resolve("playerdata"), Set.of(".dat"));
        addDirectoryFiles(result, paths.worldDir().resolve("advancements"), Set.of(".json"));
        addDirectoryFiles(result, paths.worldDir().resolve("stats"), Set.of(".json"));
        addRecursiveWorldFiles(result, paths.worldDir());
        return result.stream().distinct().toList();
    }

    public static List<Path> playerUuidFiles(UuidBridgePaths paths) throws IOException {
        List<Path> result = new ArrayList<>();
        addDirectoryFiles(result, paths.worldDir().resolve("playerdata"), Set.of(".dat"));
        addDirectoryFiles(result, paths.worldDir().resolve("advancements"), Set.of(".json"));
        addDirectoryFiles(result, paths.worldDir().resolve("stats"), Set.of(".json"));
        return result;
    }

    private static void addRecursiveWorldFiles(List<Path> result, Path worldDir) throws IOException {
        if (!Files.isDirectory(worldDir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(worldDir)) {
            stream.filter(Files::isRegularFile)
                .filter(WorldFileScanner::isWorldDataFile)
                .forEach(result::add);
        }
    }

    private static boolean isWorldDataFile(Path path) {
        String normalized = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return normalized.endsWith(".mca")
            || normalized.endsWith(".dat")
            || normalized.endsWith(".dat_old");
    }

    private static void addDirectoryFiles(List<Path> result, Path directory, Set<String> extensions) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                .filter(path -> extensions.stream().anyMatch(ext -> path.getFileName().toString().endsWith(ext)))
                .forEach(result::add);
        }
    }

    private static void addIfExists(List<Path> result, Path path) {
        if (Files.isRegularFile(path)) {
            result.add(path);
        }
    }
}
