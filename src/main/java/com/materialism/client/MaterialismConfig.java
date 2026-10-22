package com.materialism.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.materialism.Materialism;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class MaterialismConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("materialism-client.json");
    private static Data data = new Data();

    private MaterialismConfig() {
    }

    public static void load() {
        if (!Files.isRegularFile(PATH)) {
            return;
        }

        try {
            data = GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), Data.class);
            if (data == null) {
                data = new Data();
            }
        } catch (Exception exception) {
            Materialism.LOGGER.warn("Failed to load client config", exception);
            data = new Data();
        }
    }

    public static boolean confirmBeforeSelling() {
        return data.confirmBeforeSelling;
    }

    public static void setConfirmBeforeSelling(boolean value) {
        data.confirmBeforeSelling = value;
        save();
    }

    private static void save() {
        try {
            Path parent = PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(PATH, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Materialism.LOGGER.error("Failed to save client config", exception);
        }
    }

    private static final class Data {
        private boolean confirmBeforeSelling;
    }
}
