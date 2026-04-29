package dev.kara.uuidbridge.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandOptionsTest {
    @Test
    void reportsRemovedAllowNetworkOptionAsUnknown() {
        CommandOptions options = CommandOptions.parse("--mapping mapping.csv --allow-network");

        assertEquals("mapping.csv", options.mapping().orElseThrow());
        assertEquals(1, options.unknownOptions().size());
        assertEquals("--allow-network", options.unknownOptions().getFirst());
    }

    @Test
    void keepsKnownOptionsOutOfUnknownList() {
        CommandOptions options = CommandOptions.parse("--targets uuidbridge/targets.json --confirm");

        assertTrue(options.confirm());
        assertTrue(options.unknownOptions().isEmpty());
    }
}
