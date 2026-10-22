package com.materialism.menu;

import com.materialism.WorthData;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class SellSlot extends Slot {
    public SellSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && WorthData.get().quote(stack).units() > 0L;
    }
}
