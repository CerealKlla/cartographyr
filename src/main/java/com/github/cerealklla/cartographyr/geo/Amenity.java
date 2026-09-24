package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;

/**
 * A meaningful physical/infrastructural feature of a {@link GeographicEntity} (design document
 * Section 5.5), e.g. a settlement having a market or a sawmill. World Builder can use these as
 * generation inputs; Cartography just records their geographic existence.
 *
 * <p>Closed enum for now, same caveat as {@link Characteristic} and {@link EntityType}: the
 * design document phrases this list as examples, implying eventual extensibility.
 */
public enum Amenity {
    MARKET,
    MINE,
    INN,
    HARBOR;

    public static final Codec<Amenity> CODEC = Codec.STRING.xmap(Amenity::valueOf, Enum::name);
}
