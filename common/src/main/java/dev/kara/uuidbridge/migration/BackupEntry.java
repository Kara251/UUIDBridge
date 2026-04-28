package dev.kara.uuidbridge.migration;

public record BackupEntry(
    String originalPath,
    String backupPath,
    long size,
    String sha256
) {
}
