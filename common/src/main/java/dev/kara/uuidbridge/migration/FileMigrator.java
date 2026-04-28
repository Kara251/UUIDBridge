package dev.kara.uuidbridge.migration;

import dev.kara.uuidbridge.migration.io.SafeFileWriter;
import dev.kara.uuidbridge.migration.rewrite.RegionFileRewriter;
import dev.kara.uuidbridge.migration.rewrite.UuidReplacementEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class FileMigrator {
    private FileMigrator() {
    }

    public static FileChangeResult preview(Path file, List<UuidMapping> mappings) throws IOException {
        return rewriteBytes(file, mappings);
    }

    public static long rewrite(Path file, List<UuidMapping> mappings) throws IOException {
        FileChangeResult result = rewriteBytes(file, mappings);
        if (result.changed()) {
            SafeFileWriter.writeAtomic(file, result.content());
        }
        return result.replacements();
    }

    private static FileChangeResult rewriteBytes(Path file, List<UuidMapping> mappings) throws IOException {
        byte[] content = Files.readAllBytes(file);
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".mca")) {
            return RegionFileRewriter.rewrite(content, mappings);
        }
        if (name.endsWith(".dat") || name.endsWith(".dat_old")) {
            try {
                return UuidReplacementEngine.rewriteGzip(content, mappings);
            } catch (IOException ignored) {
                return UuidReplacementEngine.rewritePlain(content, mappings);
            }
        }
        return UuidReplacementEngine.rewritePlain(content, mappings);
    }
}
