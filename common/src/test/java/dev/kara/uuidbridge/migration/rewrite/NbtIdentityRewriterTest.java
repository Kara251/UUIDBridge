package dev.kara.uuidbridge.migration.rewrite;

import dev.kara.uuidbridge.migration.FileChangeResult;
import dev.kara.uuidbridge.migration.MappingSource;
import dev.kara.uuidbridge.migration.MigrationDirection;
import dev.kara.uuidbridge.migration.UuidMapping;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NbtIdentityRewriterTest {
    private static final UUID ONLINE = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID OFFLINE = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UuidMapping MAPPING = new UuidMapping(
        "Alice",
        ONLINE,
        OFFLINE,
        MigrationDirection.ONLINE_TO_OFFLINE,
        MappingSource.MAPPING_FILE
    );

    @Test
    void rewritesSemanticNbtIdentityShapes() throws Exception {
        FileChangeResult result = NbtIdentityRewriter.rewrite(vanillaIdentityFixture(ONLINE), List.of(MAPPING));

        assertEquals(6, result.replacements());

        UuidMapping reverse = new UuidMapping(
            "Alice",
            ONLINE,
            OFFLINE,
            MigrationDirection.OFFLINE_TO_ONLINE,
            MappingSource.MAPPING_FILE
        );
        assertEquals(6, NbtIdentityRewriter.rewrite(result.content(), List.of(reverse)).replacements());
    }

    private static byte[] vanillaIdentityFixture(UUID uuid) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeByte(10);
            output.writeUTF("");
            writeString(output, "Owner", uuid.toString());
            writeString(output, "owner_uuid", uuid.toString());
            writeIntArray(output, "Trusted", UuidBinaryCodec.asIntArray(uuid));
            writeLong(output, "UUIDMost", uuid.getMostSignificantBits());
            writeLong(output, "UUIDLeast", uuid.getLeastSignificantBits());
            writeStringList(output, "Players", List.of(uuid.toString()));
            writeGossips(output, uuid);
            output.writeByte(0);
        }
        return bytes.toByteArray();
    }

    private static void writeString(DataOutputStream output, String name, String value) throws Exception {
        output.writeByte(8);
        output.writeUTF(name);
        output.writeUTF(value);
    }

    private static void writeLong(DataOutputStream output, String name, long value) throws Exception {
        output.writeByte(4);
        output.writeUTF(name);
        output.writeLong(value);
    }

    private static void writeIntArray(DataOutputStream output, String name, byte[] encoded) throws Exception {
        output.writeByte(11);
        output.writeUTF(name);
        output.writeInt(4);
        for (int index = 0; index < encoded.length; index += 4) {
            output.writeInt(((encoded[index] & 0xFF) << 24)
                | ((encoded[index + 1] & 0xFF) << 16)
                | ((encoded[index + 2] & 0xFF) << 8)
                | (encoded[index + 3] & 0xFF));
        }
    }

    private static void writeStringList(DataOutputStream output, String name, List<String> values) throws Exception {
        output.writeByte(9);
        output.writeUTF(name);
        output.writeByte(8);
        output.writeInt(values.size());
        for (String value : values) {
            output.writeUTF(value);
        }
    }

    private static void writeGossips(DataOutputStream output, UUID uuid) throws Exception {
        output.writeByte(9);
        output.writeUTF("Gossips");
        output.writeByte(10);
        output.writeInt(1);
        writeString(output, "Target", uuid.toString());
        output.writeByte(0);
    }
}
