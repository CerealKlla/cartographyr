package com.github.cerealklla.cartographyr.natural;

import java.util.List;
import java.util.Set;

import com.github.cerealklla.cartographyr.geo.EntityType;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Pairs a set of vanilla biomes with an {@link EntityType} and two tiers of name options, for
 * {@link NaturalRegionDiscovery}. Internal to {@code .natural} — not part of Cartographyr's
 * persisted data or public API, just discovery-time configuration.
 *
 * <p>Covers the common overworld surface land biome families, matching the design document's
 * "discovered incrementally... not exhaustively" instruction (Section 5.8) — deliberately still
 * not everything: oceans, beaches, mushroom fields, cherry groves, ice spikes, and cave/Nether/End
 * biomes have no profile. Adding one for another biome family is a small, self-contained addition.
 */
record NaturalRegionProfile(EntityType type, Set<ResourceKey<Biome>> biomes, List<String> smallNames, List<String> largeNames) {

    static final List<NaturalRegionProfile> ALL = List.of(
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
            )
    );

    boolean matches(Holder<Biome> biome) {
        return biomes.stream().anyMatch(biome::is);
    }
}
