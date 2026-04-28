package dev.kara.uuidbridge.migration;

public record IdentityReference(
    String path,
    String adapter,
    String representation,
    String keyHint
) {
}
