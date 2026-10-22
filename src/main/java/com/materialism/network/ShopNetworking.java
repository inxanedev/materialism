package com.materialism.network;

import com.materialism.CurrencyFormat;
import com.materialism.Money;
import com.materialism.shop.ShopConfig;
import com.materialism.shop.ShopService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
        PayloadTypeRegistry.clientboundPlay().register(
                CurrencyFormatPayload.TYPE,
                CurrencyFormatPayload.CODEC
        );
        PayloadTypeRegistry.serverboundPlay().register(
                BuyItemPayload.TYPE,
                BuyItemPayload.CODEC
        );

        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                Money.setCurrencyFormat(ShopConfig.get().currencyFormat())
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, CurrencyFormatPayload.TYPE)) {
                CurrencyFormat format = Money.currencyFormat();
                ServerPlayNetworking.send(
                        handler.player,
                        new CurrencyFormatPayload(format.symbol(), format.prefix())
                );
            }
        });

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
