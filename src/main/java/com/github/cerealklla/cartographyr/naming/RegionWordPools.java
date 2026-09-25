package com.github.cerealklla.cartographyr.naming;

import java.util.List;

/**
 * Cross-biome word pools for natural-region name composition (see {@link NameComposer}) --
 * shared across every {@code natural.NaturalRegionProfile}, as opposed to that class's own
 * per-family {@code terrainNouns}/{@code thematicPrefixes}. All placeholder content, untuned like
 * everything else in this project.
 */
public final class RegionWordPools {

    private RegionWordPools() {
    }

    public static final List<String> SIZE_SMALL = List.of("Little", "Small", "Modest", "Humble", "Narrow");
    public static final List<String> SIZE_LARGE = List.of("Vast", "Grand", "Endless", "Boundless", "Great");

    public static final List<String> CLIMATE_COLD = List.of("Frozen", "Frosted", "Chilly", "Frostbound", "Wintry");
    public static final List<String> CLIMATE_HOT = List.of("Sunbaked", "Scorched", "Parched", "Blazing", "Sweltering");

    /**
     * Not tied to any particular size/climate/biome -- a wildcard pool used as an occasional
     * trailing "of &lt;word&gt;" (e.g. "The Ancient Woods of Sorrow"), not a fourth interchangeable
     * prefix category -- see {@code natural.NaturalRegionDiscovery#pickName}.
     */
    public static final List<String> GENERIC_FLAVOR = List.of("Sorrow", "Silence", "Shadow", "Solitude", "Wonder", "Whisper");
}
