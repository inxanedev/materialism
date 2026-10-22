package com.materialism.network;

import com.materialism.Materialism;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PurchaseResultPayload(
        boolean success,
        long balance,
        String itemId,
        int quantity,
        int boxes,
        int dropped,
        long total,
        boolean boxed
) implements CustomPacketPayload {
    public static final Type<PurchaseResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Materialism.MOD_ID, "purchase_result")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseResultPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.success());
                buffer.writeVarLong(payload.balance());
                buffer.writeUtf(payload.itemId(), 32767);
                buffer.writeVarInt(payload.quantity());
                buffer.writeVarInt(payload.boxes());
                buffer.writeVarInt(payload.dropped());
                buffer.writeVarLong(payload.total());
                buffer.writeBoolean(payload.boxed());
            },
            buffer -> new PurchaseResultPayload(
                    buffer.readBoolean(),
                    buffer.readVarLong(),
                    buffer.readUtf(32767),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarLong(),
                    buffer.readBoolean()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
