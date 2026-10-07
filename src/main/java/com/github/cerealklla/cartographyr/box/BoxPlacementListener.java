package com.github.cerealklla.cartographyr.box;

import java.util.UUID;

import com.github.cerealklla.cartographyr.api.Cartography;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Mints a durable {@link UUID} for every storage box (any {@code BlockEntity} implementing
 * vanilla's {@link Container}) the instant it's placed, and purges its reverse-lookup cache entry
 * when it breaks. Eager at placement, not lazy-on-first-open -- so a box nobody ever opens still
 * has an id (design decision, 2026-10-05, see decisions.md).
 *
 * <p>{@link BlockEvent.EntityPlaceEvent} also fires for in-place tool transformations (hoe-till,
 * axe-strip, etc.) -- same real gotcha {@code Protectyons.enforcement.VoxelProtectionListener}
 * already documents and guards against. Not actually reachable for any {@link Container} block in
 * practice (a chest/barrel/shulker can't be tool-transformed from something else), but guarded
 * anyway for defense-in-depth against a future tool-ability expansion.
 */
public final class BoxPlacementListener {

    @SubscribeEvent
    public void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (isInPlaceToolTransformation(event)) {
            return;
        }
        if (!(serverLevel.getBlockEntity(event.getPos()) instanceof BlockEntity blockEntity)
                || !(blockEntity instanceof Container)) {
            return;
        }
        if (blockEntity.getExistingData(BoxAttachments.BOX_ID).isPresent()) {
            return;
        }
        UUID boxId = UUID.randomUUID();
        blockEntity.setData(BoxAttachments.BOX_ID, boxId);
        Cartography.registerBox(serverLevel, event.getPos(), boxId);
    }

    @SubscribeEvent
    public void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        // Fires pre-removal -- the block entity (and its attachment) is still readable here.
        if (!(serverLevel.getBlockEntity(event.getPos()) instanceof BlockEntity blockEntity)) {
            return;
        }
        blockEntity.getExistingData(BoxAttachments.BOX_ID)
                .ifPresent(boxId -> Cartography.unregisterBox(serverLevel, boxId));
    }

    /** Same technique as {@code Protectyons.enforcement.VoxelProtectionListener#isInPlaceToolTransformation}. */
    private boolean isInPlaceToolTransformation(BlockEvent.EntityPlaceEvent event) {
        return !event.getBlockSnapshot().getState().isAir();
    }
}
