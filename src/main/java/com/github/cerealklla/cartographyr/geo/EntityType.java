package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;

/**
 * The kind of geographic entity. The design document treats this list as illustrative examples
 * rather than exhaustive; this is a closed enum for the first milestone only — if other mods
 * eventually need to define their own types, this will need to become an open/registry-backed
 * type instead. Not needed yet.
 *
 * <p>Deliberately no VILLAGE/TOWN/CITY tier distinction: that's a population/prestige
 * classification Cartography doesn't own (Section 14 — government/economy are out of scope).
 * SETTLEMENT covers all inhabited constructed places; other mods layer tier on top via
 * characteristics/amenities/extension data. No RUIN either — a ruined settlement is just a
 * SETTLEMENT with {@link LifecycleState#ABANDONED} or {@link LifecycleState#DESTROYED}, not a
 * different type.
 */
public enum EntityType {
    REGION,
    MOUNTAIN,
    RIVER,
    SETTLEMENT,
    MINE,
    ROAD;

    public static final Codec<EntityType> CODEC = Codec.STRING.xmap(EntityType::valueOf, Enum::name);
}
