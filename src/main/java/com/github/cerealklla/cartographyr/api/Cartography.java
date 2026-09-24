package com.github.cerealklla.cartographyr.api;

import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityId;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.storage.CartographySavedData;

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
}
