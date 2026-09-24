package com.github.cerealklla.cartographyr.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.DataResult;

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
                LifecycleState.PLANNED
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
                LifecycleState.REALIZED
        );

        GeographicEntity decoded = roundTrip(original);

        assertEquals(original, decoded);
    }

    private static GeographicEntity roundTrip(GeographicEntity entity) {
        DataResult<Tag> encodeResult = GeographicEntity.CODEC.encodeStart(NbtOps.INSTANCE, entity);
        Tag encoded = encodeResult.result().orElseThrow(() -> new AssertionError("Encode failed: " + encodeResult.error()));

        DataResult<GeographicEntity> decodeResult = GeographicEntity.CODEC.parse(NbtOps.INSTANCE, encoded);
        return decodeResult.result().orElseThrow(() -> new AssertionError("Decode failed: " + decodeResult.error()));
    }
}
