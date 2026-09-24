package com.github.cerealklla.cartographyr.network;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.cartographyr.CartographyrMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-to-client: the name of the geographic entity the receiving player currently occupies,
 * for the persistent top-right location overlay (design doc Section 5.8/demo notification,
 * replaced chat-message-based notification 2026-09-24 -- see decisions.md).
 */
public record LocationPayload(String name) implements CustomPacketPayload {

    public static final Type<LocationPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CartographyrMod.MODID, "location"));

    public static final StreamCodec<ByteBuf, LocationPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, LocationPayload::name, LocationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
