package com.github.cerealklla.cartographyr.geo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;

/**
 * In-memory registry of each {@link Layer}'s default {@link ProtectionLevel} for newly created
 * entities, e.g. a future Religyons mod registering a "Church" layer and setting its default to
 * {@code "consecrated_ground"}. Populated via {@link
 * com.github.cerealklla.cartographyr.api.Cartography#setDefaultProtectionLevel} at mod startup,
 * same not-persisted/rebuilt-every-boot shape as {@link LayerRegistry}/{@link
 * ProtectionLevelRegistry}.
 *
 * <p>Only consulted by {@link GeographicEntity#create} when an {@link EntityDefinition} doesn't
 * specify its own {@link EntityDefinition#protectionLevel()} explicitly -- an explicit value on
 * the definition always wins. Cartographyr's own built-in {@link Layer#REGION_ID}/{@link
 * Layer#SETTLEMENT_ID} layers each carry their own accurate default ({@link
 * ProtectionLevel#UNPROTECTED} and {@link ProtectionLevel#NO_VOXEL_CHANGE_ALONG_SURFACE_AND_UP}
 * respectively) -- natural region and settlement creation both rely on this alone, with no
 * explicit per-creation override needed. (Before the 2026-09-26 Region/Settlement layer split,
 * both shared one layer and couldn't express two different defaults this way; settlement-creating
 * code passed its protection level explicitly as a workaround -- see decisions.md, same date, for
 * that history.) A single default-per-layer is the right fit whenever a layer is 1:1 with one kind
 * of thing, as in the Church example above.
 */
public final class ProtectionDefaults {

    private static final Map<Identifier, ProtectionLevel> DEFAULTS = new ConcurrentHashMap<>();

    private ProtectionDefaults() {
    }

    public static void set(Identifier layerId, ProtectionLevel level) {
        DEFAULTS.put(layerId, level);
    }

    public static Optional<ProtectionLevel> get(Identifier layerId) {
        return Optional.ofNullable(DEFAULTS.get(layerId));
    }
}
