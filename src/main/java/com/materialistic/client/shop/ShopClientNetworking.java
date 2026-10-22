package com.materialistic.client.shop;

import com.google.gson.Gson;
import com.materialistic.network.OpenShopPayload;
import com.materialistic.network.PurchaseResultPayload;
import com.materialistic.shop.ShopSnapshot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ShopClientNetworking {
    private static final Gson GSON = new Gson();

    private ShopClientNetworking() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(OpenShopPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    ShopSnapshot snapshot = GSON.fromJson(payload.snapshotJson(), ShopSnapshot.class);
                    if (snapshot != null) {
                        context.client().gui.setScreen(new ShopScreen(snapshot));
                    }
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(PurchaseResultPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().gui.screen() instanceof ShopScreen screen) {
                        screen.handlePurchaseResult(payload);
                    }
                })
        );
    }
}
