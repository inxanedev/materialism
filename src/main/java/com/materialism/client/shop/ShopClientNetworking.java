package com.materialism.client.shop;

import com.google.gson.Gson;
import com.materialism.CurrencyFormat;
import com.materialism.Money;
import com.materialism.network.CurrencyFormatPayload;
import com.materialism.network.OpenShopPayload;
import com.materialism.network.PurchaseResultPayload;
import com.materialism.shop.ShopSnapshot;
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

        ClientPlayNetworking.registerGlobalReceiver(CurrencyFormatPayload.TYPE, (payload, context) ->
                context.client().execute(() -> Money.setCurrencyFormat(
                        new CurrencyFormat(payload.symbol(), payload.prefix())
                ))
        );
    }
}
