package com.github.cerealklla.cartographyr.geo;

import java.util.List;
import java.util.Optional;

/**
 * The result of {@code getNames} (design document Section 5.3): an entity's current authoritative
 * name plus its full alternate/historical name list. Not persisted itself — assembled on read
 * from {@link GeographicEntity}'s own {@code name} and {@code alternateNames} fields.
 */
public record EntityNames(Optional<String> currentName, List<AlternateName> alternateNames) {
}
