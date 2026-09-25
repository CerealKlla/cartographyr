package com.github.cerealklla.cartographyr.settlement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.github.cerealklla.cartographyr.CartographyrMod;
import com.github.cerealklla.cartographyr.api.Cartography;
import com.github.cerealklla.cartographyr.geo.Classification;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityType;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.geo.Geometry;
import com.github.cerealklla.cartographyr.geo.Layer;
import com.github.cerealklla.cartographyr.geo.LifecycleState;
import com.github.cerealklla.cartographyr.natural.NaturalRegionProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Creates a {@link GeographicEntity} from a vanilla-generated village's real placement data,
 * called by {@link SettlementListener} once per structure instance. This is the one piece of this
 * package that cannot be unit tested the way everything else has been — it fundamentally needs a
 * real {@link StructureStart} produced by live world generation, not something JUnit can fake
 * without a running game (same limitation as {@code natural.NaturalRegionDiscovery}). The polygon
 * math it calls into ({@link Geometry.Polygon#convexHull}) is what's actually unit tested; verify
 * this class itself manually via {@code ./gradlew runClient}.
 */
public final class SettlementDiscovery {

    // How far out (and where) to sample for a nearby natural feature to theme the name off of --
    // deliberately a small, non-exhaustive point set, same "sample a handful of points, not every
    // block" precedent as NaturalRegionDiscovery.chunkMatchesProfile's 5-point cross.
    private static final int[] SAMPLE_RADII = {32, 64};

    private static final Random RANDOM = new Random();

    private SettlementDiscovery() {
    }

    /** Builds and persists a new SETTLEMENT entity from {@code start}, associated to its structure. */
    public static GeographicEntity discover(ServerLevel level, StructureStart start) {
        List<Geometry.Polygon.Vertex> corners = new ArrayList<>();
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox box = piece.getBoundingBox();
            corners.add(new Geometry.Polygon.Vertex(box.minX(), box.minZ()));
            corners.add(new Geometry.Polygon.Vertex(box.minX(), box.maxZ()));
            corners.add(new Geometry.Polygon.Vertex(box.maxX(), box.minZ()));
            corners.add(new Geometry.Polygon.Vertex(box.maxX(), box.maxZ()));
        }
        Geometry footprint = Geometry.Polygon.convexHull(corners);

        Optional<EntityType> nearbyFeature = findNearbyNaturalFeature(level, start);
        String name = SettlementNaming.pickName(nearbyFeature, RANDOM);
        int pieceCount = start.getPieces().size();
        String designation = SettlementNaming.designationFor(pieceCount);

        GeographicEntity created = Cartography.createEntity(level, new EntityDefinition(
                level.dimension(),
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Layer.LOCATION_ID,
                Optional.of(name),
                footprint,
                LifecycleState.REALIZED
        ));
        // Set as a starting point, not baked into EntityDefinition -- a town-management mod is
        // expected to be the thing that upgrades this over the settlement's lifetime (design
        // intent, see Cartography#setDesignation and decisions.md, 2026-09-25).
        Cartography.setDesignation(level, created.id(), designation);

        GlobalPos structureReference = new GlobalPos(level.dimension(), start.getChunkPos().getWorldPosition());
        Cartography.associateStructure(level, created.id(), structureReference);

        CartographyrMod.LOGGER.info(
                "Settlement discovery: created new {} '{}' ({}) from {} piece(s), {} footprint vertice(s)",
                designation, name, created.id(), pieceCount,
                footprint instanceof Geometry.Polygon polygon ? polygon.vertices().size() : "bounds-fallback");

        return created;
    }

    /**
     * Samples a small, non-exhaustive ring of points around the structure's center for a matching
     * {@link NaturalRegionProfile} (reusing Natural Geography's own biome-matching logic directly,
     * not a duplicate copy). Center is checked first, then a 32-block ring, then a 64-block ring --
     * closer features win. {@link EntityType#PLAINS} is deliberately excluded from "found a
     * feature": it's the most common biome and has no strong visual identity, so without this
     * exclusion a plains village with a river just outside the inner ring would short-circuit on
     * its own uninteresting biome before ever sampling further out.
     */
    private static Optional<EntityType> findNearbyNaturalFeature(ServerLevel level, StructureStart start) {
        BlockPos center = start.getBoundingBox().getCenter();

        List<BlockPos> samples = new ArrayList<>();
        samples.add(center);
        for (int radius : SAMPLE_RADII) {
            samples.add(center.offset(radius, 0, 0));
            samples.add(center.offset(-radius, 0, 0));
            samples.add(center.offset(0, 0, radius));
            samples.add(center.offset(0, 0, -radius));
        }

        for (BlockPos pos : samples) {
            Optional<NaturalRegionProfile> match = NaturalRegionProfile.find(level.getBiome(pos));
            if (match.isPresent() && !match.get().type().equals(EntityType.PLAINS)) {
                return Optional.of(match.get().type());
            }
        }
        return Optional.empty();
    }
}
