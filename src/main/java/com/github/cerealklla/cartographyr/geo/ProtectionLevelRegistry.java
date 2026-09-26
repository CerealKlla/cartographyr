package com.github.cerealklla.cartographyr.geo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;

/**
 * In-memory registry of every known {@link ProtectionLevel}, populated by mods calling {@link
 * com.github.cerealklla.cartographyr.api.Cartography#registerProtectionLevel} at their own
 * startup -- not persisted, not tied to a specific world, rebuilt fresh every boot from whoever
 * registers. Same trust-based governance already resolved for {@link EntityType}/{@link
 * Classification}/{@link LayerRegistry}: no claiming step, registering under an id that's already
 * present just replaces it.
 *
 * <p>This registry exists purely so other mods (and eventually a config/debug screen) can
 * enumerate "every protection level anyone knows about" via {@link
 * com.github.cerealklla.cartographyr.api.Cartography#getRegisteredProtectionLevels}. A {@link
 * GeographicEntity} can carry a {@link ProtectionLevel} that was never registered here -- same
 * trust-based spirit as an unregistered {@link Layer} reference on {@code layerId}.
 */
public final class ProtectionLevelRegistry {

    private static final Map<Identifier, ProtectionLevel> LEVELS = new ConcurrentHashMap<>();

    private ProtectionLevelRegistry() {
    }

    public static void register(ProtectionLevel level) {
        LEVELS.put(level.id(), level);
    }

    public static Collection<ProtectionLevel> all() {
        return List.copyOf(LEVELS.values());
    }
}
