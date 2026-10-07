package com.github.cerealklla.cartographyr.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.github.cerealklla.cartographyr.geo.Geometry;

import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Secondary, in-memory-only chunk-cell candidate index over box locations -- same shape and same
 * "candidates only, exact containment is the caller's job" contract as {@link SpatialIndex}, just
 * keyed by {@link UUID} instead of {@code EntityId}. Never persisted; rebuilt from {@code
 * CartographySavedData}'s authoritative {@code boxLocations} map on load and after every mutation.
 */
public final class BoxIndex {

    private final Map<ResourceKey<Level>, Map<Long, Set<UUID>>> cells = new HashMap<>();

    public void rebuild(Map<UUID, GlobalPos> boxes) {
        cells.clear();
        for (Map.Entry<UUID, GlobalPos> entry : boxes.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void put(UUID id, GlobalPos pos) {
        Map<Long, Set<UUID>> dimensionCells = cells.computeIfAbsent(pos.dimension(), d -> new HashMap<>());
        long cell = ChunkPos.pack(pos.pos().getX() >> 4, pos.pos().getZ() >> 4);
        dimensionCells.computeIfAbsent(cell, c -> new HashSet<>()).add(id);
    }

    public void remove(UUID id) {
        for (Map<Long, Set<UUID>> dimensionCells : cells.values()) {
            for (Set<UUID> candidates : dimensionCells.values()) {
                candidates.remove(id);
            }
        }
    }

    /** Every box id whose chunk cell falls within {@code geometry}'s bounding chunk range -- candidates only. */
    public Set<UUID> candidatesInRange(ResourceKey<Level> dimension, Geometry geometry) {
        Map<Long, Set<UUID>> dimensionCells = cells.get(dimension);
        if (dimensionCells == null) {
            return Set.of();
        }
        ChunkPos min = geometry.minChunk();
        ChunkPos max = geometry.maxChunk();
        Set<UUID> result = new HashSet<>();
        for (int cx = min.x(); cx <= max.x(); cx++) {
            for (int cz = min.z(); cz <= max.z(); cz++) {
                Set<UUID> candidates = dimensionCells.get(ChunkPos.pack(cx, cz));
                if (candidates != null) {
                    result.addAll(candidates);
                }
            }
        }
        return result;
    }
}
