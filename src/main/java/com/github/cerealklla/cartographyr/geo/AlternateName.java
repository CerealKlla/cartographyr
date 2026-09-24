package com.github.cerealklla.cartographyr.geo;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A historical or alternate name for a {@link GeographicEntity}, with optional free-text
 * metadata/provenance (design document Section 3: "Historical or alternate names with optional
 * metadata/provenance"). The design doc doesn't specify a structured provenance shape, so this
 * stays minimal — a single free-text field — rather than inventing a taxonomy (source type, date,
 * etc.) nothing has asked for yet.
 */
public record AlternateName(String name, Optional<String> metadata) {
    public static final Codec<AlternateName> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(AlternateName::name),
            Codec.STRING.optionalFieldOf("metadata").forGetter(AlternateName::metadata)
    ).apply(i, AlternateName::new));
}
