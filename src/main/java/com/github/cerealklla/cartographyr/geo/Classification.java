package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;

import com.github.cerealklla.cartographyr.CartographyrMod;

import net.minecraft.resources.Identifier;

/**
 * Whether a {@link GeographicEntity} is a naturally occurring formation or a constructed one,
 * keyed by an {@link Identifier} rather than a closed Java enum -- same open, trust-based shape as
 * {@link EntityType} (see that class's Javadoc and decisions.md, 2026-09-24, for the full
 * reasoning). Comparisons must use {@link #equals(Object)}, never {@code ==}.
 */
public record Classification(Identifier id) {

    public static final Codec<Classification> CODEC = Identifier.CODEC.xmap(Classification::new, Classification::id);

    public static final Classification NATURAL = builtin("natural");
    public static final Classification CONSTRUCTED = builtin("constructed");

    private static Classification builtin(String path) {
        return new Classification(Identifier.fromNamespaceAndPath(CartographyrMod.MODID, path));
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
