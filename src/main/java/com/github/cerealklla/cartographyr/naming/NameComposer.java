package com.github.cerealklla.cartographyr.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Shared word-pool composition helpers, used by both natural-region naming ({@code
 * natural.NaturalRegionDiscovery}) and settlement naming ({@code settlement.SettlementNaming}).
 * Replaces the earlier approach of hand-authoring every full name phrase (e.g. "The Forbidden
 * Forest") with picking from small, reusable word pools and combining them at generation time --
 * the same technique the user used when first prototyping this mod, ported here so both naming
 * systems share one implementation instead of duplicating the pick-and-combine logic. See
 * decisions.md, 2026-09-25.
 */
public final class NameComposer {

    private NameComposer() {
    }

    /**
     * Picks a random noun from {@code nouns}, and with probability {@code prefixChance} prepends a
     * random word from one randomly-chosen non-empty category in {@code prefixCategories} --
     * mirroring "chance to add Size, Climate, or Terrain prefix... choose random prefix... choose
     * random noun" from the user's own original design. Result is {@code "The <prefix> <noun>"} or
     * just {@code "The <noun>"}. Empty categories are skipped rather than treated as eligible picks
     * (e.g. a temperate biome contributing no climate prefix shouldn't reduce the odds of getting
     * one at all).
     */
    public static String composeDescriptive(Random random, double prefixChance, List<List<String>> prefixCategories, List<String> nouns) {
        String noun = nouns.get(random.nextInt(nouns.size()));
        List<List<String>> eligible = new ArrayList<>();
        for (List<String> category : prefixCategories) {
            if (!category.isEmpty()) {
                eligible.add(category);
            }
        }
        if (!eligible.isEmpty() && random.nextDouble() < prefixChance) {
            List<String> category = eligible.get(random.nextInt(eligible.size()));
            String prefix = category.get(random.nextInt(category.size()));
            return "The " + prefix + " " + noun;
        }
        return "The " + noun;
    }

    /**
     * Combines a random root + random suffix into one compact word (e.g. {@code "Wobble"} +
     * {@code "sburg"} -> {@code "Wobblesburg"}) -- for proper place names, not descriptive terrain,
     * so no leading "The" article.
     */
    public static String composeCompound(Random random, List<String> roots, List<String> suffixes) {
        String root = roots.get(random.nextInt(roots.size()));
        String suffix = suffixes.get(random.nextInt(suffixes.size()));
        return root + suffix;
    }
}
