package com.materialism;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class WorthData {
    private static final WorthData INSTANCE = load();
    private static final int MAX_CONTAINER_DEPTH = 3;

    private final Map<String, Double> items;
    private final Map<String, Variant> variants;

    private WorthData(Map<String, Double> items, Map<String, Variant> variants) {
        this.items = Map.copyOf(items);
        this.variants = Map.copyOf(variants);
    }

    public static WorthData get() {
        return INSTANCE;
    }

    public double baseValue(ItemStack stack) {
        return this.baseValue(id(stack));
    }

    public double baseValue(String itemId) {
        return this.items.getOrDefault(itemId, 0.0D);
    }

    public SaleQuote quote(ItemStack stack) {
        if (stack.isEmpty()) {
            return SaleQuote.EMPTY;
        }

        Totals totals = new Totals();
        List<ItemStack> returns = new ArrayList<>();
        this.evaluate(stack, totals, returns, 0);
        return new SaleQuote(totals.units, totals.count, List.copyOf(returns));
    }

    private void evaluate(ItemStack stack, Totals totals, List<ItemStack> returns, int depth) {
        if (stack.isEmpty() || stack.getCount() <= 0) {
            return;
        }

        ContainerKind kind = containerKind(stack);
        if (kind != ContainerKind.NONE) {
            if (stack.has(DataComponents.CONTAINER_LOOT)) {
                returns.add(stack.copy());
                return;
            }

            List<ItemStack> contents = containerContents(stack, kind);
            if (contents.isEmpty()) {
                this.addValue(stack, this.baseValue(stack), totals, returns);
                return;
            }

            if (depth >= MAX_CONTAINER_DEPTH || stack.getCount() > 1) {
                returns.add(stack.copy());
                return;
            }

            for (int copy = 0; copy < stack.getCount(); copy++) {
                for (ItemStack inner : contents) {
                    this.evaluate(inner, totals, returns, depth + 1);
                }

                ItemStack empty = stack.copyWithCount(1);
                empty.remove(DataComponents.CONTAINER);
                empty.remove(DataComponents.BUNDLE_CONTENTS);
                returns.add(empty);
            }
            return;
        }

        Double special = this.specialValue(stack);
        double value = special != null ? special : this.baseValue(stack);
        value *= durabilityFactor(stack);
        this.addValue(stack, value, totals, returns);
    }

    private static double durabilityFactor(ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0 || stack.has(DataComponents.UNBREAKABLE)) {
            return 1.0D;
        }

        int remaining = Math.max(0, maxDamage - stack.getDamageValue());
        return Math.min(1.0D, remaining / (double) maxDamage);
    }

    private void addValue(ItemStack stack, double value, Totals totals, List<ItemStack> returns) {
        if (value <= 0.0D) {
            returns.add(stack.copy());
            return;
        }

        totals.units += Money.units(value, stack.getCount());
        totals.count += stack.getCount();
    }

    private Double specialValue(ItemStack stack) {
        String itemId = id(stack);
        Variant variant = this.variants.get(itemId);
        if (variant == null) {
            return null;
        }

        switch (itemId) {
            case "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow" -> {
                PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
                if (contents == null || contents.potion().isEmpty()) {
                    return null;
                }

                return variant.value(holderKey(contents.potion().get()));
            }
            case "minecraft:enchanted_book" -> {
                ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchantments == null || enchantments.isEmpty()) {
                    return null;
                }

                double value = this.baseValue(stack);
                for (var entry : enchantments.entrySet()) {
                    value += variant.level(holderKey(entry.getKey()), entry.getIntValue());
                }

                return value;
            }
            case "minecraft:goat_horn" -> {
                InstrumentComponent instrument = stack.get(DataComponents.INSTRUMENT);
                return instrument == null ? null : variant.value(holderKey(instrument.instrument()));
            }
            case "minecraft:ominous_bottle" -> {
                OminousBottleAmplifier amplifier = stack.get(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
                return amplifier == null ? null : variant.value(Integer.toString(amplifier.value()));
            }
            case "minecraft:suspicious_stew" -> {
                SuspiciousStewEffects effects = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
                if (effects == null || effects.effects().isEmpty()) {
                    return null;
                }

                double value = this.baseValue(stack);
                for (SuspiciousStewEffects.Entry effect : effects.effects()) {
                    Double bonus = variant.value(holderKey(effect.effect()));
                    if (bonus != null) {
                        value += bonus;
                    }
                }

                return value;
            }
            default -> {
                return null;
            }
        }
    }

    private static ContainerKind containerKind(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            return ContainerKind.SHULKER;
        }

        if (stack.getItem() instanceof BundleItem) {
            return ContainerKind.BUNDLE;
        }

        return ContainerKind.NONE;
    }

    private static List<ItemStack> containerContents(ItemStack stack, ContainerKind kind) {
        if (kind == ContainerKind.SHULKER) {
            ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
            return contents == null ? List.of() : contents.nonEmptyItemCopyStream().toList();
        }

        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null || contents.isEmpty() ? List.of() : contents.itemCopyStream().toList();
    }

    private static String id(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    private static String holderKey(Holder<?> holder) {
        return holder.unwrapKey().map(key -> key.identifier().toString()).orElse("");
    }

    private static WorthData load() {
        try (var stream = WorthData.class.getResourceAsStream("/assets/materialism/worth.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing /assets/materialism/worth.json");
            }

            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                Map<String, Double> items = new HashMap<>();
                JsonObject itemObject = root.getAsJsonObject("items");
                for (Map.Entry<String, JsonElement> entry : itemObject.entrySet()) {
                    items.put(entry.getKey(), entry.getValue().getAsDouble());
                }

                Map<String, Variant> variants = new HashMap<>();
                JsonObject variantObject = root.getAsJsonObject("variants");
                if (variantObject != null) {
                    for (Map.Entry<String, JsonElement> entry : variantObject.entrySet()) {
                        JsonObject object = entry.getValue().getAsJsonObject();
                        Map<String, Double> values = new HashMap<>();
                        JsonObject valueObject = object.getAsJsonObject("values");
                        if (valueObject != null) {
                            for (Map.Entry<String, JsonElement> value : valueObject.entrySet()) {
                                values.put(value.getKey(), value.getValue().getAsDouble());
                            }
                        }

                        Map<String, List<Double>> levels = new HashMap<>();
                        JsonObject levelObject = object.getAsJsonObject("levels");
                        if (levelObject != null) {
                            for (Map.Entry<String, JsonElement> level : levelObject.entrySet()) {
                                JsonArray array = level.getValue().getAsJsonArray();
                                List<Double> valuesByLevel = new ArrayList<>(array.size());
                                for (JsonElement element : array) {
                                    valuesByLevel.add(element.getAsDouble());
                                }
                                levels.put(level.getKey(), List.copyOf(valuesByLevel));
                            }
                        }

                        variants.put(entry.getKey(), new Variant(values, levels));
                    }
                }

                return new WorthData(items, variants);
            }
        } catch (Exception exception) {
            Materialism.LOGGER.error("Failed to load item values", exception);
            return new WorthData(Map.of(), Map.of());
        }
    }

    private enum ContainerKind {
        NONE,
        SHULKER,
        BUNDLE
    }

    private static final class Totals {
        private long units;
        private int count;
    }

    private record Variant(Map<String, Double> values, Map<String, List<Double>> levels) {
        private Double value(String key) {
            return key == null || key.isEmpty() ? null : this.values.get(key);
        }

        private double level(String key, int level) {
            List<Double> values = key == null || key.isEmpty() ? null : this.levels.get(key);
            if (values == null || level < 0 || level >= values.size()) {
                return 0.0D;
            }

            return values.get(level);
        }
    }
}
