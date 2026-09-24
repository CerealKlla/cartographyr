package com.github.cerealklla.cartographyr.geo;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A descriptive historical record for a {@link GeographicEntity} (design document Section 5.6).
 * Cartography records these; it does not simulate or generate history itself.
 *
 * <p>{@code gameTime} (the world's game time in ticks, {@code Level#getGameTime()}) is a real
 * field, not buried in free-text metadata, since the design doc explicitly calls for "enough
 * metadata to support provenance and chronology" — chronology needs something sortable. It's
 * caller-supplied rather than read from a live server, keeping storage decoupled from any notion
 * of "current time." {@code metadata} stays free-text for provenance, same as {@link AlternateName}.
 */
public record HistoricalFact(String description, long gameTime, Optional<String> metadata) {
    public static final Codec<HistoricalFact> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("description").forGetter(HistoricalFact::description),
            Codec.LONG.fieldOf("game_time").forGetter(HistoricalFact::gameTime),
            Codec.STRING.optionalFieldOf("metadata").forGetter(HistoricalFact::metadata)
    ).apply(i, HistoricalFact::new));
}
