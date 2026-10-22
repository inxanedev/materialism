package com.materialism.shop;

import com.google.gson.Gson;
import com.materialism.Materialism;
import com.materialism.Money;
import com.materialism.WorthData;
import com.materialism.economy.EconomyManager;
import com.materialism.economy.EconomyStore;
import com.materialism.network.OpenShopPayload;
import com.materialism.network.PurchaseResultPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public final class ShopService {
    private static final Gson GSON = new Gson();
    private static final int SHULKER_ROWS = 27;

    private ShopService() {
    }

    public static void open(ServerPlayer player) {
        ShopSnapshot snapshot = snapshot(player);
        ServerPlayNetworking.send(player, new OpenShopPayload(GSON.toJson(snapshot)));
    }

    public static void buy(ServerPlayer player, Identifier itemId, int quantity, boolean boxed) {
        ShopConfig config = ShopConfig.get();
        ShopCatalog.CatalogItem catalogItem = ShopCatalog.find(itemId.toString());
        Optional<Item> resolved = BuiltInRegistries.ITEM.getOptional(itemId);

        if (catalogItem == null || resolved.isEmpty() || resolved.get() == Items.AIR) {
            fail(player, itemId, quantity, boxed, "materialism.shop.invalid");
            return;
        }

        Item item = resolved.get();
        ItemStack prototype = new ItemStack(item);
        int stackSize = Math.max(1, prototype.getMaxStackSize());
        int capacity = SHULKER_ROWS * stackSize;
        boolean packable = !(item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock);
        int maxQuantity = packable ? config.maxShulkerBoxes() * capacity : SHULKER_ROWS;

        if (quantity <= 0 || quantity > maxQuantity) {
            fail(player, itemId, quantity, boxed, "materialism.shop.invalid_quantity");
            return;
        }

        if (boxed && !packable) {
            fail(player, itemId, quantity, boxed, "materialism.shop.not_packable");
            return;
        }

        int boxes = boxed ? (int) Math.ceil(quantity / (double) capacity) : 0;
        if (boxes > config.maxShulkerBoxes()) {
            fail(player, itemId, quantity, boxed, "materialism.shop.invalid_quantity");
            return;
        }

        long unitPrice = config.itemPriceUnits(itemId.toString());
        long total;
        try {
            total = Math.addExact(Math.multiplyExact(unitPrice, quantity), Math.multiplyExact(config.shulkerBoxPriceUnits(), boxes));
        } catch (ArithmeticException exception) {
            fail(player, itemId, quantity, boxed, "materialism.shop.invalid_quantity");
            return;
        }

        EconomyStore economy = EconomyManager.get(player.level().getServer());
        if (!economy.spend(player.getUUID(), total)) {
            fail(player, itemId, quantity, boxed, "materialism.shop.insufficient");
            return;
        }

        int dropped = boxed
                ? deliverBoxes(player, prototype, quantity, stackSize)
                : deliverLoose(player, prototype, quantity, stackSize);

        long balance = economy.get(player.getUUID());
        player.sendSystemMessage(Component.translatable(
                "materialism.shop.bought",
                quantity,
                prototype.getHoverName(),
                Money.currency(total)
        ));

        if (dropped > 0) {
            player.sendSystemMessage(Component.translatable(
                    boxed ? "materialism.shop.boxes_dropped" : "materialism.shop.items_dropped",
                    dropped
            ));
        }

        ServerPlayNetworking.send(player, new PurchaseResultPayload(
                true,
                balance,
                itemId.toString(),
                quantity,
                boxes,
                dropped,
                total,
                boxed
        ));
    }

    private static ShopSnapshot snapshot(ServerPlayer player) {
        ShopConfig config = ShopConfig.get();
        List<ShopEntry> entries = new ArrayList<>();
        for (ShopCatalog.CatalogItem catalogItem : ShopCatalog.items()) {
            Identifier id = Identifier.tryParse(catalogItem.id);
            if (id == null || BuiltInRegistries.ITEM.getOptional(id).isEmpty()) {
                continue;
            }

            if (WorthData.get().baseValue(catalogItem.id) <= 0.0D) {
                continue;
            }

            entries.add(new ShopEntry(catalogItem.id, catalogItem.category, config.itemPriceUnits(catalogItem.id)));
        }

        return new ShopSnapshot(
                ShopCatalog.categories(),
                entries,
                EconomyManager.get(player.level().getServer()).get(player.getUUID()),
                config.shulkerBoxPriceUnits(),
                config.maxShulkerBoxes()
        );
    }

    private static void fail(ServerPlayer player, Identifier itemId, int quantity, boolean boxed, String key) {
        player.sendSystemMessage(Component.translatable(key));
        ServerPlayNetworking.send(player, new PurchaseResultPayload(
                false,
                EconomyManager.get(player.level().getServer()).get(player.getUUID()),
                itemId.toString(),
                quantity,
                0,
                0,
                0L,
                boxed
        ));
    }

    private static int deliverLoose(ServerPlayer player, ItemStack prototype, int quantity, int stackSize) {
        int remaining = quantity;
        int dropped = 0;

        while (remaining > 0) {
            int count = Math.min(stackSize, remaining);
            remaining -= count;
            ItemStack stack = prototype.copyWithCount(count);
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                dropped += stack.getCount();
                player.drop(stack, false);
            }
        }

        return dropped;
    }

    private static int deliverBoxes(ServerPlayer player, ItemStack prototype, int quantity, int stackSize) {
        int capacity = SHULKER_ROWS * stackSize;
        int remaining = quantity;
        int dropped = 0;

        while (remaining > 0) {
            List<ItemStack> contents = new ArrayList<>(SHULKER_ROWS);
            int toPlace = Math.min(capacity, remaining);
            remaining -= toPlace;

            while (toPlace > 0) {
                int count = Math.min(stackSize, toPlace);
                contents.add(prototype.copyWithCount(count));
                toPlace -= count;
            }

            while (contents.size() < SHULKER_ROWS) {
                contents.add(ItemStack.EMPTY);
            }

            ItemStack box = new ItemStack(Items.SHULKER_BOX);
            box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
            player.getInventory().add(box);
            if (!box.isEmpty()) {
                dropped++;
                player.drop(box, false);
            }
        }

        return dropped;
    }
}
