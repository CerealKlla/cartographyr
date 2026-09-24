package com.github.cerealklla.cartographyr.geo;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A geographic entity: something Cartographyr tracks the identity, location, and lifecycle of.
 * Immutable — updates go through the "with" methods below, producing a new instance that the
 * storage layer replaces the old one with (see {@code CartographySavedData#updateEntity}).
 *
 * This is the trimmed first-milestone shape: no characteristics, amenities, history, source, or
 * extension data yet (design document Section 3 lists the full field set).
 */
public record GeographicEntity(
        EntityId id,
        ResourceKey<Level> dimension,
        Classification classification,
        EntityType type,
        Optional<String> name,
        Geometry geometry,
        LifecycleState lifecycleState,
        Set<GlobalPos> structureReferences
) {
    public static final Codec<GeographicEntity> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityId.CODEC.fieldOf("id").forGetter(GeographicEntity::id),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(GeographicEntity::dimension),
            Classification.CODEC.fieldOf("classification").forGetter(GeographicEntity::classification),
            EntityType.CODEC.fieldOf("type").forGetter(GeographicEntity::type),
            Codec.STRING.optionalFieldOf("name").forGetter(GeographicEntity::name),
            Geometry.CODEC.fieldOf("geometry").forGetter(GeographicEntity::geometry),
            LifecycleState.CODEC.fieldOf("lifecycle_state").forGetter(GeographicEntity::lifecycleState),
            Codec.list(GlobalPos.CODEC).xmap(Set::copyOf, List::copyOf)
                    .fieldOf("structure_references").forGetter(GeographicEntity::structureReferences)
    ).apply(i, GeographicEntity::new));

    public static GeographicEntity create(EntityId id, EntityDefinition definition) {
        return new GeographicEntity(
                id,
                definition.dimension(),
                definition.classification(),
                definition.type(),
                definition.name(),
                definition.geometry(),
                definition.lifecycleState(),
                Set.of()
        );
    }

    public GeographicEntity withLifecycleState(LifecycleState newState) {
        return new GeographicEntity(id, dimension, classification, type, name, geometry, newState, structureReferences);
    }

    public GeographicEntity withGeometry(Geometry newGeometry) {
        return new GeographicEntity(id, dimension, classification, type, name, newGeometry, lifecycleState, structureReferences);
    }

    public GeographicEntity withName(Optional<String> newName) {
        return new GeographicEntity(id, dimension, classification, type, newName, geometry, lifecycleState, structureReferences);
    }

    public GeographicEntity withAddedStructureReference(GlobalPos structureReference) {
        Set<GlobalPos> updated = new HashSet<>(structureReferences);
        updated.add(structureReference);
        return new GeographicEntity(id, dimension, classification, type, name, geometry, lifecycleState, Set.copyOf(updated));
    }
}
