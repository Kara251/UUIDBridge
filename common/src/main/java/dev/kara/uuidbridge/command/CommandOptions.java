package dev.kara.uuidbridge.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

record CommandOptions(Optional<String> mapping, boolean allowNetwork, boolean confirm) {
    static CommandOptions parse(String raw) {
        List<String> tokens = split(raw);
        Optional<String> mapping = Optional.empty();
        boolean allowNetwork = false;
        boolean confirm = false;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if ("--mapping".equals(token) && i + 1 < tokens.size()) {
                mapping = Optional.of(tokens.get(++i));
            } else if ("--allow-network".equals(token)) {
                allowNetwork = true;
            } else if ("--confirm".equals(token)) {
                confirm = true;
            }
        }
        return new CommandOptions(mapping, allowNetwork, confirm);
    }

    private static List<String> split(String raw) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                continue;
            }
            if (Character.isWhitespace(c) && !quoted) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
