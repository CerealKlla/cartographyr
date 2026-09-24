package com.github.cerealklla.cartographyr.geo;

import java.util.Optional;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Everything needed to create a new {@link GeographicEntity}, minus the {@link EntityId} — that's
 * allocated by the storage layer on creation, not chosen by the caller. Mirrors the design
 * document's Section 5.1 "EntityDefinition" input to {@code createEntity}.
 *
 * @param layerId which {@link Layer} this entity belongs to (see {@link Layer#LOCATION_ID} for
 *                Cartographyr's own built-in layer) — no validation against {@link LayerRegistry}
 *                at creation time; a dangling reference to an unregistered layer is allowed, same
 *                trust-based spirit as {@link EntityType}/{@link Classification}.
 */
public record EntityDefinition(
        ResourceKey<Level> dimension,
        Classification classification,
        EntityType type,
        Identifier layerId,
        Optional<String> name,
        Geometry geometry,
        LifecycleState lifecycleState
) {
}
