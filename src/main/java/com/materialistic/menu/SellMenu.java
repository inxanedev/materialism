package com.materialistic.menu;

import com.materialistic.Money;
import com.materialistic.SaleQuote;
import com.materialistic.WorthData;
import com.materialistic.economy.EconomyManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class SellMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 54;
    private static final int CONTAINER_END = CONTAINER_SIZE;
    private static final int INVENTORY_END = CONTAINER_END + 36;

    private final SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);
    private final Player owner;
    private boolean processed;
    private boolean cancelled;

    public SellMenu(int containerId, Inventory inventory) {
        super(ModMenus.SELL, containerId);
        this.owner = inventory.player;
        checkContainerSize(this.container, CONTAINER_SIZE);

        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 9; column++) {
                int index = column + row * 9;
                this.addSlot(new SellSlot(this.container, index, 16 + column * 18, 32 + row * 18));
            }
        }

        this.addStandardInventorySlots(inventory, 16, 156);
    }

    public Summary summary() {
        long units = 0L;
        int count = 0;
        for (int index = 0; index < CONTAINER_SIZE; index++) {
            SaleQuote quote = WorthData.get().quote(this.container.getItem(index));
            units += quote.units();
            count += quote.itemCount();
        }

        return new Summary(units, count);
    }

    public void sell(ServerPlayer player) {
        if (this.processed || !player.getUUID().equals(this.owner.getUUID())) {
            return;
        }

        this.processed = true;
        this.sellContents(player);
        player.closeContainer();
    }

    public void cancel(ServerPlayer player) {
        if (this.processed || !player.getUUID().equals(this.owner.getUUID())) {
            return;
        }

        this.processed = true;
        this.cancelled = true;
        this.returnAll(player);
        player.closeContainer();
    }

    private void sellContents(ServerPlayer player) {
        List<ItemStack> returns = new ArrayList<>();
        long units = 0L;
        int count = 0;

        for (int index = 0; index < CONTAINER_SIZE; index++) {
            ItemStack stack = this.container.getItem(index);
            if (stack.isEmpty()) {
                continue;
            }

            SaleQuote quote = WorthData.get().quote(stack);
            units += quote.units();
            count += quote.itemCount();
            returns.addAll(quote.returns());
        }

        if (units <= 0L) {
            this.returnAll(player);
            return;
        }

        EconomyManager.get(player.level().getServer()).add(player.getUUID(), units);

        for (int index = 0; index < CONTAINER_SIZE; index++) {
            this.container.setItem(index, ItemStack.EMPTY);
        }

        this.giveReturns(player, returns);
        player.sendSystemMessage(Component.translatable(
                "materialistic.command.sold",
                count,
                Money.currency(units)
        ));
    }

    private void returnAll(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int index = 0; index < CONTAINER_SIZE; index++) {
            ItemStack stack = this.container.removeItemNoUpdate(index);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }

        this.giveReturns(player, stacks);
    }

    private void giveReturns(ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack copy = stack.copy();
            if (player.hasDisconnected()) {
                player.drop(copy, false);
            } else if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();

        if (slotIndex < CONTAINER_END) {
            if (!this.moveItemStackTo(stack, CONTAINER_END, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, CONTAINER_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getUUID().equals(this.owner.getUUID());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.processed || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        this.processed = true;
        if (this.cancelled) {
            this.returnAll(serverPlayer);
        } else {
            this.sellContents(serverPlayer);
        }
    }

    public SimpleContainer getContainer() {
        return this.container;
    }

    public record Summary(long units, int itemCount) {
    }
}
