package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;

/**
 * Stable identifier for a {@link GeographicEntity}. Names are descriptive data and are never
 * used as identity — this is the only thing other mods should hold onto across sessions.
 */
public record EntityId(long value) {
    // Backed by Codec.STRING rather than Codec.LONG: Codec.unboundedMap requires its key codec
    // to round-trip through DynamicOps#getStringValue, which a raw numeric tag doesn't reliably do.
    public static final Codec<EntityId> CODEC = Codec.STRING.xmap(
            s -> new EntityId(Long.parseLong(s)),
            id -> Long.toString(id.value())
    );
}
