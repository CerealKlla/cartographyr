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
 * Pairs a set of vanilla biomes with an {@link EntityType} and two tiers of name options, for
 * {@link NaturalRegionDiscovery}. Not part of Cartographyr's persisted data or public API, just
 * discovery-time configuration — but made {@code public} (widened 2026-09-24) so {@code
 * .settlement.SettlementDiscovery} can reuse the same biome-matching logic for contextual
 * settlement naming, rather than duplicating a second copy of the biome-family mapping.
 *
 * <p>Covers the common overworld surface land biome families plus oceans/beaches/the Nether
 * (added 2026-09-25, see decisions.md), matching the design document's "discovered
 * incrementally... not exhaustively" instruction (Section 5.8) — deliberately still not
 * everything: mushroom fields, cherry groves, ice spikes, and cave/End biomes have no profile.
 * Adding one for another biome family is a small, self-contained addition.
 */
public record NaturalRegionProfile(EntityType type, Set<ResourceKey<Biome>> biomes, List<String> smallNames, List<String> largeNames) {

    public static final List<NaturalRegionProfile> ALL = List.of(
            new NaturalRegionProfile(
                    EntityType.DESERT,
                    Set.of(Biomes.DESERT),
                    List.of("The Sunbaked Flat", "Dust Hollow", "The Dry Reach", "Cracked Basin"),
                    List.of("The Forsaken Sands", "The Scorched Expanse", "The Great Dune Sea", "The Endless Waste")
            ),
            new NaturalRegionProfile(
                    EntityType.FOREST,
                    Set.of(Biomes.FOREST, Biomes.DARK_FOREST, Biomes.BIRCH_FOREST, Biomes.FLOWER_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST),
                    List.of("The Whispering Grove", "Mossy Thicket", "The Quiet Copse", "Fernhollow"),
                    List.of("The Forbidden Forest", "The Wildwood", "The Shadowed Woods", "The Ancient Boughs")
            ),
            new NaturalRegionProfile(
                    EntityType.PLAINS,
                    Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS),
                    List.of("The Open Field", "Quiet Pasture", "Windrow Flat", "The Green Stretch"),
                    List.of("The Boundless Plain", "The Wide Grasslands", "The Endless Fields", "The Open Expanse")
            ),
            new NaturalRegionProfile(
                    EntityType.SWAMP,
                    Set.of(Biomes.SWAMP, Biomes.MANGROVE_SWAMP),
                    List.of("The Murky Hollow", "Mossbog", "The Wet Thicket", "Reedmire"),
                    List.of("The Great Mire", "The Sunken Marsh", "The Endless Bog", "The Drowned Wood")
            ),
            new NaturalRegionProfile(
                    EntityType.TAIGA,
                    Set.of(Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
                    List.of("The Frostpine Grove", "Needlewood", "The Quiet Pines", "Coldbranch Thicket"),
                    List.of("The Frostpine Expanse", "The Silent Taiga", "The Snowbound Forest", "The Ancient Pines")
            ),
            new NaturalRegionProfile(
                    EntityType.JUNGLE,
                    Set.of(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE),
                    List.of("The Tangled Grove", "Vinehollow", "The Dense Thicket", "Canopy's Edge"),
                    List.of("The Emerald Canopy", "The Untamed Jungle", "The Verdant Wilds", "The Lost Canopy")
            ),
            new NaturalRegionProfile(
                    EntityType.SAVANNA,
                    Set.of(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA),
                    List.of("The Dry Grassland", "Acacia Flat", "The Sunburnt Field", "Duskgrass Reach"),
                    List.of("The Golden Savanna", "The Wide Acacia Plain", "The Sunbaked Grassland", "The Endless Savanna")
            ),
            new NaturalRegionProfile(
                    EntityType.BADLANDS,
                    Set.of(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS),
                    List.of("The Rust Hollow", "Clayspire Flat", "The Cracked Mesa", "Redrock Hollow"),
                    List.of("The Painted Wastes", "The Great Mesa", "The Burning Cliffs", "The Ashen Badlands")
            ),
            new NaturalRegionProfile(
                    EntityType.MOUNTAIN,
                    Set.of(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
                            Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS, Biomes.SNOWY_SLOPES,
                            Biomes.GROVE, Biomes.MEADOW),
                    List.of("The Rocky Rise", "Stonewatch Ridge", "The Windswept Crag", "Highfell Slope"),
                    List.of("The Shattered Peaks", "The Frostbound Range", "The Ancient Mountains", "The Skyreach Summit")
            ),
            new NaturalRegionProfile(
                    EntityType.RIVER,
                    // Rivers are often narrower than one chunk cell, so chunk-center-point sampling
                    // (see NaturalRegionDiscovery) may miss or under-detect them -- a known
                    // limitation of the whole coarse discovery approach, not specific to rivers.
                    Set.of(Biomes.RIVER, Biomes.FROZEN_RIVER),
                    List.of("The Quiet Stream", "Brookside", "The Narrow Waters", "Clearwater Bend"),
                    List.of("The Winding River", "The Silverflow", "The Long Waterway", "The Wandering River")
            ),
            new NaturalRegionProfile(
                    EntityType.OCEAN,
                    Set.of(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN,
                            Biomes.DEEP_LUKEWARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
                            Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN),
                    List.of("The Shallow Bay", "Tidewater Cove", "The Calm Shoals", "Saltmist Reach"),
                    List.of("The Deep Blue", "The Boundless Sea", "The Drowned Expanse", "The Endless Tide")
            ),
            new NaturalRegionProfile(
                    EntityType.BEACH,
                    Set.of(Biomes.BEACH, Biomes.SNOWY_BEACH),
                    List.of("The Sandy Spit", "Driftwood Shore", "The Quiet Strand", "Tidewrack Flat"),
                    List.of("The Sunbleached Coast", "The Long Shore", "The Windswept Strand", "The Silver Sands")
            ),
            // Deliberately one shared type/name-pool for every Nether biome rather than five
            // separate themed ones -- explicitly requested by the user rather than hand-authoring
            // a themed pool per sub-biome (nether_wastes/soul_sand_valley/crimson_forest/
            // warped_forest/basalt_deltas), see decisions.md, 2026-09-25.
            new NaturalRegionProfile(
                    EntityType.NETHER_WASTELAND,
                    Set.of(Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.CRIMSON_FOREST,
                            Biomes.WARPED_FOREST, Biomes.BASALT_DELTAS),
                    List.of("The Scorched Hollow", "Ashfall Reach", "The Smoldering Waste", "Cinderflat"),
                    List.of("The Burning Wastes", "The Infernal Expanse", "The Charred Reaches", "The Endless Blaze")
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
