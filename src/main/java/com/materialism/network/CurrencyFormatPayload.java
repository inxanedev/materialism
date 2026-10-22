package com.materialism.network;

import com.materialism.Materialism;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CurrencyFormatPayload(String symbol, boolean prefix) implements CustomPacketPayload {
    public static final Type<CurrencyFormatPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Materialism.MOD_ID, "currency_format")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CurrencyFormatPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.symbol(), 256);
                buffer.writeBoolean(payload.prefix());
            },
            buffer -> new CurrencyFormatPayload(buffer.readUtf(256), buffer.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
