package com.materialism.menu;

import com.materialism.Materialism;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final MenuType<SellMenu> SELL = register("sell", new MenuType<>(SellMenu::new, FeatureFlagSet.of()));

    private ModMenus() {
    }

    public static void init() {
    }

    private static <T extends SellMenu> MenuType<T> register(String name, MenuType<T> type) {
        return Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(Materialism.MOD_ID, name),
                type
        );
    }
}
