package com.github.cerealklla.cartographyr.natural;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link NaturalRegionDiscovery#pickName} -- the pure, composable part of natural-region
 * naming ({@code naming.NameComposer} + {@code naming.RegionWordPools} + a profile's own
 * pools). The rest of {@code NaturalRegionDiscovery} needs a live {@code ServerLevel} to sample
 * real biome data and isn't unit-testable, same limitation documented on the class itself.
 */
class NaturalRegionDiscoveryTest {

    private static final NaturalRegionProfile FOREST_PROFILE = NaturalRegionProfile.ALL.stream()
            .filter(p -> p.type().equals(com.github.cerealklla.cartographyr.geo.EntityType.FOREST))
            .findFirst().orElseThrow();

    @Test
    void everyGeneratedNameContainsOneOfTheProfilesTerrainNouns() {
        for (int i = 0; i < 100; i++) {
            String name = NaturalRegionDiscovery.pickName(FOREST_PROFILE, 5, 0.8f);
            assertTrue(containsAny(name, FOREST_PROFILE.terrainNouns()), name + " should contain a forest terrain noun");
        }
    }

    @Test
    void everyGeneratedNameStartsWithTheArticle() {
        for (int i = 0; i < 20; i++) {
            String name = NaturalRegionDiscovery.pickName(FOREST_PROFILE, 5, 0.8f);
            assertTrue(name.startsWith("The "), name + " should start with \"The \"");
        }
    }

    @Test
    void bareNounWithoutPrefixIsAPossibleOutcome() {
        // With PREFIX_CHANCE < 1.0, enough draws should eventually produce a bare "The <noun>"
        // with no modifier at all -- confirms the coin flip is actually wired up, not forcing a
        // prefix every time.
        boolean sawBare = false;
        for (int i = 0; i < 500 && !sawBare; i++) {
            String name = NaturalRegionDiscovery.pickName(FOREST_PROFILE, 5, 0.8f);
            sawBare = isBareNoun(name, FOREST_PROFILE.terrainNouns());
        }
        assertTrue(sawBare, "expected at least one bare-noun (no prefix) name across repeated draws");
    }

    @Test
    void coldTemperatureCanProduceAColdPrefix() {
        boolean sawColdPrefix = false;
        for (int i = 0; i < 500 && !sawColdPrefix; i++) {
            String name = NaturalRegionDiscovery.pickName(FOREST_PROFILE, 5, -0.5f);
            sawColdPrefix = containsAny(name, com.github.cerealklla.cartographyr.naming.RegionWordPools.CLIMATE_COLD);
        }
        assertTrue(sawColdPrefix, "expected at least one cold-climate-prefixed name for a cold biome across repeated draws");
    }

    private static boolean containsAny(String name, List<String> words) {
        for (String word : words) {
            if (name.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBareNoun(String name, List<String> nouns) {
        for (String noun : nouns) {
            if (name.equals("The " + noun)) {
                return true;
            }
        }
        return false;
    }
}
