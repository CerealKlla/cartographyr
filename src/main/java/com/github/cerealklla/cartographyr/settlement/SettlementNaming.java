package com.github.cerealklla.cartographyr.settlement;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import com.github.cerealklla.cartographyr.geo.EntityType;
import com.github.cerealklla.cartographyr.naming.NameComposer;

/**
 * Picks a settlement name, optionally themed off a nearby natural feature (see {@code
 * SettlementDiscovery#findNearbyNaturalFeature}), and a starting {@link
 * SettlementDiscovery#discover} designation tier based on the structure's own piece count. Two
 * distinct name pools, not one:
 *
 * <ul>
 *   <li>{@link #THEMED} — keyed by the {@link EntityType} of whatever natural feature was found
 *       nearby, e.g. a river-adjacent settlement might get "Bridgeville." Only used when a
 *       feature was actually detected, and even then only some of the time (see {@link #pickName}) —
 *       not every settlement should read as environmentally themed.
 *   <li>{@link #NEUTRAL} — explicitly, unambiguously <em>not</em> nature-themed. This is the
 *       fallback whenever no feature is found, whenever the coin flip misses, and always for
 *       {@link EntityType#PLAINS} (see {@code SettlementDiscovery} — plains is deliberately
 *       excluded from "found a feature" entirely, so it always lands here).
 * </ul>
 *
 * <p><b>Compositional, 2026-09-25</b> (see decisions.md) — each pool used to be a flat list of
 * whole names; now it's a root-word pool + suffix pool combined at generation time ({@code
 * naming.NameComposer#composeCompound}), the same "small reusable word pools, not one full phrase
 * per name" technique {@code natural.NaturalRegionProfile} uses. Far more combinations from the
 * same amount of authored content, without hand-writing every one.
 *
 * <p>All names/tiers here are placeholder content, same as every other untuned magnitude in this
 * project.
 */
final class SettlementNaming {

    // Placeholder ratio -- how often a detected feature actually gets used, rather than always
    // preferring the neutral pool when one's available. Not everything should read as themed.
    private static final double THEMED_CHANCE = 0.5;

    // Deliberately no nature/geography word roots or suffixes here (brook/wood/field/haven/stone/
    // etc.) -- the whole point of NEUTRAL is to read as unambiguously not nature-themed, not just
    // accidentally lack a matching feature. Enforced by SettlementNamingTest.
    /** Package-visible for {@code SettlementNamingTest}. */
    static final List<String> NEUTRAL_ROOTS = List.of(
            "Dal", "Wren", "Perc", "Marl", "Corw", "Talb", "Reev", "Aldr", "Dern", "Wobb", "Bram", "Cadw");
    /** Package-visible for {@code SettlementNamingTest}. */
    static final List<String> NEUTRAL_SUFFIXES = List.of(
            "e", "ow", "ic", "ot", "sburg", "brig", "ton", "don", "ley", "in", "ith");

    /** Package-visible for {@code SettlementNamingTest}. */
    static final Map<EntityType, RootSuffixPool> THEMED = Map.of(
            EntityType.RIVER, new RootSuffixPool(
                    List.of("Bridge", "Ford", "River", "Weir", "Brook", "Mill"),
                    List.of("ville", "ham", "mouth", "brook", "ford", "wick")),
            EntityType.FOREST, new RootSuffixPool(
                    List.of("Oak", "Timber", "Green", "Ash", "Elm", "Thorn"),
                    List.of("stead", "gate", "hollow", "wood", "ton", "mere")),
            EntityType.DESERT, new RootSuffixPool(
                    List.of("Sand", "Dune", "Sun", "Amber", "Dust"),
                    List.of("haven", "mere", "scar", "reach", "ford")),
            EntityType.MOUNTAIN, new RootSuffixPool(
                    List.of("Stone", "Crag", "High", "Peak", "Iron"),
                    List.of("wick", "moor", "hold", "shadow", "gate")),
            EntityType.SWAMP, new RootSuffixPool(
                    List.of("Moss", "Bog", "Reed", "Murk", "Fen"),
                    List.of("port", "land", "marsh", "water", "mire")),
            EntityType.TAIGA, new RootSuffixPool(
                    List.of("Frost", "Snow", "Pine", "Cold", "Needle"),
                    List.of("pine", "bough", "hollow", "ridge", "reach")),
            EntityType.JUNGLE, new RootSuffixPool(
                    List.of("Vine", "Canopy", "Fern", "Moss", "Bramble"),
                    List.of("haven", "reach", "wild", "grove", "vale")),
            EntityType.SAVANNA, new RootSuffixPool(
                    List.of("Golden", "Dusk", "Amber", "Sun", "Wide"),
                    List.of("reach", "grass", "plain", "veld", "field")),
            EntityType.BADLANDS, new RootSuffixPool(
                    List.of("Red", "Rust", "Clay", "Ash", "Dry"),
                    List.of("scar", "cliff", "stead", "canyon", "mesa"))
    );

    /**
     * A settlement's starting designation tier, by structure piece count -- a natural small-to-
     * large progression per the user's own ordering: Hovel &lt; Outpost &lt; Settlement &lt;
     * Village &lt; Town &lt; City (corrected 2026-09-25 -- an earlier version had Village/Outpost
     * and Town/Settlement swapped, see decisions.md). Package-visible for {@code
     * SettlementNamingTest}.
     */
    private static final List<DesignationTier> DESIGNATION_TIERS = List.of(
            new DesignationTier(20, "Hovel"),
            new DesignationTier(40, "Outpost"),
            new DesignationTier(70, "Settlement"),
            new DesignationTier(110, "Village"),
            new DesignationTier(160, "Town")
            // Anything at or above the last threshold falls through to "City" -- see designationFor.
    );

    /** Package-visible for {@code SettlementNamingTest}. */
    record RootSuffixPool(List<String> roots, List<String> suffixes) {
    }

    private record DesignationTier(int maxPieceCount, String designation) {
    }

    private SettlementNaming() {
    }

    static String pickName(Optional<EntityType> nearbyFeature, Random random) {
        RootSuffixPool themedPool = nearbyFeature.map(THEMED::get).orElse(null);
        if (themedPool != null && random.nextDouble() < THEMED_CHANCE) {
            return NameComposer.composeCompound(random, themedPool.roots(), themedPool.suffixes());
        }
        return NameComposer.composeCompound(random, NEUTRAL_ROOTS, NEUTRAL_SUFFIXES);
    }

    /** Package-visible for {@code SettlementNamingTest}. */
    static String designationFor(int pieceCount) {
        for (DesignationTier tier : DESIGNATION_TIERS) {
            if (pieceCount < tier.maxPieceCount()) {
                return tier.designation();
            }
        }
        return "City";
    }
}
