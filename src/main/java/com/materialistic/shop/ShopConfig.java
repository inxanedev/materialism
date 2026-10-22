package com.materialistic.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.materialistic.Materialistic;
import com.materialistic.Money;
import com.materialistic.WorthData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class ShopConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("materialistic-shop.json");
    private static ShopConfig instance;

    private double buyPriceMultiplier = 3.0D;
    private Double shulkerBoxPriceOverride;
    private int maxShulkerBoxesPerPurchase = 5;
    private Map<String, Double> itemPriceOverrides = new HashMap<>();

    private ShopConfig() {
    }

    public static synchronized ShopConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public long itemPriceUnits(String itemId) {
        Double override = this.itemPriceOverrides.get(itemId);
        if (override != null) {
            return Math.max(1L, Math.round(override * Money.SCALE));
        }

        long sellUnits = Math.max(1L, Math.round(WorthData.get().baseValue(itemId) * Money.SCALE));
        return Math.max(1L, (long) Math.ceil(sellUnits * this.buyPriceMultiplier));
    }

    public long shulkerBoxPriceUnits() {
        if (this.shulkerBoxPriceOverride != null) {
            return Math.max(1L, Money.units(this.shulkerBoxPriceOverride, 1));
        }

        return this.itemPriceUnits("minecraft:shulker_box");
    }

    public int maxShulkerBoxes() {
        return Math.max(1, this.maxShulkerBoxesPerPurchase);
    }

    private static ShopConfig load() {
        if (!Files.isRegularFile(PATH)) {
            ShopConfig config = new ShopConfig();
            config.save();
            return config;
        }

        try {
            ShopConfig config = GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), ShopConfig.class);
            if (config == null) {
                config = new ShopConfig();
            }
            if (config.itemPriceOverrides == null) {
                config.itemPriceOverrides = new HashMap<>();
            }
            return config;
        } catch (Exception exception) {
            Materialistic.LOGGER.error("Failed to load shop config {}", PATH, exception);
            return new ShopConfig();
        }
    }

    private void save() {
        try {
            Path parent = PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(PATH, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Materialistic.LOGGER.error("Failed to save shop config {}", PATH, exception);
        }
    }
}
