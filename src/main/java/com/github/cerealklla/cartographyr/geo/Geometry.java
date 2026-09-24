package com.github.cerealklla.cartographyr.geo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.ChunkPos;

/**
 * Where a {@link GeographicEntity} is located. Deliberately minimal for the first milestone —
 * a single point, or a simple axis-aligned rectangle. The design document leaves room for
 * "another supported shape" (e.g. polygons) later; new kinds slot in as additional record
 * variants plus a new dispatch case, without touching existing data.
 */
public sealed interface Geometry permits Geometry.Point, Geometry.Bounds {

    Codec<Geometry> CODEC = Codec.STRING.dispatch("kind", Geometry::kind, kind -> switch (kind) {
        case "point" -> Point.MAP_CODEC;
        case "bounds" -> Bounds.MAP_CODEC;
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
}
