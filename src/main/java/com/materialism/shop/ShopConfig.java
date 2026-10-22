package com.materialism.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.materialism.CurrencyFormat;
import com.materialism.Materialism;
import com.materialism.Money;
import com.materialism.WorthData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class ShopConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("materialism-shop.json");
    private static ShopConfig instance;

    private double buyPriceMultiplier = 3.0D;
    private Double shulkerBoxPriceOverride;
    private int maxShulkerBoxesPerPurchase = 5;
    private Map<String, Double> itemPriceOverrides = new HashMap<>();
    private String currencySymbol = "c";
    private String currencyPosition = "suffix";

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

    public CurrencyFormat currencyFormat() {
        return CurrencyFormat.parse(this.currencySymbol, this.currencyPosition);
    }

    private static ShopConfig load() {
        if (!Files.isRegularFile(PATH)) {
            ShopConfig config = new ShopConfig();
            config.save();
            return config;
        }

        try {
            JsonObject root = JsonParser.parseString(Files.readString(PATH, StandardCharsets.UTF_8)).getAsJsonObject();
            ShopConfig config = GSON.fromJson(root, ShopConfig.class);
            if (config == null) {
                config = new ShopConfig();
            }

            boolean changed = !root.has("currencySymbol") || !root.has("currencyPosition");
            if (config.itemPriceOverrides == null) {
                config.itemPriceOverrides = new HashMap<>();
                changed = true;
            }
            if (config.currencySymbol == null) {
                config.currencySymbol = "c";
                changed = true;
            }
            if (config.currencyPosition == null) {
                config.currencyPosition = "suffix";
                changed = true;
            } else {
                String position = config.currencyPosition.trim();
                if (!position.equalsIgnoreCase("prefix") && !position.equalsIgnoreCase("suffix")) {
                    Materialism.LOGGER.warn(
                            "Invalid currencyPosition '{}' in {}; using suffix",
                            config.currencyPosition,
                            PATH
                    );
                    position = "suffix";
                }
                if (!position.equals(config.currencyPosition)) {
                    config.currencyPosition = position;
                    changed = true;
                }
            }

            if (changed) {
                config.save();
            }
            return config;
        } catch (Exception exception) {
            Materialism.LOGGER.error("Failed to load shop config {}", PATH, exception);
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
            Materialism.LOGGER.error("Failed to save shop config {}", PATH, exception);
        }
    }
}
