package com.github.cerealklla.cartographyr.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.cerealklla.cartographyr.geo.EntityId;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Secondary, in-memory-only index from dimension + chunk cell to candidate entity IDs. Never
 * persisted — rebuilt from the authoritative entity map on load and after every mutation, since
 * incremental add/remove would need an entity's previous geometry to know what to evict from.
 *
 * Cells hold candidates only; callers must still check the real geometry for exact containment.
 */
public final class SpatialIndex {

    private final Map<ResourceKey<Level>, Map<Long, Set<EntityId>>> cells = new HashMap<>();

    public void rebuild(Collection<GeographicEntity> entities) {
        cells.clear();
        for (GeographicEntity entity : entities) {
            insert(entity);
        }
    }

    private void insert(GeographicEntity entity) {
        ChunkPos min = entity.geometry().minChunk();
        ChunkPos max = entity.geometry().maxChunk();
        Map<Long, Set<EntityId>> dimensionCells = cells.computeIfAbsent(entity.dimension(), d -> new HashMap<>());
        for (int cx = min.x(); cx <= max.x(); cx++) {
            for (int cz = min.z(); cz <= max.z(); cz++) {
                dimensionCells.computeIfAbsent(ChunkPos.pack(cx, cz), c -> new HashSet<>()).add(entity.id());
            }
        }
    }

    public Set<EntityId> candidatesAt(ResourceKey<Level> dimension, int x, int z) {
        Map<Long, Set<EntityId>> dimensionCells = cells.get(dimension);
        if (dimensionCells == null) {
            return Set.of();
        }
        Set<EntityId> candidates = dimensionCells.get(ChunkPos.pack(x >> 4, z >> 4));
        return candidates == null ? Set.of() : candidates;
    }
}
