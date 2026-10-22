package com.materialism.shop;

public final class ShopEntry {
    public String id;
    public String category;
    public long price;

    public ShopEntry() {
    }

    public ShopEntry(String id, String category, long price) {
        this.id = id;
        this.category = category;
        this.price = price;
    }
}
