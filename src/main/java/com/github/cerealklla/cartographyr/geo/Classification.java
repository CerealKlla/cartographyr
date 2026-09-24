package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;

/** Whether a {@link GeographicEntity} is a naturally occurring formation or a constructed one. */
public enum Classification {
    NATURAL,
    CONSTRUCTED;

    public static final Codec<Classification> CODEC = Codec.STRING.xmap(Classification::valueOf, Enum::name);
}
