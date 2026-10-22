package com.materialism.shop;

import com.google.gson.Gson;
import com.materialism.Materialism;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ShopCatalog {
    private static final Gson GSON = new Gson();
    private static final CatalogFile FILE = load();
    private static final Map<String, CatalogItem> BY_ID = new HashMap<>();

    static {
        for (CatalogItem item : FILE.items) {
            BY_ID.put(item.id, item);
        }
    }

    private ShopCatalog() {
    }

    public static List<ShopCategory> categories() {
        return FILE.categories;
    }

    public static List<CatalogItem> items() {
        return FILE.items;
    }

    public static CatalogItem find(String itemId) {
        return BY_ID.get(itemId);
    }

    private static CatalogFile load() {
        try (var stream = ShopCatalog.class.getResourceAsStream("/assets/materialism/shop.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing /assets/materialism/shop.json");
            }

            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                CatalogFile file = GSON.fromJson(reader, CatalogFile.class);
                if (file == null) {
                    throw new IllegalStateException("Empty shop catalog");
                }

                if (file.categories == null) {
                    file.categories = new ArrayList<>();
                }
                if (file.items == null) {
                    file.items = new ArrayList<>();
                }
                return file;
            }
        } catch (Exception exception) {
            Materialism.LOGGER.error("Failed to load shop catalog", exception);
            return new CatalogFile();
        }
    }

    private static final class CatalogFile {
        private int version;
        private List<ShopCategory> categories = new ArrayList<>();
        private List<CatalogItem> items = new ArrayList<>();
    }

    public static final class CatalogItem {
        public String id;
        public String category;

        public CatalogItem() {
        }

        public CatalogItem(String id, String category) {
            this.id = id;
            this.category = category;
        }
    }
}
