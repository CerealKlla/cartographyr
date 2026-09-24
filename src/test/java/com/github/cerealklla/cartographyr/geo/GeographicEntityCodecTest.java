package com.github.cerealklla.cartographyr.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

class GeographicEntityCodecTest {

    @Test
    void roundTripsPointGeometry() {
        GeographicEntity original = new GeographicEntity(
                new EntityId(42L),
                Level.OVERWORLD,
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Optional.of("Capital City of Nonce"),
                new Geometry.Point(100, -50),
                LifecycleState.PLANNED,
                Set.of(),
                Set.of(),
                Set.of(),
                List.of(),
                List.of()
        );

        GeographicEntity decoded = roundTrip(original);

        assertEquals(original, decoded);
        assertEquals(original.id(), decoded.id());
    }

    @Test
    void roundTripsBoundsGeometry() {
        GeographicEntity original = new GeographicEntity(
                new EntityId(7L),
                Level.OVERWORLD,
                Classification.NATURAL,
                EntityType.MOUNTAIN,
                Optional.empty(),
                new Geometry.Bounds(-10, -10, 10, 10),
                LifecycleState.REALIZED,
                Set.of(),
                Set.of(),
                Set.of(),
                List.of(),
                List.of()
        );

        GeographicEntity decoded = roundTrip(original);

        assertEquals(original, decoded);
    }

    @Test
    void roundTripsStructureReferences() {
        GeographicEntity original = new GeographicEntity(
                new EntityId(99L),
                Level.OVERWORLD,
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Optional.of("Nonceville"),
                new Geometry.Point(0, 0),
                LifecycleState.REALIZED,
                Set.of(GlobalPos.of(Level.OVERWORLD, new BlockPos(0, 64, 0))),
                Set.of(),
                Set.of(),
                List.of(),
                List.of()
        );

        GeographicEntity decoded = roundTrip(original);

        assertEquals(original, decoded);
        assertEquals(original.structureReferences(), decoded.structureReferences());
    }

    @Test
    void roundTripsCharacteristics() {
        GeographicEntity original = new GeographicEntity(
                new EntityId(5L),
                Level.OVERWORLD,
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Optional.of("Nonceville"),
                new Geometry.Point(0, 0),
                LifecycleState.REALIZED,
                Set.of(),
                Set.of(Characteristic.LUMBER, Characteristic.FARMING),
                Set.of(),
                List.of(),
                List.of()
        );

        GeographicEntity decoded = roundTrip(original);

        assertEquals(original, decoded);
        assertEquals(original.characteristics(), decoded.characteristics());
    }

    @Test
    void roundTripsAmenities() {
        GeographicEntity original = new GeographicEntity(
                new EntityId(6L),
                Level.OVERWORLD,
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Optional.of("Nonceville"),
                new Geometry.Point(0, 0),
                LifecycleState.REALIZED,
                Set.of(),
                Set.of(),
                Set.of(Amenity.MARKET, Amenity.INN),
                List.of(),
                List.of()
        );

        GeographicEntity decoded = roundTrip(original);

        assertEquals(original, decoded);
        assertEquals(original.amenities(), decoded.amenities());
    }

    @Test
    void roundTripsAlternateNames() {
        GeographicEntity original = new GeographicEntity(
                new EntityId(8L),
                Level.OVERWORLD,
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Optional.of("Nonceville"),
                new Geometry.Point(0, 0),
                LifecycleState.REALIZED,
                Set.of(),
                Set.of(),
                Set.of(),
                List.of(
                        new AlternateName("Old Nonceville", Optional.of("pre-renaming name, per town records")),
                        new AlternateName("The Lumber Camp", Optional.empty())
                ),
                List.of()
        );

        GeographicEntity decoded = roundTrip(original);

        assertEquals(original, decoded);
        assertEquals(original.alternateNames(), decoded.alternateNames());
    }

    @Test
    void roundTripsHistoricalFacts() {
        GeographicEntity original = new GeographicEntity(
                new EntityId(9L),
                Level.OVERWORLD,
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Optional.of("Nonceville"),
                new Geometry.Point(0, 0),
                LifecycleState.REALIZED,
                Set.of(),
                Set.of(),
                Set.of(),
                List.of(),
                List.of(
                        new HistoricalFact("Founded by wandering traders", 0L, Optional.of("town records")),
                        new HistoricalFact("Lumber mill built", 24000L, Optional.empty())
                )
        );

        GeographicEntity decoded = roundTrip(original);

        assertEquals(original, decoded);
        assertEquals(original.historicalFacts(), decoded.historicalFacts());
    }

    private static GeographicEntity roundTrip(GeographicEntity entity) {
        DataResult<Tag> encodeResult = GeographicEntity.CODEC.encodeStart(NbtOps.INSTANCE, entity);
        Tag encoded = encodeResult.result().orElseThrow(() -> new AssertionError("Encode failed: " + encodeResult.error()));

        DataResult<GeographicEntity> decodeResult = GeographicEntity.CODEC.parse(NbtOps.INSTANCE, encoded);
        return decodeResult.result().orElseThrow(() -> new AssertionError("Decode failed: " + decodeResult.error()));
    }
}
