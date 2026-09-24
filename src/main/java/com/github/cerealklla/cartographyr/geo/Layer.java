package com.github.cerealklla.cartographyr.geo;

import com.github.cerealklla.cartographyr.CartographyrMod;

import net.minecraft.resources.Identifier;

/**
 * A named, orderable category a {@link GeographicEntity} belongs to — e.g. Cartographyr's own
 * built-in "Location" layer (natural regions and settlements alike), or a future third-party
 * mod's "Territory" layer. Unlike {@link EntityType}/{@link Classification}, this isn't a bare
 * tag value; it carries real metadata ({@code label}, {@code placement}), so it's backed by an
 * actual registry ({@link LayerRegistry}), not just trust-based construction.
 *
 * <p>Cartographyr only owns this as a data registry — it does not render anything itself. A
 * consumer (e.g. Lyfe's location overlay) is responsible for resolving a {@code layerId} to its
 * {@link Layer} via {@link com.github.cerealklla.cartographyr.api.Cartography#getLayer}, sorting
 * by {@code placement}, and deciding how to display it. {@code placement} has no built-in meaning
 * beyond "a sort key a consumer can use" — by convention (not enforced), a consumer renders higher
 * placements more prominently (e.g. Lyfe stacks lines with the highest placement on top).
 *
 * @param id the layer's own identity, referenced by {@link GeographicEntity#layerId()}
 * @param label a short display label a consumer can show verbatim (e.g. "Location", "Territory")
 * @param placement a sort key; no enforced uniqueness, no enforced meaning beyond ordering
 */
public record Layer(Identifier id, String label, int placement) {

    /** Cartographyr's own built-in layer — natural regions and settlements alike. Placement 0. */
    public static final Identifier LOCATION_ID = Identifier.fromNamespaceAndPath(CartographyrMod.MODID, "location");
}
