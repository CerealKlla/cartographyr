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
 * <p>Deliberately covers only two biome families for this first cut, matching the design
 * document's "discovered incrementally... not exhaustively" instruction (Section 5.8). Biomes not
 * covered by any profile here (plains, swamp, etc.) simply aren't discovered yet — adding a
 * profile for another biome family later is a small, self-contained addition.
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
            )
    );

    boolean matches(Holder<Biome> biome) {
        return biomes.stream().anyMatch(biome::is);
    }
}
