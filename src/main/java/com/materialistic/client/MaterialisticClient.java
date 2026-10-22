package com.materialistic.client;

import com.materialistic.Materialistic;
import com.materialistic.client.shop.ShopClientNetworking;
import com.materialistic.menu.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;

public final class MaterialisticClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Materialistic.MOD_ID, "main")
    );
    private static KeyMapping sellKey;

    @Override
    public void onInitializeClient() {
        MaterialisticConfig.load();
        ModMenus.init();
        MenuScreens.register(ModMenus.SELL, SellScreen::new);
        ShopClientNetworking.init();

        sellKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.materialistic.sell",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (sellKey.consumeClick()) {
                if (client.player != null && client.getConnection() != null) {
                    client.getConnection().sendCommand("sell");
                }
            }
        });
    }
}
