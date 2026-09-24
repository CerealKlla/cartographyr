package com.github.cerealklla.cartographyr.settlement;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.github.cerealklla.cartographyr.geo.EntityType;

/**
 * Picks a settlement name, optionally themed off a nearby natural feature (see {@code
 * SettlementDiscovery#findNearbyNaturalFeature}). Two distinct pools, not one:
 *
 * <ul>
 *   <li>{@link #THEMED} — keyed by the {@link EntityType} of whatever natural feature was found
 *       nearby, e.g. a river-adjacent settlement might get "Bridgeville." Only used when a
 *       feature was actually detected, and even then only some of the time (see {@link #pick}) —
 *       not every settlement should read as environmentally themed.
 *   <li>{@link #NEUTRAL} — explicitly, unambiguously <em>not</em> nature-themed ("The Town of
 *       Dale," never "Millbrook" or anything else that could accidentally read as thematic). This
 *       is the fallback whenever no feature is found, whenever the coin flip misses, and always
 *       for {@link EntityType#PLAINS} (see {@code SettlementDiscovery} — plains is deliberately
 *       excluded from "found a feature" entirely, so it always lands here).
 * </ul>
 *
 * All names here are placeholder content, same as every other untuned magnitude in this project.
 */
final class SettlementNaming {

    // Placeholder ratio -- how often a detected feature actually gets used, rather than always
    // preferring the neutral pool when one's available. Not everything should read as themed.
    private static final double THEMED_CHANCE = 0.5;

    private static final List<String> NEUTRAL = List.of(
            "The Town of Dale", "The Town of Wren", "The Town of Percy", "The Town of Marlow",
            "The Town of Corwin", "The Town of Talbot", "The Town of Reeve", "The Town of Aldric"
    );

    private static final Map<EntityType, List<String>> THEMED = Map.of(
            EntityType.RIVER, List.of("Bridgeville", "Fordham", "Rivermouth", "Weirbrook"),
            EntityType.FOREST, List.of("Oakstead", "Timbergate", "Greenhollow", "Ashwood"),
            EntityType.DESERT, List.of("Sandhaven", "Dunemere", "Sunscar", "Amberreach"),
            EntityType.MOUNTAIN, List.of("Stonewick", "Cragmoor", "Highhold", "Peakshadow"),
            EntityType.SWAMP, List.of("Mossport", "Bogland", "Reedmarsh", "Murkwater"),
            EntityType.TAIGA, List.of("Frostpine", "Snowbough", "Pinehollow", "Coldridge"),
            EntityType.JUNGLE, List.of("Vinehaven", "Canopyreach", "Fernwild", "Mossgrove"),
            EntityType.SAVANNA, List.of("Goldenreach", "Duskgrass", "Amberplain", "Sunveld"),
            EntityType.BADLANDS, List.of("Redscar", "Rustcliff", "Claystead", "Ashcanyon")
    );

    private SettlementNaming() {
    }

    static String pick(Optional<EntityType> nearbyFeature, Random random) {
        List<String> themedPool = nearbyFeature.map(THEMED::get).orElse(null);
        if (themedPool != null && random.nextDouble() < THEMED_CHANCE) {
            return themedPool.get(random.nextInt(themedPool.size()));
        }
        return NEUTRAL.get(random.nextInt(NEUTRAL.size()));
    }
}
