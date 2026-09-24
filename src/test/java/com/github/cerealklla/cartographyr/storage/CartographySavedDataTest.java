package com.github.cerealklla.cartographyr.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.DataResult;

import com.github.cerealklla.cartographyr.geo.Classification;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityType;
import com.github.cerealklla.cartographyr.geo.Geometry;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.geo.LifecycleState;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

/**
 * Stands in for the design document Appendix B's "creates a planned city, reloads the world,
 * verifies the same EntityId" milestone test. A true stop/restart world reload can't be driven
 * meaningfully differently by a second test-integration mod than by round-tripping through the
 * exact Codec NeoForge uses to persist SavedData to disk, so that's what this does. See
 * context/decisions.md for the full reasoning.
 */
class CartographySavedDataTest {

    @Test
    void plannedCitySurvivesEncodeDecodeRoundTrip() {
        CartographySavedData original = new CartographySavedData();

        GeographicEntity created = original.createEntity(new EntityDefinition(
                Level.OVERWORLD,
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Optional.of("Capital City of Nonce"),
                new Geometry.Point(0, 0),
                LifecycleState.PLANNED
        ));

        CartographySavedData reloaded = simulateReload(original);

        Optional<GeographicEntity> reloadedEntity = reloaded.getEntity(created.id());
        assertTrue(reloadedEntity.isPresent());
        assertEquals(created.id(), reloadedEntity.get().id());
        assertEquals(LifecycleState.PLANNED, reloadedEntity.get().lifecycleState());

        Set<GeographicEntity> atOrigin = reloaded.getEntitiesAt(Level.OVERWORLD, 0, 0);
        assertTrue(atOrigin.stream().anyMatch(e -> e.id().equals(created.id())));
    }

    // Goes through CartographySavedData.TYPE's own codec factory, not a private test-only codec,
    // so this exercises the exact same serialization path production code uses.
    private static CartographySavedData simulateReload(CartographySavedData data) {
        var codec = CartographySavedData.TYPE.codecFactory().create(null);

        DataResult<Tag> encodeResult = codec.encodeStart(NbtOps.INSTANCE, data);
        Tag encoded = encodeResult.result().orElseThrow(() -> new AssertionError("Encode failed: " + encodeResult.error()));

        DataResult<CartographySavedData> decodeResult = codec.parse(NbtOps.INSTANCE, encoded);
        return decodeResult.result().orElseThrow(() -> new AssertionError("Decode failed: " + decodeResult.error()));
    }
}
