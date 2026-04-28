package dev.kara.uuidbridge.migration.io;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public final class MojangProfileClient {
    private static final URI LOOKUP_BASE = URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/");

    private final HttpClient client;

    public MojangProfileClient() {
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    }

    public Optional<UUID> lookupOnlineUuid(String name) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(LOOKUP_BASE.resolve(encoded))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Mojang profile lookup failed with HTTP " + response.statusCode());
        }
        String id = JsonCodecs.gson().fromJson(response.body(), com.google.gson.JsonObject.class).get("id").getAsString();
        return Optional.of(parseUndashedUuid(id));
    }

    static UUID parseUndashedUuid(String value) {
        if (value.length() != 32) {
            return UUID.fromString(value);
        }
        return UUID.fromString(value.substring(0, 8) + "-"
            + value.substring(8, 12) + "-"
            + value.substring(12, 16) + "-"
            + value.substring(16, 20) + "-"
            + value.substring(20));
    }
}
