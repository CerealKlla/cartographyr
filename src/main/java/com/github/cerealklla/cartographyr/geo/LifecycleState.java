package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;

/**
 * Where a {@link GeographicEntity} sits between being planned and being physically realized,
 * abandoned, destroyed, or retired. Retired/destroyed entities remain historically addressable —
 * this state is descriptive, not a deletion flag.
 */
public enum LifecycleState {
    PLANNED,
    REALIZED,
    ABANDONED,
    DESTROYED,
    RETIRED;

    public static final Codec<LifecycleState> CODEC = Codec.STRING.xmap(LifecycleState::valueOf, Enum::name);
}
