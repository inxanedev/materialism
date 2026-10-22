package com.materialism.network;

import com.materialism.Materialism;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenShopPayload(String snapshotJson) implements CustomPacketPayload {
    public static final Type<OpenShopPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Materialism.MOD_ID, "open_shop")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeUtf(payload.snapshotJson(), 1_048_576),
            buffer -> new OpenShopPayload(buffer.readUtf(1_048_576))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
