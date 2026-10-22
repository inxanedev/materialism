package com.materialistic.network;

import com.materialistic.shop.ShopService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ShopNetworking {
    private ShopNetworking() {
    }

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(
                OpenShopPayload.TYPE,
                OpenShopPayload.CODEC,
                1_048_576
        );
        PayloadTypeRegistry.clientboundPlay().register(
                PurchaseResultPayload.TYPE,
                PurchaseResultPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                BuyItemPayload.TYPE,
                BuyItemPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(BuyItemPayload.TYPE, (payload, context) ->
                context.server().execute(() -> ShopService.buy(
                        context.player(),
                        payload.itemId(),
                        payload.quantity(),
                        payload.boxed()
                ))
        );
    }
}
