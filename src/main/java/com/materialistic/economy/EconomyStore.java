package com.materialistic.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.materialistic.Materialistic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class EconomyStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Map<String, Long> balances = new HashMap<>();

    private EconomyStore(Path file) {
        this.file = file;
        this.load();
    }

    public static EconomyStore open(MinecraftServer server) {
        Path directory = server.getWorldPath(LevelResource.ROOT).resolve("materialistic");
        return new EconomyStore(directory.resolve("economy.json"));
    }

    public synchronized long get(UUID player) {
        return this.balances.getOrDefault(player.toString(), 0L);
    }

    public synchronized void add(UUID player, long units) {
        if (units <= 0L) {
            return;
        }

        this.balances.merge(player.toString(), units, Long::sum);
        this.save();
    }

    public synchronized boolean spend(UUID player, long units) {
        if (units <= 0L) {
            return false;
        }

        String key = player.toString();
        long balance = this.balances.getOrDefault(key, 0L);
        if (balance < units) {
            return false;
        }

        this.balances.put(key, balance - units);
        this.save();
        return true;
    }

    private void load() {
        if (!Files.isRegularFile(this.file)) {
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(Files.readString(this.file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject stored = root.getAsJsonObject("balances");
            if (stored == null) {
                return;
            }

            for (Map.Entry<String, JsonElement> entry : stored.entrySet()) {
                try {
                    UUID.fromString(entry.getKey());
                    this.balances.put(entry.getKey(), Math.max(0L, entry.getValue().getAsLong()));
                } catch (IllegalArgumentException exception) {
                    Materialistic.LOGGER.warn("Ignoring invalid UUID in economy file: {}", entry.getKey());
                }
            }
        } catch (Exception exception) {
            Materialistic.LOGGER.error("Failed to load economy file {}", this.file, exception);
        }
    }

    public synchronized void save() {
        try {
            Path parent = this.file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            JsonObject stored = new JsonObject();
            this.balances.forEach(stored::addProperty);
            root.add("balances", stored);

            Path temporary = this.file.resolveSibling(this.file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            Materialistic.LOGGER.error("Failed to save economy file {}", this.file, exception);
        }
    }
}
