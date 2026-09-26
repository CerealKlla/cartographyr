package com.github.cerealklla.cartographyr.natural;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.cerealklla.cartographyr.geo.EntityType;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Pairs a set of vanilla biomes with an {@link EntityType} and the family-specific word pools
 * {@link NaturalRegionDiscovery} composes into a name (see {@code naming.NameComposer}) — a base
 * {@code terrainNouns} pool and a {@code thematicPrefixes} pool of biome-flavored adjectives,
 * combined with cross-biome size/climate/generic pools ({@code naming.RegionWordPools}) at
 * generation time rather than picking from a hand-authored list of full phrases (replaced
 * entirely 2026-09-25, see decisions.md — considerably more combinations from the same amount of
 * authored content). Not part of Cartographyr's persisted data or public API, just discovery-time
 * configuration — but made {@code public} (widened 2026-09-24) so {@code
 * .settlement.SettlementDiscovery} can reuse the same biome-matching logic for contextual
 * settlement naming, rather than duplicating a second copy of the biome-family mapping.
 *
 * <p>Covers the common overworld surface land biome families plus oceans/beaches/the Nether,
 * matching the design document's "discovered incrementally... not exhaustively" instruction
 * (Section 5.8) — deliberately still not everything: only End biomes (and the void) have no
 * profile as of 2026-09-26, after a full audit against the vanilla {@code Biomes} constant list
 * confirmed every other vanilla biome (including snowy plains, pale gardens, and stony shores,
 * closed off that day) is covered. Adding one for another biome family is a small, self-contained
 * addition.
 */
public record NaturalRegionProfile(EntityType type, Set<ResourceKey<Biome>> biomes, List<String> terrainNouns, List<String> thematicPrefixes) {

    public static final List<NaturalRegionProfile> ALL = List.of(
            new NaturalRegionProfile(
                    EntityType.DESERT,
                    Set.of(Biomes.DESERT),
                    List.of("Sands", "Dunes", "Wastes", "Flat", "Reach"),
                    List.of("Sunbaked", "Forsaken", "Scorched", "Cracked", "Dusty")
            ),
            new NaturalRegionProfile(
                    EntityType.FOREST,
                    Set.of(Biomes.FOREST, Biomes.DARK_FOREST, Biomes.BIRCH_FOREST, Biomes.FLOWER_FOREST,
                            Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.CHERRY_GROVE, Biomes.PALE_GARDEN),
                    List.of("Grove", "Thicket", "Copse", "Hollow", "Woods", "Boughs"),
                    List.of("Whispering", "Mossy", "Quiet", "Forbidden", "Shadowed", "Ancient", "Blossoming")
            ),
            new NaturalRegionProfile(
                    EntityType.PLAINS,
                    Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SNOWY_PLAINS),
                    List.of("Field", "Pasture", "Flat", "Stretch", "Grasslands"),
                    List.of("Open", "Green", "Windrow", "Sunlit", "Golden")
            ),
            new NaturalRegionProfile(
                    EntityType.SWAMP,
                    Set.of(Biomes.SWAMP, Biomes.MANGROVE_SWAMP),
                    List.of("Hollow", "Bog", "Thicket", "Mire", "Marsh"),
                    List.of("Murky", "Mossy", "Sunken", "Drowned", "Reedy")
            ),
            new NaturalRegionProfile(
                    EntityType.TAIGA,
                    Set.of(Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
                    List.of("Grove", "Pines", "Thicket", "Expanse", "Woods"),
                    List.of("Frostpine", "Needlebound", "Quiet", "Coldbranch", "Silent")
            ),
            new NaturalRegionProfile(
                    EntityType.JUNGLE,
                    Set.of(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE),
                    List.of("Grove", "Hollow", "Thicket", "Canopy", "Wilds"),
                    List.of("Tangled", "Vined", "Dense", "Untamed", "Lost")
            ),
            new NaturalRegionProfile(
                    EntityType.SAVANNA,
                    Set.of(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA),
                    List.of("Grassland", "Flat", "Field", "Plain", "Reach"),
                    List.of("Dry", "Sunburnt", "Golden", "Duskgrass", "Wide")
            ),
            new NaturalRegionProfile(
                    EntityType.BADLANDS,
                    Set.of(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS),
                    List.of("Hollow", "Flat", "Mesa", "Wastes", "Cliffs"),
                    List.of("Rusted", "Clayspire", "Cracked", "Painted", "Ashen")
            ),
            new NaturalRegionProfile(
                    EntityType.MOUNTAIN,
                    Set.of(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
                            Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS, Biomes.SNOWY_SLOPES,
                            Biomes.GROVE, Biomes.MEADOW),
                    List.of("Rise", "Ridge", "Crag", "Slope", "Peaks", "Steppes", "Summit"),
                    List.of("Rocky", "Stonewatch", "Windswept", "Highfell", "Shattered", "Skyreach")
            ),
            new NaturalRegionProfile(
                    EntityType.RIVER,
                    // Rivers are often narrower than one chunk cell, so chunk-center-point sampling
                    // (see NaturalRegionDiscovery) may miss or under-detect them -- a known
                    // limitation of the whole coarse discovery approach, not specific to rivers.
                    Set.of(Biomes.RIVER, Biomes.FROZEN_RIVER),
                    List.of("Stream", "Waters", "Bend", "Waterway", "Flow"),
                    List.of("Quiet", "Narrow", "Clearwater", "Winding", "Wandering")
            ),
            new NaturalRegionProfile(
                    EntityType.OCEAN,
                    Set.of(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN,
                            Biomes.DEEP_LUKEWARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
                            Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN),
                    List.of("Bay", "Cove", "Shoals", "Reach", "Tide"),
                    List.of("Shallow", "Tidewater", "Calm", "Saltmist", "Restless")
            ),
            new NaturalRegionProfile(
                    EntityType.BEACH,
                    Set.of(Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE),
                    List.of("Spit", "Shore", "Strand", "Flat", "Coast"),
                    List.of("Sandy", "Driftwood", "Quiet", "Tidewrack", "Sunbleached")
            ),
            // Deliberately one shared type/word-pool for every Nether biome rather than five
            // separate themed ones -- explicitly requested by the user rather than hand-authoring
            // a themed pool per sub-biome (nether_wastes/soul_sand_valley/crimson_forest/
            // warped_forest/basalt_deltas), see decisions.md, 2026-09-25.
            new NaturalRegionProfile(
                    EntityType.NETHER_WASTELAND,
                    Set.of(Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.CRIMSON_FOREST,
                            Biomes.WARPED_FOREST, Biomes.BASALT_DELTAS),
                    List.of("Hollow", "Reach", "Waste", "Expanse", "Blaze"),
                    List.of("Scorched", "Ashfall", "Smoldering", "Cinderbound", "Infernal")
            ),
            // Added 2026-09-25 (see decisions.md) -- a real playtest report: an established
            // underground base is a very common place for a player to actually be, and biome
            // sampling (NaturalRegionDiscovery#discover) uses the player's real 3D position, not a
            // surface heightmap sample, so standing in one of these previously always came back
            // completely unclassified. One shared type/pool for all three cave biomes, same
            // "don't hand-author five near-identical pools" reasoning as NETHER_WASTELAND.
            new NaturalRegionProfile(
                    EntityType.CAVE,
                    Set.of(Biomes.DRIPSTONE_CAVES, Biomes.LUSH_CAVES, Biomes.DEEP_DARK),
                    List.of("Hollow", "Depths", "Cavern", "Grotto", "Underreach"),
                    List.of("Sunless", "Echoing", "Dripping", "Buried", "Lightless")
            ),
            // Added 2026-09-25 (see decisions.md) -- the last two overworld biomes flagged as
            // uncovered since this class's original doc, closed off after a playtest report of
            // several places generating no location name at all.
            new NaturalRegionProfile(
                    EntityType.MUSHROOM_FIELDS,
                    Set.of(Biomes.MUSHROOM_FIELDS),
                    List.of("Isle", "Mycelium", "Growth", "Spore Flat", "Thicket"),
                    List.of("Spongy", "Towering", "Fungal", "Mottled", "Otherworldly")
            ),
            new NaturalRegionProfile(
                    EntityType.ICE_SPIKES,
                    Set.of(Biomes.ICE_SPIKES),
                    List.of("Spikes", "Reach", "Flat", "Expanse", "Shard"),
                    List.of("Frozen", "Glacial", "Glittering", "Jagged", "Frostbitten")
            )
    );

    public boolean matches(Holder<Biome> biome) {
        return biomes.stream().anyMatch(biome::is);
    }

    /** First profile (in declaration order) whose biome set contains {@code biome}, if any. */
    public static Optional<NaturalRegionProfile> find(Holder<Biome> biome) {
        return ALL.stream().filter(p -> p.matches(biome)).findFirst();
    }
}
