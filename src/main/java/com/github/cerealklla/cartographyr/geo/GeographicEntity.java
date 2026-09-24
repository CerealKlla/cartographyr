package com.github.cerealklla.cartographyr.geo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A geographic entity: something Cartographyr tracks the identity, location, and lifecycle of.
 * Immutable — updates go through the "with" methods below, producing a new instance that the
 * storage layer replaces the old one with (see {@code CartographySavedData#updateEntity}).
 *
 * This is the trimmed shape so far: no source or extension data yet (design document Section 3
 * lists the full field set).
 */
public record GeographicEntity(
        EntityId id,
        ResourceKey<Level> dimension,
        Classification classification,
        EntityType type,
        Identifier layerId,
        Optional<String> name,
        Geometry geometry,
        LifecycleState lifecycleState,
        Set<GlobalPos> structureReferences,
        Set<Characteristic> characteristics,
        Set<Amenity> amenities,
        List<AlternateName> alternateNames,
        List<HistoricalFact> historicalFacts
) {
    public static final Codec<GeographicEntity> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityId.CODEC.fieldOf("id").forGetter(GeographicEntity::id),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(GeographicEntity::dimension),
            Classification.CODEC.fieldOf("classification").forGetter(GeographicEntity::classification),
            EntityType.CODEC.fieldOf("type").forGetter(GeographicEntity::type),
            Identifier.CODEC.fieldOf("layer_id").forGetter(GeographicEntity::layerId),
            Codec.STRING.optionalFieldOf("name").forGetter(GeographicEntity::name),
            Geometry.CODEC.fieldOf("geometry").forGetter(GeographicEntity::geometry),
            LifecycleState.CODEC.fieldOf("lifecycle_state").forGetter(GeographicEntity::lifecycleState),
            Codec.list(GlobalPos.CODEC).xmap(Set::copyOf, List::copyOf)
                    .fieldOf("structure_references").forGetter(GeographicEntity::structureReferences),
            Codec.list(Characteristic.CODEC).xmap(Set::copyOf, List::copyOf)
                    .fieldOf("characteristics").forGetter(GeographicEntity::characteristics),
            Codec.list(Amenity.CODEC).xmap(Set::copyOf, List::copyOf)
                    .fieldOf("amenities").forGetter(GeographicEntity::amenities),
            Codec.list(AlternateName.CODEC).fieldOf("alternate_names").forGetter(GeographicEntity::alternateNames),
            Codec.list(HistoricalFact.CODEC).fieldOf("historical_facts").forGetter(GeographicEntity::historicalFacts)
    ).apply(i, GeographicEntity::new));

    public static GeographicEntity create(EntityId id, EntityDefinition definition) {
        return new GeographicEntity(
                id,
                definition.dimension(),
                definition.classification(),
                definition.type(),
                definition.layerId(),
                definition.name(),
                definition.geometry(),
                definition.lifecycleState(),
                Set.of(),
                Set.of(),
                Set.of(),
                List.of(),
                List.of()
        );
    }

    public GeographicEntity withLifecycleState(LifecycleState newState) {
        return new GeographicEntity(id, dimension, classification, type, layerId, name, geometry, newState, structureReferences, characteristics, amenities, alternateNames, historicalFacts);
    }

    public GeographicEntity withGeometry(Geometry newGeometry) {
        return new GeographicEntity(id, dimension, classification, type, layerId, name, newGeometry, lifecycleState, structureReferences, characteristics, amenities, alternateNames, historicalFacts);
    }

    public GeographicEntity withName(Optional<String> newName) {
        return new GeographicEntity(id, dimension, classification, type, layerId, newName, geometry, lifecycleState, structureReferences, characteristics, amenities, alternateNames, historicalFacts);
    }

    public GeographicEntity withAddedStructureReference(GlobalPos structureReference) {
        Set<GlobalPos> updated = new HashSet<>(structureReferences);
        updated.add(structureReference);
        return new GeographicEntity(id, dimension, classification, type, layerId, name, geometry, lifecycleState, Set.copyOf(updated), characteristics, amenities, alternateNames, historicalFacts);
    }

    public GeographicEntity withAddedCharacteristic(Characteristic characteristic) {
        Set<Characteristic> updated = new HashSet<>(characteristics);
        updated.add(characteristic);
        return new GeographicEntity(id, dimension, classification, type, layerId, name, geometry, lifecycleState, structureReferences, Set.copyOf(updated), amenities, alternateNames, historicalFacts);
    }

    public GeographicEntity withRemovedCharacteristic(Characteristic characteristic) {
        Set<Characteristic> updated = new HashSet<>(characteristics);
        updated.remove(characteristic);
        return new GeographicEntity(id, dimension, classification, type, layerId, name, geometry, lifecycleState, structureReferences, Set.copyOf(updated), amenities, alternateNames, historicalFacts);
    }

    public GeographicEntity withAddedAmenity(Amenity amenity) {
        Set<Amenity> updated = new HashSet<>(amenities);
        updated.add(amenity);
        return new GeographicEntity(id, dimension, classification, type, layerId, name, geometry, lifecycleState, structureReferences, characteristics, Set.copyOf(updated), alternateNames, historicalFacts);
    }

    public GeographicEntity withRemovedAmenity(Amenity amenity) {
        Set<Amenity> updated = new HashSet<>(amenities);
        updated.remove(amenity);
        return new GeographicEntity(id, dimension, classification, type, layerId, name, geometry, lifecycleState, structureReferences, characteristics, Set.copyOf(updated), alternateNames, historicalFacts);
    }

    public GeographicEntity withAddedAlternateName(AlternateName alternateName) {
        List<AlternateName> updated = new ArrayList<>(alternateNames);
        updated.add(alternateName);
        return new GeographicEntity(id, dimension, classification, type, layerId, name, geometry, lifecycleState, structureReferences, characteristics, amenities, List.copyOf(updated), historicalFacts);
    }

    /** Keeps {@link #historicalFacts} sorted by {@link HistoricalFact#gameTime()} on every insert,
     * since facts can be discovered out of chronological order but should still read back in order. */
    public GeographicEntity withAddedHistoricalFact(HistoricalFact fact) {
        List<HistoricalFact> updated = new ArrayList<>(historicalFacts);
        updated.add(fact);
        updated.sort(Comparator.comparingLong(HistoricalFact::gameTime));
        return new GeographicEntity(id, dimension, classification, type, layerId, name, geometry, lifecycleState, structureReferences, characteristics, amenities, alternateNames, List.copyOf(updated));
    }
}
