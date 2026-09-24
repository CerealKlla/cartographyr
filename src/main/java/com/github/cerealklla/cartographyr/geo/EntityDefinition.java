package com.github.cerealklla.cartographyr.geo;

import java.util.Optional;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Everything needed to create a new {@link GeographicEntity}, minus the {@link EntityId} — that's
 * allocated by the storage layer on creation, not chosen by the caller. Mirrors the design
 * document's Section 5.1 "EntityDefinition" input to {@code createEntity}.
 */
public record EntityDefinition(
        ResourceKey<Level> dimension,
        Classification classification,
        EntityType type,
        Optional<String> name,
        Geometry geometry,
        LifecycleState lifecycleState
) {
}
