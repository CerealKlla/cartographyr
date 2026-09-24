package com.github.cerealklla.cartographyr.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.DataResult;

import com.github.cerealklla.cartographyr.geo.Amenity;
import com.github.cerealklla.cartographyr.geo.Characteristic;
import com.github.cerealklla.cartographyr.geo.Classification;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityId;
import com.github.cerealklla.cartographyr.geo.EntityType;
import com.github.cerealklla.cartographyr.geo.Geometry;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.geo.LifecycleState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
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

    @Test
    void structureAssociationSurvivesReloadAndCompletesLifecycle() {
        CartographySavedData original = new CartographySavedData();

        GeographicEntity created = original.createEntity(new EntityDefinition(
                Level.OVERWORLD,
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Optional.of("Capital City of Nonce"),
                new Geometry.Point(0, 0),
                LifecycleState.PLANNED
        ));

        GlobalPos structure = GlobalPos.of(Level.OVERWORLD, new BlockPos(0, 64, 0));
        Optional<GeographicEntity> afterAssociate = original.associateStructure(created.id(), structure);
        assertTrue(afterAssociate.isPresent());
        assertEquals(Set.of(structure), afterAssociate.get().structureReferences());
        assertEquals(Optional.of(created.id()), original.getEntityForStructure(structure));

        original.updateEntity(created.id(), entity -> entity.withLifecycleState(LifecycleState.REALIZED));

        CartographySavedData reloaded = simulateReload(original);

        Optional<GeographicEntity> reloadedEntity = reloaded.getEntity(created.id());
        assertTrue(reloadedEntity.isPresent());
        assertEquals(LifecycleState.REALIZED, reloadedEntity.get().lifecycleState());
        assertEquals(Set.of(structure), reloaded.getAssociatedStructures(created.id()));
        assertEquals(Optional.of(created.id()), reloaded.getEntityForStructure(structure));
    }

    @Test
    void associateStructureFailsIfAlreadyOwnedByAnotherEntity() {
        CartographySavedData data = new CartographySavedData();
        GlobalPos structure = GlobalPos.of(Level.OVERWORLD, new BlockPos(0, 64, 0));

        GeographicEntity first = data.createEntity(new EntityDefinition(
                Level.OVERWORLD, Classification.CONSTRUCTED, EntityType.SETTLEMENT,
                Optional.empty(), new Geometry.Point(0, 0), LifecycleState.PLANNED
        ));
        GeographicEntity second = data.createEntity(new EntityDefinition(
                Level.OVERWORLD, Classification.CONSTRUCTED, EntityType.SETTLEMENT,
                Optional.empty(), new Geometry.Point(100, 100), LifecycleState.PLANNED
        ));

        assertTrue(data.associateStructure(first.id(), structure).isPresent());

        Optional<GeographicEntity> conflicting = data.associateStructure(second.id(), structure);
        assertTrue(conflicting.isEmpty());
        assertEquals(Optional.of(first.id()), data.getEntityForStructure(structure));

        EntityId secondId = second.id();
        assertEquals(Set.of(), data.getAssociatedStructures(secondId));
    }

    @Test
    void characteristicsCanBeAddedRemovedAndSurviveReload() {
        CartographySavedData data = new CartographySavedData();

        GeographicEntity created = data.createEntity(new EntityDefinition(
                Level.OVERWORLD, Classification.CONSTRUCTED, EntityType.SETTLEMENT,
                Optional.of("Nonceville"), new Geometry.Point(0, 0), LifecycleState.REALIZED
        ));

        assertEquals(Set.of(), data.getCharacteristics(created.id()));

        Optional<GeographicEntity> afterAdd = data.addCharacteristic(created.id(), Characteristic.LUMBER);
        assertTrue(afterAdd.isPresent());
        assertEquals(Set.of(Characteristic.LUMBER), data.getCharacteristics(created.id()));

        data.addCharacteristic(created.id(), Characteristic.FARMING);
        assertEquals(Set.of(Characteristic.LUMBER, Characteristic.FARMING), data.getCharacteristics(created.id()));

        data.removeCharacteristic(created.id(), Characteristic.FARMING);
        assertEquals(Set.of(Characteristic.LUMBER), data.getCharacteristics(created.id()));

        CartographySavedData reloaded = simulateReload(data);
        assertEquals(Set.of(Characteristic.LUMBER), reloaded.getCharacteristics(created.id()));
    }

    @Test
    void amenitiesCanBeAddedRemovedAndSurviveReload() {
        CartographySavedData data = new CartographySavedData();

        GeographicEntity created = data.createEntity(new EntityDefinition(
                Level.OVERWORLD, Classification.CONSTRUCTED, EntityType.SETTLEMENT,
                Optional.of("Nonceville"), new Geometry.Point(0, 0), LifecycleState.REALIZED
        ));

        assertEquals(Set.of(), data.getAmenities(created.id()));

        Optional<GeographicEntity> afterAdd = data.addAmenity(created.id(), Amenity.MARKET);
        assertTrue(afterAdd.isPresent());
        assertEquals(Set.of(Amenity.MARKET), data.getAmenities(created.id()));

        data.addAmenity(created.id(), Amenity.INN);
        assertEquals(Set.of(Amenity.MARKET, Amenity.INN), data.getAmenities(created.id()));

        data.removeAmenity(created.id(), Amenity.INN);
        assertEquals(Set.of(Amenity.MARKET), data.getAmenities(created.id()));

        CartographySavedData reloaded = simulateReload(data);
        assertEquals(Set.of(Amenity.MARKET), reloaded.getAmenities(created.id()));
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
