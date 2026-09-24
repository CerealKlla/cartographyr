package com.github.cerealklla.cartographyr.geo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.ChunkPos;

/**
 * Where a {@link GeographicEntity} is located: a single point, a simple axis-aligned rectangle,
 * an irregular chunk-cell region (for naturally discovered formations — see the {@code .natural}
 * package), or a polygon (for structure footprints — see the {@code .settlement} package). The
 * design document leaves room for further shapes later; new kinds slot in as additional record
 * variants plus a new dispatch case, without touching existing data.
 */
public sealed interface Geometry permits Geometry.Point, Geometry.Bounds, Geometry.Region, Geometry.Polygon {

    Codec<Geometry> CODEC = Codec.STRING.dispatch("kind", Geometry::kind, kind -> switch (kind) {
        case "point" -> Point.MAP_CODEC;
        case "bounds" -> Bounds.MAP_CODEC;
        case "region" -> Region.MAP_CODEC;
        case "polygon" -> Polygon.MAP_CODEC;
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

    /**
     * A simple polygon footprint (design document Section 5.7-adjacent — structure discovery, see
     * {@code .settlement} package), block-precise at the vertices but not pixel-perfect overall:
     * it's meant to hug an irregular structure's real shape far more closely than a single {@link
     * Bounds} rectangle would, without paying per-block or per-piece storage cost. {@code
     * contains} uses the standard even-odd ray-casting rule, so behavior exactly on an edge is not
     * guaranteed either way — acceptable given the shape itself is already an approximation.
     */
    record Polygon(List<Vertex> vertices) implements Geometry {
        static final MapCodec<Polygon> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.list(Vertex.CODEC).fieldOf("vertices").forGetter(Polygon::vertices)
        ).apply(i, Polygon::new));

        public Polygon {
            if (vertices.size() < 3) {
                throw new IllegalArgumentException("Polygon must have at least 3 vertices, got " + vertices.size());
            }
            vertices = List.copyOf(vertices);
        }

        @Override
        public String kind() {
            return "polygon";
        }

        /** Standard even-odd ray-casting point-in-polygon test. */
        @Override
        public boolean contains(int x, int z) {
            boolean inside = false;
            int n = vertices.size();
            for (int i = 0, j = n - 1; i < n; j = i++) {
                Vertex vi = vertices.get(i);
                Vertex vj = vertices.get(j);
                boolean edgeCrossesRay = (vi.z() > z) != (vj.z() > z)
                        && x < (double) (vj.x() - vi.x()) * (z - vi.z()) / (vj.z() - vi.z()) + vi.x();
                if (edgeCrossesRay) {
                    inside = !inside;
                }
            }
            return inside;
        }

        @Override
        public ChunkPos minChunk() {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            for (Vertex v : vertices) {
                minX = Math.min(minX, v.x());
                minZ = Math.min(minZ, v.z());
            }
            return new ChunkPos(minX >> 4, minZ >> 4);
        }

        @Override
        public ChunkPos maxChunk() {
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (Vertex v : vertices) {
                maxX = Math.max(maxX, v.x());
                maxZ = Math.max(maxZ, v.z());
            }
            return new ChunkPos(maxX >> 4, maxZ >> 4);
        }

        public record Vertex(int x, int z) {
            static final Codec<Vertex> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.fieldOf("x").forGetter(Vertex::x),
                    Codec.INT.fieldOf("z").forGetter(Vertex::z)
            ).apply(i, Vertex::new));
        }

        /**
         * Convex hull of {@code points} via Andrew's monotone chain algorithm — takes the (likely
         * dozens of) corner points of a structure's individual piece bounding boxes and reduces
         * them to a small, storage-cheap footprint. Returns a plain {@link Geometry}, not
         * necessarily a {@code Polygon}: if the hull degenerates to fewer than 3 distinct vertices
         * (all input points collinear — practically never happens for a real structure, but a real
         * edge case rather than an assumed-impossible one), falls back to a {@link Bounds}
         * covering the input instead of constructing an invalid polygon.
         */
        public static Geometry convexHull(List<Vertex> points) {
            if (points.isEmpty()) {
                throw new IllegalArgumentException("convexHull requires at least one point");
            }

            List<Vertex> sorted = points.stream()
                    .distinct()
                    .sorted(Comparator.comparingInt(Vertex::x).thenComparingInt(Vertex::z))
                    .toList();

            if (sorted.size() < 3) {
                return boundsOf(sorted);
            }

            int n = sorted.size();
            Vertex[] hull = new Vertex[2 * n];
            int k = 0;
            for (Vertex p : sorted) {
                while (k >= 2 && cross(hull[k - 2], hull[k - 1], p) <= 0) {
                    k--;
                }
                hull[k++] = p;
            }
            int lower = k + 1;
            for (int i = n - 2; i >= 0; i--) {
                Vertex p = sorted.get(i);
                while (k >= lower && cross(hull[k - 2], hull[k - 1], p) <= 0) {
                    k--;
                }
                hull[k++] = p;
            }

            List<Vertex> result = new ArrayList<>(Arrays.asList(hull).subList(0, k - 1));
            return result.size() < 3 ? boundsOf(sorted) : new Polygon(result);
        }

        private static long cross(Vertex o, Vertex a, Vertex b) {
            return (long) (a.x() - o.x()) * (b.z() - o.z()) - (long) (a.z() - o.z()) * (b.x() - o.x());
        }

        private static Bounds boundsOf(List<Vertex> points) {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (Vertex v : points) {
                minX = Math.min(minX, v.x());
                minZ = Math.min(minZ, v.z());
                maxX = Math.max(maxX, v.x());
                maxZ = Math.max(maxZ, v.z());
            }
            return new Bounds(minX, minZ, maxX, maxZ);
        }
    }
}
