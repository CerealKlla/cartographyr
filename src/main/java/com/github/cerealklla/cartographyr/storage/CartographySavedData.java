package com.github.cerealklla.cartographyr.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.github.cerealklla.cartographyr.CartographyrMod;
import com.github.cerealklla.cartographyr.geo.AlternateName;
import com.github.cerealklla.cartographyr.geo.Amenity;
import com.github.cerealklla.cartographyr.geo.Characteristic;
import com.github.cerealklla.cartographyr.geo.Classification;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityId;
import com.github.cerealklla.cartographyr.geo.EntityNames;
import com.github.cerealklla.cartographyr.geo.EntityType;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.geo.Geometry;
import com.github.cerealklla.cartographyr.geo.HistoricalFact;
import com.github.cerealklla.cartographyr.geo.LifecycleState;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Internal world-level (save-wide, not per-dimension) persistent store for all geographic
 * entities. This is NOT the intended integration point for other mods — see
 * {@code com.github.cerealklla.cartographyr.api.Cartography}, the stable public facade this class
 * exists behind. Every mutator here is public only because Java has no cross-package "friend"
 * access; each carries an {@code @apiNote} pointing back to the facade.
 */
public final class CartographySavedData extends SavedData {

    public static final int SCHEMA_VERSION = 1;

    public static final SavedDataType<CartographySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CartographyrMod.MODID, "geographic_data"),
            CartographySavedData::new,
            codec()
    );

    private int schemaVersion;
    private long nextEntityId;
    private final Map<EntityId, GeographicEntity> entities;
    private final SpatialIndex spatialIndex = new SpatialIndex();
    // Reverse lookup (design doc Section 7.3). Not persisted, same as spatialIndex — rebuilt by
    // reindex() from entities' own structureReferences, which are the actual source of truth.
    private final Map<GlobalPos, EntityId> structureIndex = new HashMap<>();

    // Package-private (not private) so the test suite in this same package can construct a fresh
    // instance directly without going through the SavedDataType machinery.
    CartographySavedData() {
        this(SCHEMA_VERSION, 1L, new HashMap<>());
    }

    private CartographySavedData(int schemaVersion, long nextEntityId, Map<EntityId, GeographicEntity> entities) {
        this.schemaVersion = schemaVersion;
        this.nextEntityId = nextEntityId;
        this.entities = entities;
        reindex();
    }

    private static Codec<CartographySavedData> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("schema_version").forGetter(d -> d.schemaVersion),
                Codec.LONG.fieldOf("next_entity_id").forGetter(d -> d.nextEntityId),
                Codec.unboundedMap(EntityId.CODEC, GeographicEntity.CODEC).fieldOf("entities").forGetter(d -> d.entities)
        ).apply(i, (schemaVersion, nextEntityId, entities) ->
                new CartographySavedData(schemaVersion, nextEntityId, new HashMap<>(entities))));
    }

    /** Rebuilds both secondary indices from {@link #entities}, the actual source of truth. */
    private void reindex() {
        spatialIndex.rebuild(entities.values());
        structureIndex.clear();
        for (GeographicEntity entity : entities.values()) {
            for (GlobalPos structureReference : entity.structureReferences()) {
                structureIndex.put(structureReference, entity.id());
            }
        }
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.createEntity} instead. */
    public GeographicEntity createEntity(EntityDefinition definition) {
        EntityId id = new EntityId(nextEntityId++);
        GeographicEntity entity = GeographicEntity.create(id, definition);
        entities.put(id, entity);
        reindex();
        setDirty();
        return entity;
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getEntity} instead. */
    public Optional<GeographicEntity> getEntity(EntityId id) {
        return Optional.ofNullable(entities.get(id));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.updateEntity} instead. */
    public Optional<GeographicEntity> updateEntity(EntityId id, UnaryOperator<GeographicEntity> update) {
        GeographicEntity current = entities.get(id);
        if (current == null) {
            return Optional.empty();
        }
        GeographicEntity updated = update.apply(current);
        entities.put(id, updated);
        reindex();
        setDirty();
        return Optional.of(updated);
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.retireEntity} instead. */
    public Optional<GeographicEntity> retireEntity(EntityId id) {
        return updateEntity(id, entity -> entity.withLifecycleState(LifecycleState.RETIRED));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getEntitiesAt} instead. */
    public Set<GeographicEntity> getEntitiesAt(ResourceKey<Level> dimension, int x, int z) {
        Set<EntityId> candidates = spatialIndex.candidatesAt(dimension, x, z);
        Set<GeographicEntity> result = new HashSet<>();
        for (EntityId candidate : candidates) {
            GeographicEntity entity = entities.get(candidate);
            if (entity != null && entity.geometry().contains(x, z)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * @apiNote Not the intended integration point — use {@code Cartography.associateStructure} instead.
     * Fails (returns empty) rather than reassociating if {@code structureReference} already points
     * at a different entity — otherwise the rebuilt reverse index would silently let whichever
     * entity is iterated last win, with no error.
     */
    public Optional<GeographicEntity> associateStructure(EntityId id, GlobalPos structureReference) {
        Optional<EntityId> existingOwner = getEntityForStructure(structureReference);
        if (existingOwner.isPresent() && !existingOwner.get().equals(id)) {
            return Optional.empty();
        }
        return updateEntity(id, entity -> entity.withAddedStructureReference(structureReference));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getAssociatedStructures} instead. */
    public Set<GlobalPos> getAssociatedStructures(EntityId id) {
        GeographicEntity entity = entities.get(id);
        return entity == null ? Set.of() : entity.structureReferences();
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getEntityForStructure} instead. */
    public Optional<EntityId> getEntityForStructure(GlobalPos structureReference) {
        return Optional.ofNullable(structureIndex.get(structureReference));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.addCharacteristic} instead. */
    public Optional<GeographicEntity> addCharacteristic(EntityId id, Characteristic characteristic) {
        return updateEntity(id, entity -> entity.withAddedCharacteristic(characteristic));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.removeCharacteristic} instead. */
    public Optional<GeographicEntity> removeCharacteristic(EntityId id, Characteristic characteristic) {
        return updateEntity(id, entity -> entity.withRemovedCharacteristic(characteristic));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getCharacteristics} instead. */
    public Set<Characteristic> getCharacteristics(EntityId id) {
        GeographicEntity entity = entities.get(id);
        return entity == null ? Set.of() : entity.characteristics();
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.addAmenity} instead. */
    public Optional<GeographicEntity> addAmenity(EntityId id, Amenity amenity) {
        return updateEntity(id, entity -> entity.withAddedAmenity(amenity));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.removeAmenity} instead. */
    public Optional<GeographicEntity> removeAmenity(EntityId id, Amenity amenity) {
        return updateEntity(id, entity -> entity.withRemovedAmenity(amenity));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getAmenities} instead. */
    public Set<Amenity> getAmenities(EntityId id) {
        GeographicEntity entity = entities.get(id);
        return entity == null ? Set.of() : entity.amenities();
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.setName} instead. */
    public Optional<GeographicEntity> setName(EntityId id, String name) {
        return updateEntity(id, entity -> entity.withName(Optional.of(name)));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.addAlternateName} instead. */
    public Optional<GeographicEntity> addAlternateName(EntityId id, String name, Optional<String> metadata) {
        return updateEntity(id, entity -> entity.withAddedAlternateName(new AlternateName(name, metadata)));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getNames} instead. */
    public Optional<EntityNames> getNames(EntityId id) {
        GeographicEntity entity = entities.get(id);
        return entity == null ? Optional.empty() : Optional.of(new EntityNames(entity.name(), entity.alternateNames()));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.setDesignation} instead. */
    public Optional<GeographicEntity> setDesignation(EntityId id, String designation) {
        return updateEntity(id, entity -> entity.withDesignation(Optional.of(designation)));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getDesignation} instead. */
    public Optional<String> getDesignation(EntityId id) {
        GeographicEntity entity = entities.get(id);
        return entity == null ? Optional.empty() : entity.designation();
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.setSpecialStatus} instead. */
    public Optional<GeographicEntity> setSpecialStatus(EntityId id, String specialStatus) {
        return updateEntity(id, entity -> entity.withSpecialStatus(Optional.of(specialStatus)));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getSpecialStatus} instead. */
    public Optional<String> getSpecialStatus(EntityId id) {
        GeographicEntity entity = entities.get(id);
        return entity == null ? Optional.empty() : entity.specialStatus();
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.addHistoricalFact} instead. */
    public Optional<GeographicEntity> addHistoricalFact(EntityId id, HistoricalFact fact) {
        return updateEntity(id, entity -> entity.withAddedHistoricalFact(fact));
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getHistoricalFacts} instead. */
    public List<HistoricalFact> getHistoricalFacts(EntityId id) {
        GeographicEntity entity = entities.get(id);
        return entity == null ? List.of() : entity.historicalFacts();
    }

    /** @apiNote Not the intended integration point — use {@code Cartography.getNaturalRegionAt} instead. */
    public Optional<GeographicEntity> getNaturalRegionAt(ResourceKey<Level> dimension, int x, int z) {
        return getEntitiesAt(dimension, x, z).stream()
                .filter(entity -> entity.classification().equals(Classification.NATURAL))
                .findFirst();
    }

    /**
     * @apiNote Not the intended integration point — use {@code Cartography.findNaturalRegions} instead.
     * Simplified from the design doc's broader "search criteria such as type, name, area, dimension,
     * or bounds" down to just dimension + type — the two concretely useful ones for now. O(n) scan
     * over all entities, no dedicated index, same as the characteristics/amenities getters.
     */
    public Set<GeographicEntity> findNaturalRegions(ResourceKey<Level> dimension, EntityType type) {
        Set<GeographicEntity> result = new HashSet<>();
        for (GeographicEntity entity : entities.values()) {
            if (entity.classification().equals(Classification.NATURAL)
                    && entity.dimension().equals(dimension)
                    && entity.type().equals(type)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * @apiNote Not the intended integration point — use {@code Cartography.findEntities} instead.
     * Same O(n) scan as {@link #findNaturalRegions}, but by classification alone (no type filter) --
     * e.g. every settlement in a dimension, regardless of natural/constructed type.
     */
    public Set<GeographicEntity> findEntities(ResourceKey<Level> dimension, Classification classification) {
        Set<GeographicEntity> result = new HashSet<>();
        for (GeographicEntity entity : entities.values()) {
            if (entity.classification().equals(classification) && entity.dimension().equals(dimension)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * @apiNote Not the intended integration point — use {@code Cartography.getRegionBounds} instead.
     * Generic over any entity, not just natural ones — the underlying operation doesn't need the
     * restriction the design doc's wording implies.
     */
    public Optional<Geometry> getRegionBounds(EntityId id) {
        return getEntity(id).map(GeographicEntity::geometry);
    }
}
