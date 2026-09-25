package com.github.cerealklla.cartographyr.settlement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.cerealklla.cartographyr.geo.EntityType;

class SettlementNamingTest {

    private static boolean isNeutralCombo(String name) {
        return isCombo(name, SettlementNaming.NEUTRAL_ROOTS, SettlementNaming.NEUTRAL_SUFFIXES);
    }

    private static boolean isThemedCombo(String name, EntityType type) {
        SettlementNaming.RootSuffixPool pool = SettlementNaming.THEMED.get(type);
        return pool != null && isCombo(name, pool.roots(), pool.suffixes());
    }

    private static boolean isCombo(String name, List<String> roots, List<String> suffixes) {
        for (String root : roots) {
            if (name.startsWith(root)) {
                String remainder = name.substring(root.length());
                if (suffixes.contains(remainder)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void noFeatureFoundAlwaysPicksFromNeutralPool() {
        Random random = new Random(1);
        for (int i = 0; i < 20; i++) {
            String name = SettlementNaming.pickName(Optional.empty(), random);
            assertTrue(isNeutralCombo(name), name + " should be a neutral root+suffix combo");
        }
    }

    @Test
    void featureWithNoThemedPoolFallsBackToNeutral() {
        // PLAINS deliberately has no THEMED entry (see SettlementDiscovery) -- pickName() should
        // treat that exactly like "no feature found."
        Random random = new Random(2);
        for (int i = 0; i < 20; i++) {
            String name = SettlementNaming.pickName(Optional.of(EntityType.PLAINS), random);
            assertTrue(isNeutralCombo(name), name + " should be a neutral root+suffix combo");
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
            String name = SettlementNaming.pickName(Optional.of(EntityType.RIVER), random);
            if (isNeutralCombo(name)) {
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
        Random random = new Random(4);
        for (int i = 0; i < 50; i++) {
            String name = SettlementNaming.pickName(Optional.of(EntityType.RIVER), random);
            assertTrue(isNeutralCombo(name) || isThemedCombo(name, EntityType.RIVER),
                    name + " should be either neutral or river-themed, never another biome's theme");
        }
    }

    @Test
    void everyNeutralWordIsUnambiguouslyNonNature() {
        // The whole point: a "not themed" name must read as deliberately not themed, not just
        // accidentally lack a matching feature. No nature/geography word roots allowed, in either
        // the root or suffix pool (so no combination of them can produce one).
        List<String> forbiddenRoots = List.of("brook", "wood", "field", "haven", "stone", "river",
                "mountain", "forest", "sand", "dune", "marsh", "bog", "pine", "vine", "grass");
        for (String word : SettlementNaming.NEUTRAL_ROOTS) {
            assertNoForbiddenRoot(word, forbiddenRoots);
        }
        for (String word : SettlementNaming.NEUTRAL_SUFFIXES) {
            assertNoForbiddenRoot(word, forbiddenRoots);
        }
    }

    private static void assertNoForbiddenRoot(String word, List<String> forbiddenRoots) {
        String lower = word.toLowerCase();
        for (String root : forbiddenRoots) {
            assertTrue(!lower.contains(root), word + " contains nature-toned root \"" + root + "\"");
        }
    }

    @Test
    void designationForFollowsPieceCountTiers() {
        assertEquals("Hovel", SettlementNaming.designationFor(0));
        assertEquals("Hovel", SettlementNaming.designationFor(19));
        assertEquals("Village", SettlementNaming.designationFor(20));
        assertEquals("Village", SettlementNaming.designationFor(39));
        assertEquals("Outpost", SettlementNaming.designationFor(40));
        assertEquals("Outpost", SettlementNaming.designationFor(69));
        assertEquals("Town", SettlementNaming.designationFor(70));
        assertEquals("Town", SettlementNaming.designationFor(109));
        assertEquals("Settlement", SettlementNaming.designationFor(110));
        assertEquals("Settlement", SettlementNaming.designationFor(159));
        assertEquals("City", SettlementNaming.designationFor(160));
        assertEquals("City", SettlementNaming.designationFor(1000));
    }
}
