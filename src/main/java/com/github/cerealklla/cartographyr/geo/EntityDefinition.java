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
 * @param layerId which {@link Layer} this entity belongs to (see {@link Layer#REGION_ID}/{@link
 *                Layer#SETTLEMENT_ID} for Cartographyr's own built-in layers) — no validation
 *                against {@link LayerRegistry} at creation time; a dangling reference to an
 *                unregistered layer is allowed, same trust-based spirit as {@link EntityType}/
 *                {@link Classification}.
 * @param protectionLevel an explicit starting {@link ProtectionLevel} for this entity, or {@link
 *                Optional#empty()} to fall back to {@code layerId}'s configured default (see
 *                {@link ProtectionDefaults}), and then to {@link ProtectionLevel#UNPROTECTED} if
 *                the layer has no default configured either.
 */
public record EntityDefinition(
        ResourceKey<Level> dimension,
        Classification classification,
        EntityType type,
        Identifier layerId,
        Optional<String> name,
        Geometry geometry,
        LifecycleState lifecycleState,
        Optional<ProtectionLevel> protectionLevel
) {
}
