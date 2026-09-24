package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;

/**
 * An evolving economic/functional specialty of a {@link GeographicEntity} (design document
 * Section 5.4). Cartography records characteristics but never decides why they changed — that's
 * Economy/settlement logic's call, made elsewhere and then reported here via addCharacteristic.
 *
 * <p>Closed enum for now, same caveat as {@link EntityType}: the design document phrases this
 * list as examples, implying eventual extensibility. Not needed until another mod actually wants
 * to contribute a specialty this enum doesn't have.
 */
public enum Characteristic {
    LUMBER,
    MINING,
    FARMING;

    public static final Codec<Characteristic> CODEC = Codec.STRING.xmap(Characteristic::valueOf, Enum::name);
}
