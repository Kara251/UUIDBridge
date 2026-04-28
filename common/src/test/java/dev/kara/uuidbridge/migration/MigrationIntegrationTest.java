package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.io.JsonCodecs;
import dev.kara.uuidbridge.migration.io.UuidBridgePaths;
import dev.kara.uuidbridge.migration.rewrite.OfflineUuid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationIntegrationTest {
    private static final UUID ONLINE_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID OTHER_UUID = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final String NAME = "Alice";
    private static final UUID OFFLINE_UUID = OfflineUuid.forName(NAME);

    @TempDir
    Path tempDir;

    @Test
    void migratesOnlineToOfflineAcrossWorldFilesAndWritesManifest() throws Exception {
        UuidBridgePaths paths = fixture(ONLINE_UUID, ONLINE_UUID, Optional.empty());
        MigrationService service = new MigrationService();

        MigrationPlan plan = service.createPlan(paths, MigrationDirection.ONLINE_TO_OFFLINE, Optional.empty(), false);
        assertTrue(plan.canApply());
        service.markPending(paths, plan.id());
        MigrationReport report = service.executePending(paths);

        assertTrue(report.successful());
        assertFalse(Files.exists(paths.pendingFile()));
        assertFalse(service.hasLock(paths));
        assertTrue(Files.exists(paths.worldDir().resolve("playerdata").resolve(OFFLINE_UUID + ".dat")));
        assertFalse(Files.exists(paths.worldDir().resolve("playerdata").resolve(ONLINE_UUID + ".dat")));
        assertContains(paths.gameDir().resolve("whitelist.json"), OFFLINE_UUID.toString());
        assertContains(paths.worldDir().resolve("advancements").resolve(OFFLINE_UUID + ".json"), OFFLINE_UUID.toString());
        assertGzipContains(paths.worldDir().resolve("playerdata").resolve(OFFLINE_UUID + ".dat"), OFFLINE_UUID.toString());
        assertRegionContains(paths.worldDir().resolve("entities").resolve("r.0.0.mca"), OFFLINE_UUID.toString());
        assertRegionContains(paths.worldDir().resolve("entities").resolve("r.0.0.mca"), OTHER_UUID.toString());
        assertRegionMissing(paths.worldDir().resolve("entities").resolve("r.0.0.mca"), ONLINE_UUID.toString());

        BackupManifest manifest = JsonCodecs.read(Path.of(report.backupPath()).resolve("manifest.json"), BackupManifest.class);
        assertTrue(manifest.complete());
        assertTrue(manifest.files().stream().anyMatch(entry -> entry.originalPath().contains("playerdata")));
    }

    @Test
    void migratesOfflineToOnlineWithMappingFile() throws Exception {
        UuidBridgePaths paths = fixture(OFFLINE_UUID, OFFLINE_UUID, Optional.of(mappingFile()));
        MigrationService service = new MigrationService();

        MigrationPlan plan = service.createPlan(paths, MigrationDirection.OFFLINE_TO_ONLINE,
            Optional.of("mapping.csv"), false);
        assertTrue(plan.canApply());
        service.markPending(paths, plan.id());
        MigrationReport report = service.executePending(paths);

        assertTrue(report.successful());
        assertTrue(Files.exists(paths.worldDir().resolve("playerdata").resolve(ONLINE_UUID + ".dat")));
        assertFalse(Files.exists(paths.worldDir().resolve("playerdata").resolve(OFFLINE_UUID + ".dat")));
        assertContains(paths.gameDir().resolve("ops.json"), ONLINE_UUID.toString());
        assertGzipContains(paths.worldDir().resolve("playerdata").resolve(ONLINE_UUID + ".dat"), ONLINE_UUID.toString());
    }

    @Test
    void keepsPendingLockAndReportWhenTargetFileAlreadyExists() throws Exception {
        UuidBridgePaths paths = fixture(ONLINE_UUID, ONLINE_UUID, Optional.empty());
        MigrationService service = new MigrationService();
        MigrationPlan plan = service.createPlan(paths, MigrationDirection.ONLINE_TO_OFFLINE, Optional.empty(), false);

        service.markPending(paths, plan.id());
        Files.writeString(paths.worldDir().resolve("playerdata").resolve(OFFLINE_UUID + ".dat"), "existing");
        assertThrows(java.io.IOException.class, () -> service.executePending(paths));
        MigrationReport report = JsonCodecs.read(paths.reportPath(plan.id()), MigrationReport.class);

        assertFalse(report.successful());
        assertTrue(Files.exists(paths.pendingFile()));
        assertTrue(service.hasLock(paths));
        assertTrue(Files.exists(paths.reportPath(plan.id())));
        BackupManifest manifest = JsonCodecs.read(paths.backupPath(plan.id()).resolve("manifest.json"), BackupManifest.class);
        assertFalse(manifest.complete());
    }

    @Test
    void damagedRegionIsReportedWithoutStoppingOtherFiles() throws Exception {
        UuidBridgePaths paths = fixture(ONLINE_UUID, ONLINE_UUID, Optional.empty());
        MigrationService service = new MigrationService();
        MigrationPlan plan = service.createPlan(paths, MigrationDirection.ONLINE_TO_OFFLINE, Optional.empty(), false);
        Files.write(paths.worldDir().resolve("entities").resolve("r.1.0.mca"), damagedRegionWithTargetUuid());

        service.markPending(paths, plan.id());
        assertThrows(java.io.IOException.class, () -> service.executePending(paths));
        MigrationReport report = JsonCodecs.read(paths.reportPath(plan.id()), MigrationReport.class);

        assertFalse(report.successful());
        assertTrue(report.errors().stream().anyMatch(error -> error.contains("r.1.0.mca")));
        assertTrue(Files.exists(paths.worldDir().resolve("playerdata").resolve(OFFLINE_UUID + ".dat")));
        assertTrue(Files.exists(paths.pendingFile()));
        assertTrue(service.hasLock(paths));
    }

    private UuidBridgePaths fixture(UUID fileUuid, UUID contentUuid, Optional<Path> mappingFile) throws Exception {
        Path gameDir = tempDir.resolve("server");
        Path worldDir = gameDir.resolve("world");
        Files.createDirectories(worldDir.resolve("playerdata"));
        Files.createDirectories(worldDir.resolve("advancements"));
        Files.createDirectories(worldDir.resolve("stats"));
        Files.createDirectories(worldDir.resolve("entities"));

        Files.writeString(gameDir.resolve("usercache.json"), """
            [
              {
                "name": "%s",
                "uuid": "%s"
              }
            ]
            """.formatted(NAME, contentUuid));
        Files.writeString(gameDir.resolve("whitelist.json"), listJson(contentUuid));
        Files.writeString(gameDir.resolve("ops.json"), listJson(contentUuid));
        Files.writeString(gameDir.resolve("banned-players.json"), listJson(contentUuid));

        Files.write(worldDir.resolve("playerdata").resolve(fileUuid + ".dat"), gzip("""
            {"Owner":"%s","id":"minecraft:wolf","Other":"%s"}
            """.formatted(contentUuid, OTHER_UUID)));
        Files.writeString(worldDir.resolve("advancements").resolve(fileUuid + ".json"), """
            {"criteria":{"uuid":"%s","other":"%s"}}
            """.formatted(contentUuid, OTHER_UUID));
        Files.writeString(worldDir.resolve("stats").resolve(fileUuid + ".json"), """
            {"stats":{"minecraft:custom":{"uuid":"%s"}}}
            """.formatted(contentUuid));
        Files.write(worldDir.resolve("entities").resolve("r.0.0.mca"), region("""
            {"id":"touhou_little_maid:maid","owner_uuid":"%s","Other":"%s"}
            """.formatted(contentUuid, OTHER_UUID)));

        if (mappingFile.isPresent()) {
            Files.copy(mappingFile.get(), gameDir.resolve("mapping.csv"));
        }
        return UuidBridgePaths.create(gameDir, worldDir);
    }

    private Path mappingFile() throws Exception {
        Path mapping = tempDir.resolve("mapping.csv");
        Files.writeString(mapping, "name,onlineUuid,offlineUuid\n%s,%s,%s\n".formatted(NAME, ONLINE_UUID, OFFLINE_UUID));
        return mapping;
    }

    private static String listJson(UUID uuid) {
        return """
            [
              {
                "name": "%s",
                "uuid": "%s"
              }
            ]
            """.formatted(NAME, uuid);
    }

    private static void assertContains(Path path, String text) throws Exception {
        assertTrue(Files.readString(path).contains(text), path + " should contain " + text);
    }

    private static void assertGzipContains(Path path, String text) throws Exception {
        try (GZIPInputStream input = new GZIPInputStream(Files.newInputStream(path))) {
            String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(content.contains(text), path + " should contain " + text);
        }
    }

    private static void assertRegionContains(Path path, String text) throws Exception {
        assertTrue(regionContent(path).contains(text), path + " should contain " + text);
    }

    private static void assertRegionMissing(Path path, String text) throws Exception {
        assertFalse(regionContent(path).contains(text), path + " should not contain " + text);
    }

    private static String regionContent(Path path) throws Exception {
        byte[] region = Files.readAllBytes(path);
        int location = ByteBuffer.wrap(region, 0, 4).getInt();
        int offset = ((location >> 8) & 0xFFFFFF) * 4096;
        int length = ByteBuffer.wrap(region, offset, 4).getInt();
        byte[] compressed = java.util.Arrays.copyOfRange(region, offset + 5, offset + 4 + length);
        try (InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] gzip(String content) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(output)) {
            gzip.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }

    private static byte[] region(String payload) throws Exception {
        byte[] compressed = zlib(payload.getBytes(StandardCharsets.UTF_8));
        byte[] chunk = ByteBuffer.allocate(5 + compressed.length)
            .putInt(compressed.length + 1)
            .put((byte) 2)
            .put(compressed)
            .array();
        int sectors = Math.max(1, (chunk.length + 4095) / 4096);
        ByteBuffer region = ByteBuffer.allocate(8192 + sectors * 4096);
        region.putInt((2 << 8) | sectors);
        region.position(8192);
        region.put(chunk);
        return region.array();
    }

    private static byte[] damagedRegionWithTargetUuid() {
        byte[] payload = ONLINE_UUID.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer region = ByteBuffer.allocate(8192 + 4096);
        region.putInt((2 << 8) | 1);
        region.position(8192);
        region.putInt(payload.length + 1);
        region.put((byte) 99);
        region.put(payload);
        return region.array();
    }

    private static byte[] zlib(byte[] payload) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(output)) {
            deflater.write(payload);
        }
        return output.toByteArray();
    }
}
