package com.materialism.shop;

import java.util.ArrayList;
import java.util.List;

public final class ShopSnapshot {
    public List<ShopCategory> categories = new ArrayList<>();
    public List<ShopEntry> entries = new ArrayList<>();
    public long balance;
    public long shulkerBoxPrice;
    public int maxShulkerBoxes;

    public ShopSnapshot() {
    }

    public ShopSnapshot(
            List<ShopCategory> categories,
            List<ShopEntry> entries,
            long balance,
            long shulkerBoxPrice,
            int maxShulkerBoxes
    ) {
        this.categories = categories;
        this.entries = entries;
        this.balance = balance;
        this.shulkerBoxPrice = shulkerBoxPrice;
        this.maxShulkerBoxes = maxShulkerBoxes;
    }
}
