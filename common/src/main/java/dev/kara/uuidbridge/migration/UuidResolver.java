package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.io.MappingFileParser;
import dev.kara.uuidbridge.migration.io.MojangProfileClient;
import dev.kara.uuidbridge.migration.rewrite.OfflineUuid;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class UuidResolver {
    private final MojangProfileClient mojangProfileClient;

    public UuidResolver() {
        this(new MojangProfileClient());
    }

    UuidResolver(MojangProfileClient mojangProfileClient) {
        this.mojangProfileClient = mojangProfileClient;
    }

    public ResolvedMappings resolve(
        MigrationDirection direction,
        List<KnownPlayer> knownPlayers,
        Optional<Path> mappingFile,
        boolean allowNetwork
    ) throws IOException {
        Map<String, UuidMapping> mappings = new LinkedHashMap<>();
        List<MissingMapping> missing = new ArrayList<>();

        if (mappingFile.isPresent()) {
            for (UuidMapping mapping : MappingFileParser.parse(mappingFile.get(), direction)) {
                mappings.put(mapping.name().toLowerCase(java.util.Locale.ROOT), mapping);
            }
        }

        for (KnownPlayer player : knownPlayers) {
            String name = player.name();
            if (name == null || name.isBlank()) {
                UUID knownUuid = player.onlineUuid().orElseGet(() -> player.offlineUuid().orElse(null));
                missing.add(new MissingMapping(player.displayName(), knownUuid, "Missing player name; provide a mapping file."));
                continue;
            }
            String key = name.toLowerCase(java.util.Locale.ROOT);
            if (mappings.containsKey(key)) {
                continue;
            }
            UUID offline = player.offlineUuid().orElseGet(() -> OfflineUuid.forName(name));
            Optional<UUID> online = player.onlineUuid()
                .filter(candidate -> !candidate.equals(offline));
            if (online.isEmpty() && allowNetwork) {
                try {
                    online = mojangProfileClient.lookupOnlineUuid(name);
                    Thread.sleep(250L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while resolving Mojang profile", interrupted);
                }
            }
            if (online.isPresent()) {
                mappings.put(key, new UuidMapping(name, online.get(), offline, direction, MappingSource.USERCACHE));
            } else {
                missing.add(new MissingMapping(name, offline, "Missing online UUID; provide a mapping file or allow network lookup."));
            }
        }

        return new ResolvedMappings(List.copyOf(mappings.values()), List.copyOf(missing));
    }

    public record ResolvedMappings(List<UuidMapping> mappings, List<MissingMapping> missingMappings) {
    }
}
