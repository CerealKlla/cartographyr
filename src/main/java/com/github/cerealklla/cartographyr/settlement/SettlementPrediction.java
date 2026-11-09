package com.github.cerealklla.cartographyr.settlement;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.github.cerealklla.cartographyr.CartographyrMod;
import com.github.cerealklla.cartographyr.api.Cartography;
import com.github.cerealklla.cartographyr.geo.Classification;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityType;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.geo.Geometry;
import com.github.cerealklla.cartographyr.geo.Layer;
import com.github.cerealklla.cartographyr.geo.LifecycleState;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;

/**
 * Lets a consumer (Myssions' unborn-town quests) ask "is there a town near here" and get a real,
 * nameable answer even before any chunk in that area has ever generated -- explicit user spec,
 * 2026-10-11: use vanilla's own world-gen logic to predict where a town *would* generate and
 * pre-name that spot, rather than forcing real chunk generation (a real server-tick lag risk the
 * user explicitly rejected). {@link ServerLevel#findNearestMapStructure} resolves a village's real
 * placement purely from structure seed math (the same lookup vanilla's own /locate command uses) --
 * it does not generate or load the chunk itself.
 *
 * <p>A village predicted this way is persisted immediately as a {@link LifecycleState#PLANNED}
 * entity with a real name, so every existing consumer (quests, the minimap, Lyfe's knowledge
 * system) can treat it exactly like a real settlement. {@link SettlementListener} is responsible
 * for promoting it to {@link LifecycleState#REALIZED} once the real structure actually generates.
 */
public final class SettlementPrediction {

    private static final Random RANDOM = new Random();

    private SettlementPrediction() {
    }

    /**
     * Returns a real settlement (already-discovered or freshly predicted) within {@code
     * radiusBlocks} of {@code center}, at random if more than one qualifies. Empty only if neither
     * an existing entity nor vanilla's own placement lookup finds anything within radius.
     */
    public static Optional<GeographicEntity> findOrPredictSettlementNear(ServerLevel level, BlockPos center, double radiusBlocks) {
        List<GeographicEntity> known = Cartography.findEntities(level, Classification.CONSTRUCTED, Layer.SETTLEMENT_ID).stream()
                .filter(entity -> entity.lifecycleState() == LifecycleState.REALIZED || entity.lifecycleState() == LifecycleState.PLANNED)
                .filter(entity -> withinRadius(entity, center, radiusBlocks))
                .toList();
        if (!known.isEmpty()) {
            return Optional.of(known.get(RANDOM.nextInt(known.size())));
        }

        BlockPos predicted = level.findNearestMapStructure(StructureTags.VILLAGE, center, (int) radiusBlocks, false);
        if (predicted == null) {
            return Optional.empty();
        }

        String name = SettlementNaming.pickName(Optional.empty(), RANDOM);
        GeographicEntity created = Cartography.createEntity(level, new EntityDefinition(
                level.dimension(),
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Layer.SETTLEMENT_ID,
                Optional.of(name),
                new Geometry.Point(predicted.getX(), predicted.getZ()),
                LifecycleState.PLANNED,
                Optional.empty()
        ));
        CartographyrMod.LOGGER.info(
                "Settlement prediction: planned '{}' ({}) at {} -- not yet generated, seed-predicted only",
                name, created.id(), predicted);
        return Optional.of(created);
    }

    /**
     * Like {@link #findOrPredictSettlementNear}, but always runs the real seed-prediction lookup at
     * {@code searchCenter} directly instead of first trying a "something's already known within
     * radius" shortcut -- added 2026-11-06 for Arryval's starting-settlement picker, which needs to
     * discover several genuinely distinct nearby villages by searching from several different
     * points. {@link #findOrPredictSettlementNear}'s own "known" shortcut picks randomly among
     * anything already registered within radius, which would keep returning the very first village
     * ever seeded for every later search center that happens to still be within its own radius of
     * it -- exactly the opposite of "find a different one over here instead." Still dedupes against
     * an already-registered entity at the exact same predicted position (two search centers
     * legitimately resolving the same real village via the same deterministic seed math), it just
     * never substitutes a different, merely-nearby one.
     */
    public static Optional<GeographicEntity> predictSettlementAt(ServerLevel level, BlockPos searchCenter, double radiusBlocks) {
        BlockPos predicted = level.findNearestMapStructure(StructureTags.VILLAGE, searchCenter, (int) radiusBlocks, false);
        if (predicted == null) {
            return Optional.empty();
        }
        Optional<GeographicEntity> existing = Cartography.findEntities(level, Classification.CONSTRUCTED, Layer.SETTLEMENT_ID).stream()
                .filter(entity -> entity.lifecycleState() == LifecycleState.REALIZED || entity.lifecycleState() == LifecycleState.PLANNED)
                .filter(entity -> entity.geometry().centerBlockX() == predicted.getX() && entity.geometry().centerBlockZ() == predicted.getZ())
                .findFirst();
        if (existing.isPresent()) {
            return existing;
        }
        String name = SettlementNaming.pickName(Optional.empty(), RANDOM);
        GeographicEntity created = Cartography.createEntity(level, new EntityDefinition(
                level.dimension(),
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Layer.SETTLEMENT_ID,
                Optional.of(name),
                new Geometry.Point(predicted.getX(), predicted.getZ()),
                LifecycleState.PLANNED,
                Optional.empty()
        ));
        CartographyrMod.LOGGER.info(
                "Settlement prediction: planned '{}' ({}) at {} -- not yet generated, seed-predicted only",
                name, created.id(), predicted);
        return Optional.of(created);
    }

    private static boolean withinRadius(GeographicEntity entity, BlockPos center, double radiusBlocks) {
        double dx = entity.geometry().centerBlockX() - center.getX();
        double dz = entity.geometry().centerBlockZ() - center.getZ();
        return dx * dx + dz * dz <= radiusBlocks * radiusBlocks;
    }
}
