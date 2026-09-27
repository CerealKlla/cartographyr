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

    /**
     * A single representative block position for this geometry -- the midpoint of {@link
     * #minChunk()}/{@link #maxChunk()}'s middle blocks. Not exact for irregular shapes (a {@link
     * Region}'s bounding-box center may not even be one of its own cells), but good enough for
     * coarse distance checks between entities (see {@code natural.NaturalRegionDiscovery}'s
     * duplicate-name-avoidance radius check, added 2026-09-26).
     */
    default int centerBlockX() {
        return (minChunk().getMiddleBlockX() + maxChunk().getMiddleBlockX()) / 2;
    }

    default int centerBlockZ() {
        return (minChunk().getMiddleBlockZ() + maxChunk().getMiddleBlockZ()) / 2;
    }

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

        /** A unit direction, used by {@link #localOutwardNormals} -- not itself a location. */
        public record Normal(double x, double z) {
        }

        /**
         * The local outward-pointing direction at each vertex -- the (renormalized) average of the
         * two adjacent edges' own outward unit normals. A per-vertex, edge-tangent-aware notion of
         * "which way is outward," used by {@link #coveringBlocks} (and by Settlemynts' wall-offset
         * generation, which needs the same primitive one polygon later). Deliberately **not** "vertex
         * minus the whole polygon's centroid" -- that comparison is only correct for axis-aligned
         * rectangles; for a many-vertex, roughly circular polygon (e.g. a settlement fitted from many
         * perimeter stakes) it pushes some vertices in a direction that doesn't match their own local
         * edge tangents at all, producing a visibly lumpy/humped result along diagonal runs (found via
         * live playtest, 2026-09-27, see decisions.md same date). Winding (CW/CCW) is detected from
         * the signed area so the normal always points away from the polygon's own interior regardless
         * of vertex order.
         */
        public static List<Normal> localOutwardNormals(List<Vertex> vertices) {
            int n = vertices.size();
            double signedArea2 = 0;
            for (int i = 0; i < n; i++) {
                Vertex a = vertices.get(i);
                Vertex b = vertices.get((i + 1) % n);
                signedArea2 += (double) a.x() * b.z() - (double) b.x() * a.z();
            }
            boolean ccw = signedArea2 > 0;

            double[] edgeNx = new double[n];
            double[] edgeNz = new double[n];
            for (int i = 0; i < n; i++) {
                Vertex a = vertices.get(i);
                Vertex b = vertices.get((i + 1) % n);
                double dx = b.x() - a.x();
                double dz = b.z() - a.z();
                double len = Math.hypot(dx, dz);
                if (len < 1.0e-9) {
                    continue; // Degenerate (repeated vertex) edge -- leaves both components 0, handled below.
                }
                double ux = dx / len;
                double uz = dz / len;
                edgeNx[i] = ccw ? uz : -uz;
                edgeNz[i] = ccw ? -ux : ux;
            }

            List<Normal> result = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                int prevEdge = (i - 1 + n) % n;
                double nx = edgeNx[prevEdge] + edgeNx[i];
                double nz = edgeNz[prevEdge] + edgeNz[i];
                double len = Math.hypot(nx, nz);
                if (len < 1.0e-9) {
                    // A 180-degree fold (the two adjacent edges point directly opposite) -- fall back
                    // to whichever single adjacent edge normal is non-zero; genuinely ambiguous, rare.
                    nx = edgeNx[i] != 0 || edgeNz[i] != 0 ? edgeNx[i] : edgeNx[prevEdge];
                    nz = edgeNx[i] != 0 || edgeNz[i] != 0 ? edgeNz[i] : edgeNz[prevEdge];
                    len = Math.hypot(nx, nz);
                }
                result.add(len < 1.0e-9 ? new Normal(0, 0) : new Normal(nx / len, nz / len));
            }
            return result;
        }

        /**
         * Builds a polygon that fully covers every listed block, not just the infinitesimal point at
         * each vertex's coordinate. {@code contains}'s even-odd ray-casting test is defined over
         * continuous space, but callers here (a placed stake, a structure corner) mean "this whole
         * block," which occupies the continuous square from {@code (x,z)} to {@code (x+1,z+1)}. A raw
         * {@code new Polygon(vertices)} only reaches each vertex's near corner, so a block on the
         * outward side of the shape (e.g. the far corner of a rectangle) tests as outside its own
         * polygon under ray-casting's boundary rules. This factory pushes each vertex's coordinate out
         * to the far edge of its own block on whichever side its {@link #localOutwardNormals} own
         * sign sits, per axis, so the resulting polygon's continuous extent covers every listed block
         * exactly, corners included. Exact for axis-aligned rectangles; a reasonable, edge-tangent-
         * aware approximation otherwise (replaced a cruder centroid-relative version, 2026-09-27, see
         * decisions.md same date -- that version was only correct for rectangles).
         */
        public static Polygon coveringBlocks(List<Vertex> blocks) {
            if (blocks.isEmpty()) {
                throw new IllegalArgumentException("Cannot build a polygon from zero blocks");
            }
            List<Normal> normals = localOutwardNormals(blocks);
            List<Vertex> covering = new ArrayList<>(blocks.size());
            for (int i = 0; i < blocks.size(); i++) {
                Vertex v = blocks.get(i);
                Normal normal = normals.get(i);
                int x = normal.x() >= 0 ? v.x() + 1 : v.x();
                int z = normal.z() >= 0 ? v.z() + 1 : v.z();
                covering.add(new Vertex(x, z));
            }
            return new Polygon(covering);
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
