package com.materialistic.network;

import com.materialistic.Materialistic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SellPayload() implements CustomPacketPayload {
    public static final SellPayload INSTANCE = new SellPayload();
    public static final Type<SellPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Materialistic.MOD_ID, "sell")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SellPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
