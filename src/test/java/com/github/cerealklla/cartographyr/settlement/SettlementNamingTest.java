package com.github.cerealklla.cartographyr.settlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.cerealklla.cartographyr.geo.EntityType;

class SettlementNamingTest {

    private static final List<String> NEUTRAL_NAMES = List.of(
            "The Town of Dale", "The Town of Wren", "The Town of Percy", "The Town of Marlow",
            "The Town of Corwin", "The Town of Talbot", "The Town of Reeve", "The Town of Aldric"
    );

    @Test
    void noFeatureFoundAlwaysPicksFromNeutralPool() {
        Random random = new Random(1);
        for (int i = 0; i < 20; i++) {
            String name = SettlementNaming.pick(Optional.empty(), random);
            assertTrue(NEUTRAL_NAMES.contains(name), name + " should be a neutral name");
        }
    }

    @Test
    void featureWithNoThemedPoolFallsBackToNeutral() {
        // PLAINS deliberately has no THEMED entry (see SettlementDiscovery) -- pick() should
        // treat that exactly like "no feature found."
        Random random = new Random(2);
        for (int i = 0; i < 20; i++) {
            String name = SettlementNaming.pick(Optional.of(EntityType.PLAINS), random);
            assertTrue(NEUTRAL_NAMES.contains(name), name + " should be a neutral name");
        }
    }

    @Test
    void featureWithThemedPoolCanProduceEitherThemedOrNeutralNames() {
        // With a real theme available, repeated calls should hit both pools eventually (not a
        // 100%/0% split either way) -- confirms the coin flip is actually wired up.
        Random random = new Random(3);
        boolean sawThemed = false;
        boolean sawNeutral = false;
        for (int i = 0; i < 200 && !(sawThemed && sawNeutral); i++) {
            String name = SettlementNaming.pick(Optional.of(EntityType.RIVER), random);
            if (NEUTRAL_NAMES.contains(name)) {
                sawNeutral = true;
            } else {
                sawThemed = true;
            }
        }
        assertTrue(sawThemed, "expected at least one themed name across 200 draws");
        assertTrue(sawNeutral, "expected at least one neutral name across 200 draws");
    }

    @Test
    void themedNameMatchesTheDetectedFeatureNotSomeOtherOne() {
        // A river-adjacent settlement should never get a mountain-themed name, and vice versa --
        // this is the actual point of the feature (no mismatched theming).
        List<String> riverNames = List.of("Bridgeville", "Fordham", "Rivermouth", "Weirbrook");
        Random random = new Random(4);
        for (int i = 0; i < 50; i++) {
            String name = SettlementNaming.pick(Optional.of(EntityType.RIVER), random);
            assertTrue(NEUTRAL_NAMES.contains(name) || riverNames.contains(name),
                    name + " should be either neutral or river-themed, never another biome's theme");
        }
    }

    @Test
    void everyNeutralNameIsUnambiguouslyNonNature() {
        // The whole point: a "not themed" name must read as deliberately not themed, not just
        // accidentally lack a matching feature. No nature/geography word roots allowed.
        List<String> forbiddenRoots = List.of("brook", "wood", "field", "haven", "stone", "river",
                "mountain", "forest", "sand", "dune", "marsh", "bog", "pine", "vine", "grass");
        for (String name : NEUTRAL_NAMES) {
            String lower = name.toLowerCase();
            for (String root : forbiddenRoots) {
                assertTrue(!lower.contains(root), name + " contains nature-toned root \"" + root + "\"");
            }
        }
    }
}
