package com.github.cerealklla.cartographyr.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import com.github.cerealklla.cartographyr.geo.Amenity;
import com.github.cerealklla.cartographyr.geo.Characteristic;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityId;
import com.github.cerealklla.cartographyr.geo.EntityNames;
import com.github.cerealklla.cartographyr.geo.EntityType;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.geo.Geometry;
import com.github.cerealklla.cartographyr.geo.HistoricalFact;
import com.github.cerealklla.cartographyr.natural.NaturalRegionDiscovery;
import com.github.cerealklla.cartographyr.storage.CartographySavedData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * The stable public entry point for Cartographyr's geographic data. Other mods should call these
 * methods rather than reach into storage internals directly, so internal storage/index changes
 * never require dependents to rewrite their integration.
 */
public final class Cartography {

    private Cartography() {
    }

    // Deliberately MinecraftServer, never ServerLevel#getDataStorage() — that accessor is
    // per-dimension in this Minecraft version (DimensionDataStorage was renamed to
    // SavedDataStorage and made dimension-scoped), not the save-wide store. Using it here would
    // silently give each dimension its own disconnected copy of the data instead of one
    // world-level store.
    private static CartographySavedData data(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(CartographySavedData.TYPE);
    }

    public static GeographicEntity createEntity(MinecraftServer server, EntityDefinition definition) {
        return data(server).createEntity(definition);
    }

    public static GeographicEntity createEntity(ServerLevel level, EntityDefinition definition) {
        return createEntity(level.getServer(), definition);
    }

    public static Optional<GeographicEntity> getEntity(MinecraftServer server, EntityId id) {
        return data(server).getEntity(id);
    }

    public static Optional<GeographicEntity> getEntity(ServerLevel level, EntityId id) {
        return getEntity(level.getServer(), id);
    }

    public static Optional<GeographicEntity> updateEntity(MinecraftServer server, EntityId id, UnaryOperator<GeographicEntity> update) {
        return data(server).updateEntity(id, update);
    }

    public static Optional<GeographicEntity> updateEntity(ServerLevel level, EntityId id, UnaryOperator<GeographicEntity> update) {
        return updateEntity(level.getServer(), id, update);
    }

    public static Optional<GeographicEntity> retireEntity(MinecraftServer server, EntityId id) {
        return data(server).retireEntity(id);
    }

    public static Optional<GeographicEntity> retireEntity(ServerLevel level, EntityId id) {
        return retireEntity(level.getServer(), id);
    }

    public static Set<GeographicEntity> getEntitiesAt(MinecraftServer server, ResourceKey<Level> dimension, int x, int z) {
        return data(server).getEntitiesAt(dimension, x, z);
    }

    public static Set<GeographicEntity> getEntitiesAt(ServerLevel level, int x, int z) {
        return getEntitiesAt(level.getServer(), level.dimension(), x, z);
    }

    /**
     * Links a generated structure's location to a geographic entity — typically the step between
     * creating a PLANNED entity and marking it REALIZED (design doc Section 4). Fails (returns
     * empty) if {@code structureReference} is already associated with a different entity.
     */
    public static Optional<GeographicEntity> associateStructure(MinecraftServer server, EntityId id, GlobalPos structureReference) {
        return data(server).associateStructure(id, structureReference);
    }

    public static Optional<GeographicEntity> associateStructure(ServerLevel level, EntityId id, GlobalPos structureReference) {
        return associateStructure(level.getServer(), id, structureReference);
    }

    public static Set<GlobalPos> getAssociatedStructures(MinecraftServer server, EntityId id) {
        return data(server).getAssociatedStructures(id);
    }

    public static Set<GlobalPos> getAssociatedStructures(ServerLevel level, EntityId id) {
        return getAssociatedStructures(level.getServer(), id);
    }

    public static Optional<EntityId> getEntityForStructure(MinecraftServer server, GlobalPos structureReference) {
        return data(server).getEntityForStructure(structureReference);
    }

    public static Optional<EntityId> getEntityForStructure(ServerLevel level, GlobalPos structureReference) {
        return getEntityForStructure(level.getServer(), structureReference);
    }

    /**
     * Records an evolving economic/functional specialty on an entity (design doc Section 5.4).
     * Cartography just records this — deciding *why* an entity gained a characteristic is
     * Economy/settlement logic's job, done elsewhere before calling this.
     */
    public static Optional<GeographicEntity> addCharacteristic(MinecraftServer server, EntityId id, Characteristic characteristic) {
        return data(server).addCharacteristic(id, characteristic);
    }

    public static Optional<GeographicEntity> addCharacteristic(ServerLevel level, EntityId id, Characteristic characteristic) {
        return addCharacteristic(level.getServer(), id, characteristic);
    }

    public static Optional<GeographicEntity> removeCharacteristic(MinecraftServer server, EntityId id, Characteristic characteristic) {
        return data(server).removeCharacteristic(id, characteristic);
    }

    public static Optional<GeographicEntity> removeCharacteristic(ServerLevel level, EntityId id, Characteristic characteristic) {
        return removeCharacteristic(level.getServer(), id, characteristic);
    }

    public static Set<Characteristic> getCharacteristics(MinecraftServer server, EntityId id) {
        return data(server).getCharacteristics(id);
    }

    public static Set<Characteristic> getCharacteristics(ServerLevel level, EntityId id) {
        return getCharacteristics(level.getServer(), id);
    }

    /**
     * Records a physical/infrastructural feature on an entity (design doc Section 5.5), e.g. a
     * market or a sawmill. World Builder can use amenities as generation inputs; Cartography just
     * records their geographic existence.
     */
    public static Optional<GeographicEntity> addAmenity(MinecraftServer server, EntityId id, Amenity amenity) {
        return data(server).addAmenity(id, amenity);
    }

    public static Optional<GeographicEntity> addAmenity(ServerLevel level, EntityId id, Amenity amenity) {
        return addAmenity(level.getServer(), id, amenity);
    }

    public static Optional<GeographicEntity> removeAmenity(MinecraftServer server, EntityId id, Amenity amenity) {
        return data(server).removeAmenity(id, amenity);
    }

    public static Optional<GeographicEntity> removeAmenity(ServerLevel level, EntityId id, Amenity amenity) {
        return removeAmenity(level.getServer(), id, amenity);
    }

    public static Set<Amenity> getAmenities(MinecraftServer server, EntityId id) {
        return data(server).getAmenities(id);
    }

    public static Set<Amenity> getAmenities(ServerLevel level, EntityId id) {
        return getAmenities(level.getServer(), id);
    }

    /**
     * Sets an entity's current authoritative name (design doc Section 5.3). Names are descriptive
     * data, never identity — {@link EntityId} is what other mods should actually reference.
     */
    public static Optional<GeographicEntity> setName(MinecraftServer server, EntityId id, String name) {
        return data(server).setName(id, name);
    }

    public static Optional<GeographicEntity> setName(ServerLevel level, EntityId id, String name) {
        return setName(level.getServer(), id, name);
    }

    /** Adds a historical/alternate name without replacing the current one. */
    public static Optional<GeographicEntity> addAlternateName(MinecraftServer server, EntityId id, String name, Optional<String> metadata) {
        return data(server).addAlternateName(id, name, metadata);
    }

    public static Optional<GeographicEntity> addAlternateName(ServerLevel level, EntityId id, String name, Optional<String> metadata) {
        return addAlternateName(level.getServer(), id, name, metadata);
    }

    public static Optional<EntityNames> getNames(MinecraftServer server, EntityId id) {
        return data(server).getNames(id);
    }

    public static Optional<EntityNames> getNames(ServerLevel level, EntityId id) {
        return getNames(level.getServer(), id);
    }

    /**
     * Records a descriptive historical fact on an entity (design doc Section 5.6). Cartography
     * records these; it does not simulate or generate history itself. The entity's fact list
     * stays sorted by {@link HistoricalFact#gameTime()}, so facts discovered out of order still
     * read back chronologically.
     */
    public static Optional<GeographicEntity> addHistoricalFact(MinecraftServer server, EntityId id, HistoricalFact fact) {
        return data(server).addHistoricalFact(id, fact);
    }

    public static Optional<GeographicEntity> addHistoricalFact(ServerLevel level, EntityId id, HistoricalFact fact) {
        return addHistoricalFact(level.getServer(), id, fact);
    }

    public static List<HistoricalFact> getHistoricalFacts(MinecraftServer server, EntityId id) {
        return data(server).getHistoricalFacts(id);
    }

    public static List<HistoricalFact> getHistoricalFacts(ServerLevel level, EntityId id) {
        return getHistoricalFacts(level.getServer(), id);
    }

    /** The natural (non-constructed) entity at a point, if one's been discovered there (design doc Section 5.8). */
    public static Optional<GeographicEntity> getNaturalRegionAt(MinecraftServer server, ResourceKey<Level> dimension, int x, int z) {
        return data(server).getNaturalRegionAt(dimension, x, z);
    }

    public static Optional<GeographicEntity> getNaturalRegionAt(ServerLevel level, int x, int z) {
        return getNaturalRegionAt(level.getServer(), level.dimension(), x, z);
    }

    public static Set<GeographicEntity> findNaturalRegions(MinecraftServer server, ResourceKey<Level> dimension, EntityType type) {
        return data(server).findNaturalRegions(dimension, type);
    }

    public static Set<GeographicEntity> findNaturalRegions(ServerLevel level, EntityType type) {
        return findNaturalRegions(level.getServer(), level.dimension(), type);
    }

    public static Optional<Geometry> getRegionBounds(MinecraftServer server, EntityId id) {
        return data(server).getRegionBounds(id);
    }

    public static Optional<Geometry> getRegionBounds(ServerLevel level, EntityId id) {
        return getRegionBounds(level.getServer(), id);
    }

    /**
     * Attempts to discover a new natural region at {@code pos} (design doc Section 5.8), sampling
     * live biome data — unlike {@link #getNaturalRegionAt}, this can create a new entity as a side
     * effect. Callers should check {@link #getEntitiesAt} first and only call this when nothing is
     * already there; this doesn't check for existing entities itself, matching {@code
     * NaturalRegionDiscovery.discover}'s own contract.
     *
     * <p>No {@code MinecraftServer} overload: unlike everything else here, discovery samples biome
     * data at a specific Y as well as X/Z (caves can differ from the surface), so a bare
     * dimension+x+z signature would need a fabricated Y. Callers already have a {@link ServerLevel}
     * and {@link BlockPos} on hand in every real use case (e.g. a player's current position).
     *
     * @apiNote Needs a live level to sample biomes, so (unlike everything else in this facade) this
     * can't be unit tested — see {@code NaturalRegionDiscovery}'s own notes.
     */
    public static Optional<GeographicEntity> discoverNaturalRegion(ServerLevel level, BlockPos pos) {
        return NaturalRegionDiscovery.discover(level, pos);
    }
}
