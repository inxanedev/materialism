package com.materialism.network;

import com.materialism.Materialism;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CloseShopPayload() implements CustomPacketPayload {
    public static final CloseShopPayload INSTANCE = new CloseShopPayload();
    public static final Type<CloseShopPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Materialism.MOD_ID, "close_shop")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseShopPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
