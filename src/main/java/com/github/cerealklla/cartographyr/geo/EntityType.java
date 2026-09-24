package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;

/**
 * The kind of geographic entity. The design document treats this list as illustrative examples
 * rather than exhaustive; this is a closed enum for the first milestone only — if other mods
 * eventually need to define their own types, this will need to become an open/registry-backed
 * type instead. Not needed yet.
 */
public enum EntityType {
    REGION,
    MOUNTAIN,
    RIVER,
    VILLAGE,
    TOWN,
    CITY,
    MINE,
    ROAD,
    RUIN;

    public static final Codec<EntityType> CODEC = Codec.STRING.xmap(EntityType::valueOf, Enum::name);
}
