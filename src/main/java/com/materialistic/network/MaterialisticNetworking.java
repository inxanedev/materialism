package com.materialistic.network;

import com.materialistic.menu.SellMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class MaterialisticNetworking {
    private MaterialisticNetworking() {
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(SellPayload.TYPE, SellPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CancelPayload.TYPE, CancelPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SellPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            if (context.player().containerMenu instanceof SellMenu menu) {
                menu.sell(context.player());
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(CancelPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            if (context.player().containerMenu instanceof SellMenu menu) {
                menu.cancel(context.player());
            }
        }));
    }
}
