package com.materialistic;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public record SaleQuote(long units, int itemCount, List<ItemStack> returns) {
    public static final SaleQuote EMPTY = new SaleQuote(0L, 0, List.of());

    public boolean isEmpty() {
        return this.units <= 0L;
    }
}
