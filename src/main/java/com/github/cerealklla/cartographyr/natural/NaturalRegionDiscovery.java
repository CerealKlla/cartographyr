package com.github.cerealklla.cartographyr.natural;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.github.cerealklla.cartographyr.CartographyrMod;
import com.github.cerealklla.cartographyr.api.Cartography;
import com.github.cerealklla.cartographyr.geo.Classification;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
import com.github.cerealklla.cartographyr.geo.EntityType;
import com.github.cerealklla.cartographyr.geo.GeographicEntity;
import com.github.cerealklla.cartographyr.geo.Geometry;
import com.github.cerealklla.cartographyr.geo.LifecycleState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;

/**
 * Discovers and names a natural region around a point, by sampling biome data on a live {@link
 * ServerLevel} (design document Section 5.8). This is the one piece of Cartographyr that cannot
 * be unit tested the way everything else has been — it fundamentally needs real biome data, not
 * something JUnit can fake without a running game. Verify manually via {@code ./gradlew runClient}
 * instead; everything else in {@code .natural} (the profiles) and the {@link Geometry.Region} it
 * produces are unit tested.
 */
public final class NaturalRegionDiscovery {

    // Bounds worst-case cost and satisfies "should not attempt to exhaustively map an infinite
    // world" (Section 5.8) -- a blob this size spans at most a few hundred blocks across.
    private static final int MAX_CELLS = 200;
    private static final int LARGE_REGION_THRESHOLD = 30;
    private static final Random RANDOM = new Random();

    // How far off the chunk's exact middle block the four extra sample points sit (see
    // chunkMatchesProfile). Chosen to stay well within the chunk's 16-block width regardless of
    // rounding (middle +/- 5 is always inside 0-15).
    private static final int SAMPLE_OFFSET = 5;

    private NaturalRegionDiscovery() {
    }

    /**
     * Empty if the biome at {@code start} doesn't match any known {@link NaturalRegionProfile}.
     * If the newly discovered patch borders an already-discovered region of the same type, this
     * extends that existing entity (keeping its name) instead of creating a new one — without
     * this, walking along the border of a large forest across multiple discovery calls fragments
     * it into several differently-named, overlapping-or-adjacent entities, causing the "You have
     * entered X" message to flicker between names as you cross old boundaries. Confirmed with a
     * real playtest before this fix existed; see decisions.md.
     */
    public static Optional<GeographicEntity> discover(ServerLevel level, BlockPos start) {
        Holder<Biome> startBiome = level.getBiome(start);
        Optional<NaturalRegionProfile> profileOpt = NaturalRegionProfile.ALL.stream()
                .filter(p -> p.matches(startBiome))
                .findFirst();
        if (profileOpt.isEmpty()) {
            return Optional.empty();
        }
        NaturalRegionProfile profile = profileOpt.get();

        Set<Long> newCells = floodFill(level, start, profile);

        Optional<GeographicEntity> adjacentSameType = findAdjacentSameTypeEntity(level, newCells, profile.type());
        if (adjacentSameType.isPresent()) {
            GeographicEntity existing = adjacentSameType.get();
            // Safe cast: every entity this system creates uses Geometry.Region, and
            // findAdjacentSameTypeEntity only returns entities of a profile's own EntityType.
            Set<Long> mergedCells = new HashSet<>(((Geometry.Region) existing.geometry()).cells());
            mergedCells.addAll(newCells);
            CartographyrMod.LOGGER.info(
                    "Natural region discovery: merged {} new cell(s) into existing {} '{}' ({}), now {} cell(s) total",
                    newCells.size(), existing.id(), existing.name().orElse("?"), profile.type(), mergedCells.size());
            return Cartography.updateEntity(level, existing.id(), e -> e.withGeometry(new Geometry.Region(mergedCells)));
        }

        String name = pickName(profile, newCells.size());
        GeographicEntity created = Cartography.createEntity(level, new EntityDefinition(
                level.dimension(),
                Classification.NATURAL,
                profile.type(),
                Optional.of(name),
                new Geometry.Region(newCells),
                LifecycleState.REALIZED
        ));
        CartographyrMod.LOGGER.info(
                "Natural region discovery: created new {} '{}' ({}) with {} cell(s)",
                created.id(), name, profile.type(), newCells.size());
        return Optional.of(created);
    }

    private static Set<Long> floodFill(ServerLevel level, BlockPos start, NaturalRegionProfile profile) {
        long startKey = ChunkPos.containing(start).pack();

        Set<Long> visited = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        visited.add(startKey);
        queue.add(startKey);

        int y = start.getY();

        while (!queue.isEmpty()) {
            ChunkPos currentPos = ChunkPos.unpack(queue.poll());

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    long neighborKey = ChunkPos.pack(currentPos.x() + dx, currentPos.z() + dz);
                    if (visited.contains(neighborKey)) {
                        continue;
                    }

                    ChunkPos neighborPos = ChunkPos.unpack(neighborKey);
                    int sampleX = neighborPos.getMiddleBlockX();
                    int sampleZ = neighborPos.getMiddleBlockZ();

                    // Never expand into a cell some other entity (natural or constructed) already
                    // claims -- prevents overlap and, combined with the merge step in discover(),
                    // is what stops the same forest fragmenting into multiple entities.
                    boolean alreadyClaimed = !Cartography.getEntitiesAt(level, sampleX, sampleZ).isEmpty();
                    boolean biomeMatches = chunkMatchesProfile(level, neighborPos, y, profile);

                    if (biomeMatches && !alreadyClaimed) {
                        visited.add(neighborKey);
                        queue.add(neighborKey);
                        if (visited.size() >= MAX_CELLS) {
                            return visited;
                        }
                    }
                }
            }
        }

        return visited;
    }

    /**
     * Whether any of five sample points across this chunk (its exact middle, plus four points
     * offset {@link #SAMPLE_OFFSET} blocks north/south/east/west of it) match {@code profile}.
     *
     * <p>Sampling only the exact middle point (the original approach) badly fragments narrow,
     * grid-misaligned natural features — rivers especially: a winding river frequently doesn't
     * cross a chunk's precise middle point even while visibly running through much of the chunk,
     * so that chunk would get claimed by whatever <em>does</em> sit at the middle (typically the
     * biome the river cuts through), permanently blocking the river from ever claiming it later
     * (cells can't be reclaimed once any entity holds them). Confirmed via a real playtest: a
     * single physical river had split into two differently-named entities with a "forest"-claimed
     * gap in the middle. Sampling a small cross of points instead makes it far more likely a chunk
     * a river actually passes through gets correctly recognized as river. This doesn't fully
     * eliminate the issue -- a chunk genuinely split between two biomes still has to be claimed by
     * whichever discovery reaches it first, "any point matches" or not -- but meaningfully reduces
     * it. See decisions.md, 2026-09-24.
     */
    private static boolean chunkMatchesProfile(ServerLevel level, ChunkPos pos, int y, NaturalRegionProfile profile) {
        int midX = pos.getMiddleBlockX();
        int midZ = pos.getMiddleBlockZ();
        int[][] points = {
                {midX, midZ},
                {midX - SAMPLE_OFFSET, midZ},
                {midX + SAMPLE_OFFSET, midZ},
                {midX, midZ - SAMPLE_OFFSET},
                {midX, midZ + SAMPLE_OFFSET}
        };
        for (int[] point : points) {
            if (profile.matches(level.getBiome(new BlockPos(point[0], y, point[1])))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scans the cells immediately bordering {@code newCells} (but not in it) for an existing
     * NATURAL entity of the given type. Only merges into the first one found — if the new patch
     * happens to bridge a gap between two separately-discovered same-type regions, the second one
     * is left unmerged (a known limitation; full multi-entity consolidation isn't built).
     */
    private static Optional<GeographicEntity> findAdjacentSameTypeEntity(ServerLevel level, Set<Long> newCells, EntityType type) {
        Set<Long> borderCells = new HashSet<>();
        for (long cell : newCells) {
            ChunkPos pos = ChunkPos.unpack(cell);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    long neighborKey = ChunkPos.pack(pos.x() + dx, pos.z() + dz);
                    if (!newCells.contains(neighborKey)) {
                        borderCells.add(neighborKey);
                    }
                }
            }
        }

        for (long border : borderCells) {
            ChunkPos pos = ChunkPos.unpack(border);
            Optional<GeographicEntity> natural = Cartography.getNaturalRegionAt(level, pos.getMiddleBlockX(), pos.getMiddleBlockZ());
            if (natural.isPresent() && natural.get().type().equals(type)) {
                return natural;
            }
        }
        return Optional.empty();
    }

    private static String pickName(NaturalRegionProfile profile, int cellCount) {
        List<String> options = cellCount < LARGE_REGION_THRESHOLD ? profile.smallNames() : profile.largeNames();
        return options.get(RANDOM.nextInt(options.size()));
    }
}
