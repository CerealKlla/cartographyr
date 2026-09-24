package com.github.cerealklla.cartographyr.geo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;

/**
 * In-memory registry of {@link Layer}s, populated by mods calling {@link
 * com.github.cerealklla.cartographyr.api.Cartography#registerLayer} at their own startup (before
 * any world/server exists) — not persisted, not tied to a specific world, rebuilt fresh every
 * boot from whoever registers. Same trust-based governance already resolved for {@link
 * EntityType}/{@link Classification}: no claiming step, last {@link #register} for a given id
 * wins.
 */
public final class LayerRegistry {

    private static final Map<Identifier, Layer> LAYERS = new ConcurrentHashMap<>();

    private LayerRegistry() {
    }

    public static void register(Layer layer) {
        LAYERS.put(layer.id(), layer);
    }

    public static Optional<Layer> get(Identifier id) {
        return Optional.ofNullable(LAYERS.get(id));
    }

    public static Collection<Layer> all() {
        return List.copyOf(LAYERS.values());
    }
}
