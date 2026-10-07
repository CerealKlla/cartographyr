package com.github.cerealklla.cartographyr.box;

import java.util.UUID;
import java.util.function.Supplier;

import com.github.cerealklla.cartographyr.CartographyrMod;

import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * {@code BOX_ID} is attached directly to a storage box's own {@code BlockEntity} (block entities
 * are {@code AttachmentHolder}s too, same precedent as Lyfe's {@code SIGN_REFERENCE} on a placed
 * sign) rather than tracked only in {@code CartographySavedData}'s reverse index -- this is what
 * lets the id survive exactly as long as the physical box does, with no separate migration step.
 *
 * <p>No default value is ever meaningfully used -- same convention as {@code SIGN_REFERENCE}:
 * readers always check {@code getExistingData} first, since most boxes in the world have no
 * attachment at all (never placed through {@code box.BoxPlacementListener}, e.g. pre-existing
 * world-gen chests). Not synced -- this is server-only bookkeeping, nothing client-side reads it.
 */
public final class BoxAttachments {

    private BoxAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CartographyrMod.MODID);

    public static final Supplier<AttachmentType<UUID>> BOX_ID = ATTACHMENT_TYPES.register(
            "box_id",
            () -> AttachmentType.builder(holder -> new UUID(0L, 0L))
                    .serialize(UUIDUtil.CODEC.fieldOf("id"))
                    .build()
    );
}
