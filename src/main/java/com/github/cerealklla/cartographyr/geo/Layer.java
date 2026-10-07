package com.github.cerealklla.cartographyr.geo;

import com.github.cerealklla.cartographyr.CartographyrMod;

import net.minecraft.resources.Identifier;

/**
 * A named, orderable category a {@link GeographicEntity} belongs to — e.g. Cartographyr's own
 * built-in "Region"/"Settlement" layers, or a future third-party mod's "Territory" layer. Unlike
 * {@link EntityType}/{@link Classification}, this isn't a bare tag value; it carries real metadata
 * ({@code label}, {@code placement}), so it's backed by an actual registry ({@link
 * LayerRegistry}), not just trust-based construction.
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

    /**
     * Cartographyr's own built-in Region layer (natural regions only) — placement 0. Split off
     * from a single shared "Location" layer 2026-09-26 (see decisions.md) so destroying a
     * settlement never requires touching the natural region underneath it. Despite the different
     * placement from {@link #SETTLEMENT_ID}, a consumer (Lyfe's location overlay) is expected to
     * render both on the **same** HUD row, not as two separate stacked lines — preferring the
     * Settlement layer's entity over the Region layer's when both match a point, the same
     * "prefer the more specific match" pattern this shared layer used before the split.
     */
    public static final Identifier REGION_ID = Identifier.fromNamespaceAndPath(CartographyrMod.MODID, "region");

    /**
     * Cartographyr's own built-in Settlement layer (player-founded and world-gen-discovered
     * settlements alike) — placement 1. See {@link #REGION_ID}'s Javadoc for why this is a
     * separate layer from Region despite sharing one HUD row in practice.
     */
    public static final Identifier SETTLEMENT_ID = Identifier.fromNamespaceAndPath(CartographyrMod.MODID, "settlement");

    /**
     * Cartographyr's own built-in Roadway layer (added 2026-10-06, Settlemynts' Roadways feature) —
     * placement 0, same as {@link #REGION_ID}. One {@link GeographicEntity} per paved road edge,
     * geometry = {@link Geometry.Path}. Cartographyr only declares the layer and its default {@link
     * ProtectionLevel} here; Settlemynts' own {@code roadway} package owns the stake/paving mechanic
     * and enforces "nothing can damage a road block" directly (plain unbreakable block properties),
     * not through this layer's protection level — see that mod's own context docs.
     */
    public static final Identifier ROADWAY_ID = Identifier.fromNamespaceAndPath(CartographyrMod.MODID, "roadway");
}
