package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;

import com.github.cerealklla.cartographyr.CartographyrMod;

import net.minecraft.resources.Identifier;

/**
 * The kind of geographic entity, keyed by an {@link Identifier} rather than a closed Java enum.
 * This is deliberately open: any mod calling {@code Cartography.createEntity} can tag an entity
 * with its own type (e.g. a future Factions mod's {@code new EntityType(Identifier.fromNamespaceAndPath("factionsmod", "territory"))})
 * without touching Cartographyr's source — the same open-extension shape as vanilla registries and
 * NeoForge tags. Writes are trust-based: any mod may create entities under any namespace, including
 * one it doesn't own (decided 2026-09-24, see decisions.md — no claiming/registration step exists or
 * is planned).
 *
 * <p>Because two separately-constructed instances of the same id are different objects, comparisons
 * must use {@link #equals(Object)}, never {@code ==} — this replaced a closed enum (see decisions.md,
 * 2026-09-24) where {@code ==} happened to work.
 *
 * <p>The constants below are Cartographyr's own built-in types, namespaced under {@link
 * CartographyrMod#MODID}. They exist purely for source convenience (so {@code EntityType.DESERT}
 * etc. keep compiling) — nothing about the type is special-cased for them.
 *
 * <p>Deliberately no VILLAGE/TOWN/CITY tier distinction: that's a population/prestige
 * classification Cartography doesn't own (Section 14 — government/economy are out of scope).
 * SETTLEMENT covers all inhabited constructed places; other mods layer tier on top via
 * characteristics/amenities/extension data. No RUIN either — a ruined settlement is just a
 * SETTLEMENT with {@link LifecycleState#ABANDONED} or {@link LifecycleState#DESTROYED}, not a
 * different type.
 *
 * <p>DESERT/FOREST/PLAINS/SWAMP/TAIGA/JUNGLE/SAVANNA/BADLANDS/OCEAN/BEACH/NETHER_WASTELAND
 * (Natural Geography, Section 5.8) are a different case from the VILLAGE/TOWN/CITY removal above:
 * they're intrinsic, physically-observable terrain properties (biome-driven, stable over time),
 * not a socially-constructed tier — structurally the same kind of value as MOUNTAIN/RIVER, which
 * were never in question. Deliberately still not exhaustive: mushroom fields, cherry groves, ice
 * spikes, and cave/End biomes have no profile yet — see {@code NaturalRegionProfile} and design
 * doc Section 5.8's "incremental, not exhaustive" guidance. NETHER_WASTELAND (added 2026-09-25)
 * deliberately covers every Nether biome under one type/name-pool rather than five separate
 * themed ones — see decisions.md.
 */
public record EntityType(Identifier id) {

    public static final Codec<EntityType> CODEC = Identifier.CODEC.xmap(EntityType::new, EntityType::id);

    public static final EntityType REGION = builtin("region");
    public static final EntityType MOUNTAIN = builtin("mountain");
    public static final EntityType RIVER = builtin("river");
    public static final EntityType DESERT = builtin("desert");
    public static final EntityType FOREST = builtin("forest");
    public static final EntityType PLAINS = builtin("plains");
    public static final EntityType SWAMP = builtin("swamp");
    public static final EntityType TAIGA = builtin("taiga");
    public static final EntityType JUNGLE = builtin("jungle");
    public static final EntityType SAVANNA = builtin("savanna");
    public static final EntityType BADLANDS = builtin("badlands");
    public static final EntityType OCEAN = builtin("ocean");
    public static final EntityType BEACH = builtin("beach");
    public static final EntityType NETHER_WASTELAND = builtin("nether_wasteland");
    public static final EntityType SETTLEMENT = builtin("settlement");
    public static final EntityType MINE = builtin("mine");
    public static final EntityType ROAD = builtin("road");

    private static EntityType builtin(String path) {
        return new EntityType(Identifier.fromNamespaceAndPath(CartographyrMod.MODID, path));
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
