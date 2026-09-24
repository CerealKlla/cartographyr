package com.github.cerealklla.cartographyr.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.DataResult;

import com.github.cerealklla.cartographyr.geo.AlternateName;
import com.github.cerealklla.cartographyr.geo.Amenity;
import com.github.cerealklla.cartographyr.geo.Characteristic;
import com.github.cerealklla.cartographyr.geo.Classification;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityId;
import com.github.cerealklla.cartographyr.geo.EntityNames;
import com.github.cerealklla.cartographyr.geo.EntityType;
import com.github.cerealklla.cartographyr.geo.Geometry;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.geo.HistoricalFact;
import com.github.cerealklla.cartographyr.geo.LifecycleState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
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

    @Test
    void namingSupportsSetAndAlternatesAndSurvivesReload() {
        CartographySavedData data = new CartographySavedData();

        GeographicEntity created = data.createEntity(new EntityDefinition(
                Level.OVERWORLD, Classification.CONSTRUCTED, EntityType.SETTLEMENT,
                Optional.empty(), new Geometry.Point(0, 0), LifecycleState.REALIZED
        ));

        Optional<EntityNames> beforeNaming = data.getNames(created.id());
        assertTrue(beforeNaming.isPresent());
        assertEquals(Optional.empty(), beforeNaming.get().currentName());
        assertEquals(List.of(), beforeNaming.get().alternateNames());

        data.setName(created.id(), "Nonceville");
        data.addAlternateName(created.id(), "Old Nonceville", Optional.of("pre-renaming name, per town records"));
        data.addAlternateName(created.id(), "The Lumber Camp", Optional.empty());

        Optional<EntityNames> afterNaming = data.getNames(created.id());
        assertTrue(afterNaming.isPresent());
        assertEquals(Optional.of("Nonceville"), afterNaming.get().currentName());
        assertEquals(
                List.of(
                        new AlternateName("Old Nonceville", Optional.of("pre-renaming name, per town records")),
                        new AlternateName("The Lumber Camp", Optional.empty())
                ),
                afterNaming.get().alternateNames()
        );

        CartographySavedData reloaded = simulateReload(data);
        Optional<EntityNames> reloadedNames = reloaded.getNames(created.id());
        assertTrue(reloadedNames.isPresent());
        assertEquals(afterNaming.get(), reloadedNames.get());
    }

    @Test
    void getNamesReturnsEmptyForUnknownEntity() {
        CartographySavedData data = new CartographySavedData();
        assertEquals(Optional.empty(), data.getNames(new EntityId(999L)));
    }

    @Test
    void historicalFactsStaySortedByGameTimeEvenWhenAddedOutOfOrder() {
        CartographySavedData data = new CartographySavedData();

        GeographicEntity created = data.createEntity(new EntityDefinition(
                Level.OVERWORLD, Classification.CONSTRUCTED, EntityType.SETTLEMENT,
                Optional.of("Nonceville"), new Geometry.Point(0, 0), LifecycleState.REALIZED
        ));

        assertEquals(List.of(), data.getHistoricalFacts(created.id()));

        // Deliberately added out of chronological order, e.g. an old newspaper found later
        // revealing an earlier event (design doc Section 11.2) -- should still read back sorted.
        data.addHistoricalFact(created.id(), new HistoricalFact("Lumber mill built", 24000L, Optional.empty()));
        data.addHistoricalFact(created.id(), new HistoricalFact("Founded by wandering traders", 0L, Optional.of("town records")));
        data.addHistoricalFact(created.id(), new HistoricalFact("Market established", 12000L, Optional.empty()));

        List<HistoricalFact> facts = data.getHistoricalFacts(created.id());
        assertEquals(
                List.of(
                        new HistoricalFact("Founded by wandering traders", 0L, Optional.of("town records")),
                        new HistoricalFact("Market established", 12000L, Optional.empty()),
                        new HistoricalFact("Lumber mill built", 24000L, Optional.empty())
                ),
                facts
        );

        CartographySavedData reloaded = simulateReload(data);
        assertEquals(facts, reloaded.getHistoricalFacts(created.id()));
    }

    @Test
    void naturalRegionQueriesFindDiscoveredRegions() {
        // Manually constructed, standing in for NaturalRegionDiscovery's output -- the flood-fill
        // algorithm itself needs a live ServerLevel to sample biome data and can't be unit tested
        // (see context/decisions.md). These query methods are what it's built on, so they're
        // tested directly against a hand-built Region entity instead.
        CartographySavedData data = new CartographySavedData();

        Geometry.Region desertShape = new Geometry.Region(Set.of(ChunkPos.pack(0, 0), ChunkPos.pack(1, 0)));
        GeographicEntity desert = data.createEntity(new EntityDefinition(
                Level.OVERWORLD, Classification.NATURAL, EntityType.DESERT,
                Optional.of("The Forsaken Sands"), desertShape, LifecycleState.REALIZED
        ));

        GeographicEntity settlement = data.createEntity(new EntityDefinition(
                Level.OVERWORLD, Classification.CONSTRUCTED, EntityType.SETTLEMENT,
                Optional.of("Nonceville"), new Geometry.Point(500, 500), LifecycleState.REALIZED
        ));

        // A point inside the desert's first cell (chunk 0,0 covers blocks 0..15)
        Optional<GeographicEntity> foundAt = data.getNaturalRegionAt(Level.OVERWORLD, 5, 5);
        assertTrue(foundAt.isPresent());
        assertEquals(desert.id(), foundAt.get().id());

        // The settlement is CONSTRUCTED, not NATURAL, so it must never show up here even though
        // getEntitiesAt would find it at its own point.
        assertEquals(Optional.empty(), data.getNaturalRegionAt(Level.OVERWORLD, 500, 500));

        Set<GeographicEntity> deserts = data.findNaturalRegions(Level.OVERWORLD, EntityType.DESERT);
        assertEquals(Set.of(desert), deserts);
        assertEquals(Set.of(), data.findNaturalRegions(Level.OVERWORLD, EntityType.FOREST));

        assertEquals(Optional.of(desertShape), data.getRegionBounds(desert.id()));
        assertEquals(Optional.empty(), data.getRegionBounds(new EntityId(999L)));

        // Not classification-restricted -- works for the constructed entity too.
        assertTrue(data.getRegionBounds(settlement.id()).isPresent());
    }

    @Test
    void thirdPartyNamespacedTypeSurvivesReloadAndEqualityLookups() {
        // Simulates a mod other than Cartographyr tagging an entity with its own EntityType/
        // Classification (e.g. a future Factions mod) -- the whole point of the open,
        // Identifier-keyed migration (see decisions.md, 2026-09-24). No registration step exists;
        // any namespace works.
        EntityType factionTerritory = new EntityType(Identifier.fromNamespaceAndPath("factionsmod", "territory"));
        Classification claimed = new Classification(Identifier.fromNamespaceAndPath("factionsmod", "claimed"));

        CartographySavedData data = new CartographySavedData();
        GeographicEntity territory = data.createEntity(new EntityDefinition(
                Level.OVERWORLD, claimed, factionTerritory,
                Optional.of("Redguard Claim"), new Geometry.Point(10, 10), LifecycleState.REALIZED
        ));

        CartographySavedData reloaded = simulateReload(data);
        Optional<GeographicEntity> reloadedEntity = reloaded.getEntity(territory.id());
        assertTrue(reloadedEntity.isPresent());

        // A freshly-constructed EntityType/Classification instance for the same id must be .equals()
        // to the one decoded from storage -- these are records now, not enum singletons, so this is
        // the actual regression this migration could introduce if left unverified.
        assertEquals(new EntityType(Identifier.fromNamespaceAndPath("factionsmod", "territory")), reloadedEntity.get().type());
        assertEquals(new Classification(Identifier.fromNamespaceAndPath("factionsmod", "claimed")), reloadedEntity.get().classification());

        // findNaturalRegions filters on Classification.NATURAL, so a CONSTRUCTED-style third-party
        // classification correctly finds nothing here -- confirms .equals() (not identity) is doing
        // the real filtering work end to end, including through a third-party classification value.
        assertEquals(Set.of(), data.findNaturalRegions(Level.OVERWORLD, factionTerritory));
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
