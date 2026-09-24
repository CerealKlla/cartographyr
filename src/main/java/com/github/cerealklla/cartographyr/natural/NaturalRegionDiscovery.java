package com.github.cerealklla.cartographyr.natural;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.github.cerealklla.cartographyr.api.Cartography;
import com.github.cerealklla.cartographyr.geo.Classification;
import com.github.cerealklla.cartographyr.geo.EntityDefinition;
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

    private NaturalRegionDiscovery() {
    }

    /** Empty if the biome at {@code start} doesn't match any known {@link NaturalRegionProfile}. */
    public static Optional<GeographicEntity> discover(ServerLevel level, BlockPos start) {
        Holder<Biome> startBiome = level.getBiome(start);
        Optional<NaturalRegionProfile> profile = NaturalRegionProfile.ALL.stream()
                .filter(p -> p.matches(startBiome))
                .findFirst();
        if (profile.isEmpty()) {
            return Optional.empty();
        }

        Set<Long> cells = floodFill(level, start, profile.get());
        String name = pickName(profile.get(), cells.size());

        GeographicEntity created = Cartography.createEntity(level, new EntityDefinition(
                level.dimension(),
                Classification.NATURAL,
                profile.get().type(),
                Optional.of(name),
                new Geometry.Region(cells),
                LifecycleState.REALIZED
        ));
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
                    BlockPos samplePos = new BlockPos(neighborPos.getMiddleBlockX(), y, neighborPos.getMiddleBlockZ());
                    if (profile.matches(level.getBiome(samplePos))) {
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

    private static String pickName(NaturalRegionProfile profile, int cellCount) {
        List<String> options = cellCount < LARGE_REGION_THRESHOLD ? profile.smallNames() : profile.largeNames();
        return options.get(RANDOM.nextInt(options.size()));
    }
}
