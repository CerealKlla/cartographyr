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
 * the definition always wins. This matters for Cartographyr's own built-in {@link
 * Layer#LOCATION_ID} layer, which natural regions and settlements both share despite wanting
 * different defaults ({@link ProtectionLevel#UNPROTECTED} vs {@link
 * ProtectionLevel#NO_VOXEL_CHANGE_ALONG_SURFACE_AND_UP}): rather than that one layer's default
 * trying to express two different values, settlement-creating code passes its own protection
 * level explicitly and only natural-region creation relies on the layer default. A single
 * default-per-layer is otherwise the right fit whenever a layer is truly 1:1 with one kind of
 * thing, as in the Church example above.
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
