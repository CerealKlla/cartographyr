package com.github.cerealklla.cartographyr.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.github.cerealklla.cartographyr.CartographyrMod;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityId;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
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
}
