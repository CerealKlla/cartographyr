package com.github.cerealklla.cartographyr.settlement;

import java.util.Map;
import java.util.Optional;

import com.github.cerealklla.cartographyr.api.Cartography;
import com.github.cerealklla.cartographyr.geo.EntityId;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Detects naturally-generated villages the moment their chunk loads and hands them to {@link
 * SettlementDiscovery}. Unlike Natural Geography (which needs an external caller providing a
 * player's position -- see {@code natural} package, driven by Lyfe's location tracker), this is
 * entirely world-gen-driven and self-triggering: Cartographyr registers this listener on its own
 * event bus and needs nothing external to fire it. See decisions.md, 2026-09-24, for the "world-gen
 * driven detection is Cartographyr's own job; player/social-driven detection stays push-based"
 * distinction this is built on.
 */
public final class SettlementListener {

    private SettlementListener() {
    }

    /**
     * Fires on every chunk load, not just first-ever generation -- deliberately not gated on
     * {@code event.isNewChunk()}. The idempotency check below (via {@code getEntityForStructure})
     * is what makes this safe to run unconditionally, and it has a real benefit: a world with
     * villages that generated before this feature existed gets them discovered lazily as the
     * player revisits those chunks, with no separate migration/backfill code needed.
     */
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Map<Structure, StructureStart> starts = event.getChunk().getAllStarts();
        if (starts.isEmpty()) {
            return;
        }

        var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        for (Map.Entry<Structure, StructureStart> entry : starts.entrySet()) {
            StructureStart start = entry.getValue();
            if (!start.isValid()) {
                continue;
            }
            if (!structureRegistry.wrapAsHolder(entry.getKey()).is(StructureTags.VILLAGE)) {
                continue;
            }

            GlobalPos structureReference = new GlobalPos(level.dimension(), start.getChunkPos().getWorldPosition());
            Optional<EntityId> existing = Cartography.getEntityForStructure(level, structureReference);
            if (existing.isPresent()) {
                continue;
            }

            SettlementDiscovery.discover(level, start);
        }
    }
}
