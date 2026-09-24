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
import com.github.cerealklla.cartographyr.geo.LifecycleState;

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

    // Deliberately a single flat pool for v1, not small/large tiers like NaturalRegionProfile --
    // villages don't vary enough in this first cut to justify it. Revisit if that turns out wrong.
    private static final List<String> NAMES = List.of(
            "Millhaven", "Oakstead", "Riverbend", "Stonewick", "Hearthfield",
            "Ashford", "Cobble Hollow", "Wheatfield", "Brookside", "Thornbury"
    );
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

        String name = NAMES.get(RANDOM.nextInt(NAMES.size()));

        GeographicEntity created = Cartography.createEntity(level, new EntityDefinition(
                level.dimension(),
                Classification.CONSTRUCTED,
                EntityType.SETTLEMENT,
                Optional.of(name),
                footprint,
                LifecycleState.REALIZED
        ));

        GlobalPos structureReference = new GlobalPos(level.dimension(), start.getChunkPos().getWorldPosition());
        Cartography.associateStructure(level, created.id(), structureReference);

        CartographyrMod.LOGGER.info(
                "Settlement discovery: created new settlement '{}' ({}) from {} piece(s), {} footprint vertice(s)",
                name, created.id(), start.getPieces().size(),
                footprint instanceof Geometry.Polygon polygon ? polygon.vertices().size() : "bounds-fallback");

        return created;
    }
}
