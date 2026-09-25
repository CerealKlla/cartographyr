package com.github.cerealklla.cartographyr.geo;

import java.util.List;
import java.util.Random;

/**
 * Composes a {@link GeographicEntity}'s canonical human-facing display text from its raw data
 * fields ({@code name}, {@code designation}, {@code lifecycleState}) -- the single implementation
 * every consumer (Lyfe, a future minimap, a future town-management mod) should call, so two mods
 * never disagree about what text represents a given place. Moved here from Lyfe 2026-09-25 (see
 * decisions.md) after the user pointed out that leaving composition to each consumer risked
 * exactly that kind of drift -- {@code name}/{@code designation}/{@code specialStatus} living in
 * Cartographyr as shared data doesn't help if every mod renders them differently.
 *
 * <p>Shape: {@code "[<ruin prefix> ]<designation> of <name>"} when a designation is present (e.g.
 * "Lost Village of Stonecrest"), or just {@code "[<ruin prefix> ]<name>"} otherwise (natural
 * regions never have a designation -- see {@link GeographicEntity#designation()}'s own doc).
 */
public final class DisplayText {

    // Placeholder pool, untuned like every other magnitude in this project.
    private static final List<String> RUIN_PREFIXES = List.of("Lost", "Ruined", "Abandoned", "Forgotten", "Fallen");

    private DisplayText() {
    }

    public static String forEntity(GeographicEntity entity) {
        String name = entity.name().orElse("an unnamed place");
        String base = entity.designation().map(designation -> designation + " of " + name).orElse(name);

        if (!isRuined(entity.lifecycleState())) {
            return base;
        }
        // Seeded off the entity's own id, not a fresh Random each call, so the same ruin stays
        // described the same way every time it's read rather than flickering between prefixes.
        Random random = new Random(entity.id().value());
        String prefix = RUIN_PREFIXES.get(random.nextInt(RUIN_PREFIXES.size()));
        return prefix + " " + base;
    }

    private static boolean isRuined(LifecycleState state) {
        return state == LifecycleState.ABANDONED || state == LifecycleState.DESTROYED;
    }
}
