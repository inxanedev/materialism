package com.materialism.network;

import com.materialism.Materialism;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BuyItemPayload(Identifier itemId, int quantity, boolean boxed) implements CustomPacketPayload {
    public static final Type<BuyItemPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Materialism.MOD_ID, "buy_item")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BuyItemPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeIdentifier(payload.itemId());
                buffer.writeVarInt(payload.quantity());
                buffer.writeBoolean(payload.boxed());
            },
            buffer -> new BuyItemPayload(
                    buffer.readIdentifier(),
                    buffer.readVarInt(),
                    buffer.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
