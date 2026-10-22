package com.materialistic.network;

import com.materialistic.Materialistic;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CancelPayload() implements CustomPacketPayload {
    public static final CancelPayload INSTANCE = new CancelPayload();
    public static final Type<CancelPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Materialistic.MOD_ID, "cancel")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CancelPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
