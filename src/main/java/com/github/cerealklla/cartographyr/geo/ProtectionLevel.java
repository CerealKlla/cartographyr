package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;

import com.github.cerealklla.cartographyr.CartographyrMod;

import net.minecraft.resources.Identifier;

/**
 * An open, informational tag on a {@link GeographicEntity} describing how much voxel/terrain
 * change is expected to be allowed there -- keyed by an {@link Identifier} rather than a closed
 * Java enum, same trust-based shape as {@link EntityType}/{@link Classification} (see those
 * classes' Javadoc for the full reasoning).
 *
 * <p><b>Cartographyr does not enforce any of this.</b> This is purely descriptive data other mods
 * can read and act on (e.g. a protection mod could cancel a block-break event inside a {@code
 * GeographicEntity} whose {@link GeographicEntity#protectionLevel()} isn't {@link #UNPROTECTED}).
 * Whether and how a level is actually enforced is entirely up to whichever mod is doing the
 * enforcing -- Cartographyr just carries the label.
 *
 * <p>Any mod may register its own level via {@link ProtectionLevelRegistry#register} -- e.g. a
 * future Religyons mod defining {@code "consecrated_ground"} for churches, so an Undead-mob mod
 * can check for it and apply damage. Comparisons must use {@link #equals(Object)}, never {@code ==}.
 */
public record ProtectionLevel(Identifier id) {

    public static final Codec<ProtectionLevel> CODEC = Identifier.CODEC.xmap(ProtectionLevel::new, ProtectionLevel::id);

    /** No restriction implied at all -- the default for naturally-occurring terrain. */
    public static final ProtectionLevel UNPROTECTED = builtin("unprotected");

    /** No voxel changes anywhere in the entity's footprint, from bedrock to build height. */
    public static final ProtectionLevel NO_VOXEL_CHANGE_FULL_HEIGHT = builtin("no_voxel_change_full_height");

    /**
     * No voxel changes at/above the ground surface within the entity's footprint -- the default
     * for player-founded settlements (a settlement still needs its underlying terrain query-able,
     * but the built world above it shouldn't be diggable/buildable-over by outsiders).
     */
    public static final ProtectionLevel NO_VOXEL_CHANGE_ALONG_SURFACE_AND_UP = builtin("no_voxel_change_along_surface_and_up");

    private static ProtectionLevel builtin(String path) {
        return new ProtectionLevel(Identifier.fromNamespaceAndPath(CartographyrMod.MODID, path));
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
