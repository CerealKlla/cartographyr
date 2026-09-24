package com.github.cerealklla.cartographyr.geo;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A geographic entity: something Cartographyr tracks the identity, location, and lifecycle of.
 * Immutable — updates go through the "with" methods below, producing a new instance that the
 * storage layer replaces the old one with (see {@code CartographySavedData#updateEntity}).
 *
 * This is the trimmed first-milestone shape: no characteristics, amenities, history, structure
 * references, source, or extension data yet (design document Section 3 lists the full field set).
 */
public record GeographicEntity(
        EntityId id,
        ResourceKey<Level> dimension,
        Classification classification,
        EntityType type,
        Optional<String> name,
        Geometry geometry,
        LifecycleState lifecycleState
) {
    public static final Codec<GeographicEntity> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityId.CODEC.fieldOf("id").forGetter(GeographicEntity::id),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(GeographicEntity::dimension),
            Classification.CODEC.fieldOf("classification").forGetter(GeographicEntity::classification),
            EntityType.CODEC.fieldOf("type").forGetter(GeographicEntity::type),
            Codec.STRING.optionalFieldOf("name").forGetter(GeographicEntity::name),
            Geometry.CODEC.fieldOf("geometry").forGetter(GeographicEntity::geometry),
            LifecycleState.CODEC.fieldOf("lifecycle_state").forGetter(GeographicEntity::lifecycleState)
    ).apply(i, GeographicEntity::new));

    public static GeographicEntity create(EntityId id, EntityDefinition definition) {
        return new GeographicEntity(
                id,
                definition.dimension(),
                definition.classification(),
                definition.type(),
                definition.name(),
                definition.geometry(),
                definition.lifecycleState()
        );
    }

    public GeographicEntity withLifecycleState(LifecycleState newState) {
        return new GeographicEntity(id, dimension, classification, type, name, geometry, newState);
    }

    public GeographicEntity withGeometry(Geometry newGeometry) {
        return new GeographicEntity(id, dimension, classification, type, name, newGeometry, lifecycleState);
    }

    public GeographicEntity withName(Optional<String> newName) {
        return new GeographicEntity(id, dimension, classification, type, newName, geometry, lifecycleState);
    }
}
