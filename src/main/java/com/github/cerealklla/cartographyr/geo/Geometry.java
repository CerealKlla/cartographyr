package com.github.cerealklla.cartographyr.geo;

import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.ChunkPos;

/**
 * Where a {@link GeographicEntity} is located: a single point, a simple axis-aligned rectangle,
 * or an irregular chunk-cell region (for naturally discovered formations — see the {@code
 * .natural} package). The design document leaves room for further shapes later; new kinds slot in
 * as additional record variants plus a new dispatch case, without touching existing data.
 */
public sealed interface Geometry permits Geometry.Point, Geometry.Bounds, Geometry.Region {

    Codec<Geometry> CODEC = Codec.STRING.dispatch("kind", Geometry::kind, kind -> switch (kind) {
        case "point" -> Point.MAP_CODEC;
        case "bounds" -> Bounds.MAP_CODEC;
        case "region" -> Region.MAP_CODEC;
        default -> throw new IllegalArgumentException("Unknown geometry kind: " + kind);
    });

    String kind();

    /** Exact containment check against the real geometry (spatial-index cells only hold candidates). */
    boolean contains(int x, int z);

    /** Chunk cell range this geometry occupies, for spatial-index insertion. */
    ChunkPos minChunk();

    ChunkPos maxChunk();

    record Point(int x, int z) implements Geometry {
        static final MapCodec<Point> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Point::x),
                Codec.INT.fieldOf("z").forGetter(Point::z)
        ).apply(i, Point::new));

        @Override
        public String kind() {
            return "point";
        }

        @Override
        public boolean contains(int x, int z) {
            return this.x == x && this.z == z;
        }

        @Override
        public ChunkPos minChunk() {
            return new ChunkPos(x >> 4, z >> 4);
        }

        @Override
        public ChunkPos maxChunk() {
            return minChunk();
        }
    }

    record Bounds(int minX, int minZ, int maxX, int maxZ) implements Geometry {
        static final MapCodec<Bounds> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("min_x").forGetter(Bounds::minX),
                Codec.INT.fieldOf("min_z").forGetter(Bounds::minZ),
                Codec.INT.fieldOf("max_x").forGetter(Bounds::maxX),
                Codec.INT.fieldOf("max_z").forGetter(Bounds::maxZ)
        ).apply(i, Bounds::new));

        public Bounds {
            if (minX > maxX || minZ > maxZ) {
                throw new IllegalArgumentException(
                        "Invalid bounds: min must not exceed max (x: " + minX + ".." + maxX + ", z: " + minZ + ".." + maxZ + ")");
            }
        }

        @Override
        public String kind() {
            return "bounds";
        }

        @Override
        public boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        @Override
        public ChunkPos minChunk() {
            return new ChunkPos(minX >> 4, minZ >> 4);
        }

        @Override
        public ChunkPos maxChunk() {
            return new ChunkPos(maxX >> 4, maxZ >> 4);
        }
    }

    /**
     * An irregular region as a set of chunk cells (design document Section 7.4: "compact region
     * geometry... sampled/coarse cells," not per-block storage). Cells are {@link ChunkPos#pack()}
     * keys. {@code minChunk}/{@code maxChunk} are just the bounding box over {@code cells} — the
     * spatial index only needs candidates from them, since {@link #contains} does the real check.
     */
    record Region(Set<Long> cells) implements Geometry {
        static final MapCodec<Region> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.list(Codec.LONG).xmap(Set::copyOf, List::copyOf).fieldOf("cells").forGetter(Region::cells)
        ).apply(i, Region::new));

        public Region {
            if (cells.isEmpty()) {
                throw new IllegalArgumentException("Region must contain at least one cell");
            }
        }

        @Override
        public String kind() {
            return "region";
        }

        @Override
        public boolean contains(int x, int z) {
            return cells.contains(ChunkPos.pack(x >> 4, z >> 4));
        }

        @Override
        public ChunkPos minChunk() {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            for (long cell : cells) {
                ChunkPos pos = ChunkPos.unpack(cell);
                minX = Math.min(minX, pos.x());
                minZ = Math.min(minZ, pos.z());
            }
            return new ChunkPos(minX, minZ);
        }

        @Override
        public ChunkPos maxChunk() {
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (long cell : cells) {
                ChunkPos pos = ChunkPos.unpack(cell);
                maxX = Math.max(maxX, pos.x());
                maxZ = Math.max(maxZ, pos.z());
            }
            return new ChunkPos(maxX, maxZ);
        }
    }
}
