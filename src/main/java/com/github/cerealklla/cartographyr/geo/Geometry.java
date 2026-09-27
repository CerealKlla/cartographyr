package com.github.cerealklla.cartographyr.geo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

        /**
         * Builds a polygon that fully covers every listed block, not just the infinitesimal point at
         * each vertex's coordinate. {@code contains}'s even-odd ray-casting test is defined over
         * continuous space, but callers here (stakes placed in order around a settlement/plot) mean
         * "every one of these whole blocks, and everything traced between them, is inside" -- a raw
         * {@code new Polygon(vertices)} only reaches each vertex's own near corner, so blocks on the
         * outward side of the shape (e.g. the far corner of a rectangle) test as outside their own
         * polygon under ray-casting's boundary rules.
         *
         * <p>{@code stakesInPlacementOrder} must be listed in the order the stakes were actually
         * placed (the real path a player walked), <b>not</b> re-sorted by angle around a centroid --
         * angular sorting only produces a valid shape when the polygon is star-shaped from its
         * centroid (true for a rectangle/blob, false for a C-shape, ring, or anything whose centroid
         * sits outside the material; see decisions.md 2026-09-27).
         *
         * <p>Implementation is rasterize-then-contour, not a per-vertex outward push: an earlier
         * version pushed each vertex along its local edge-tangent bisector, which is exact for convex
         * polygons but was hand-verified wrong at reflex (concave) vertices -- the push can bleed
         * extra, un-staked blocks into what should stay an excluded notch (see decisions.md 2026-09-27
         * for the worked L-shape example). Instead: (1) every stake's own block is always covered;
         * (2) each edge whose *both* endpoints are convex gets a "supercover" (thick, gap-free) line
         * walk marking every block it passes over -- edges touching a reflex vertex get no such stamp,
         * which is exactly what avoids bleeding into a notch; (3) every block in the stakes' bounding
         * box whose *center* tests inside the raw (unpushed) placement-order polygon is also covered
         * -- a block center is never exactly on an edge/vertex the way a corner is, so this step never
         * hits ray-casting's boundary ambiguity. The resulting covered-block set's own outer boundary
         * is then traced into an ordinary vertex list (standard grid-boundary contour tracing, merging
         * collinear runs) -- {@code contains()} itself is completely unchanged; only how a polygon's
         * vertex list gets built from stakes changes.
         *
         * <p>Known limitation: assumes the placement order traces a simple (non-self-crossing) loop.
         * If it doesn't, or if it fully encloses a hole (stakes placed all the way around a ring, back
         * to the start), only the single largest-area traced boundary is kept -- {@link Polygon} has
         * no hole support, so a genuinely donut-shaped input's interior is treated as solid rather than
         * excluded. Good enough for the shapes this mod actually needs; flagged rather than silently
         * assumed away.
         */
        public static Polygon coveringBlocks(List<Vertex> stakesInPlacementOrder) {
            if (stakesInPlacementOrder.size() < 3) {
                throw new IllegalArgumentException("Polygon must have at least 3 vertices, got " + stakesInPlacementOrder.size());
            }
            Set<Long> covered = coveredBlockKeys(stakesInPlacementOrder);
            return new Polygon(traceOuterBoundary(covered));
        }

        /**
         * The block ring immediately outside {@code polygon}'s own footprint -- every block {@code
         * polygon} doesn't contain but that's orthogonally adjacent to one it does. Derived directly
         * from {@code polygon.contains()} itself (a plain bounding-box scan plus a neighbor check), so
         * it's correct at concave notches "for free" -- no vertex-offset approximation involved, unlike
         * the per-vertex push this replaced (see {@code coveringBlocks}' own doc, 2026-09-27). Callers
         * generating this for a shape that isn't registered with Cartographyr yet (still being staked
         * out, not yet finalized) should build the polygon via {@link #coveringBlocks} first, then pass
         * the result here -- the two together are what a settlement/plot wall, or a live fence-post
         * preview while staking, should trace.
         */
        public static List<Vertex> outerRing(Polygon polygon) {
            Set<Long> covered = coveredSetFromContains(polygon);
            Set<Long> ring = new LinkedHashSet<>();
            for (long cellKey : covered) {
                int x = unpackX(cellKey);
                int z = unpackZ(cellKey);
                addIfUncovered(covered, ring, x - 1, z);
                addIfUncovered(covered, ring, x + 1, z);
                addIfUncovered(covered, ring, x, z - 1);
                addIfUncovered(covered, ring, x, z + 1);
            }
            List<Vertex> result = new ArrayList<>(ring.size());
            for (long key : ring) {
                result.add(new Vertex(unpackX(key), unpackZ(key)));
            }
            return result;
        }

        /**
         * A version of {@code polygon} grown outward by {@code blocks}, following its own shape --
         * for a buffer/padding zone (e.g. a plot's "Town Proper" or a settlement's outer "No Man's
         * Land") that must hug a concave shape's real boundary rather than cut across an indentation.
         * Grown via 8-connected (king-move) multi-source expansion from every covered block, so it
         * only ever spreads from blocks the shape actually occupies -- unlike the older radial-
         * scale-from-centroid technique this replaced (2026-09-27, live playtest: a C-shaped plot's
         * buffer cut straight across the C's open notch instead of following it, since scaling every
         * vertex away from one shared center point doesn't know about the shape's own concavity at
         * all). 8-connectivity gives a reasonably round/octagonal buffer instead of a diamond
         * (4-connectivity's Manhattan-distance artifact) -- a deliberate, precedent-consistent
         * approximation of a true circular offset, not an exact one.
         */
        public static Polygon expandedBy(Polygon polygon, int blocks) {
            Set<Long> all = coveredSetFromContains(polygon);
            Set<Long> frontier = all;
            for (int layer = 0; layer < blocks; layer++) {
                Set<Long> next = new HashSet<>();
                for (long cellKey : frontier) {
                    int x = unpackX(cellKey);
                    int z = unpackZ(cellKey);
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dz == 0) {
                                continue;
                            }
                            long neighbor = packBlock(x + dx, z + dz);
                            if (!all.contains(neighbor)) {
                                next.add(neighbor);
                            }
                        }
                    }
                }
                all.addAll(next);
                frontier = next;
            }
            return new Polygon(traceOuterBoundary(all));
        }

        private static Set<Long> coveredSetFromContains(Polygon polygon) {
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (Vertex v : polygon.vertices()) {
                minX = Math.min(minX, v.x());
                maxX = Math.max(maxX, v.x());
                minZ = Math.min(minZ, v.z());
                maxZ = Math.max(maxZ, v.z());
            }
            Set<Long> covered = new HashSet<>();
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (polygon.contains(x, z)) {
                        covered.add(packBlock(x, z));
                    }
                }
            }
            return covered;
        }

        private static void addIfUncovered(Set<Long> covered, Set<Long> ring, int x, int z) {
            long key = packBlock(x, z);
            if (!covered.contains(key)) {
                ring.add(key);
            }
        }

        /**
         * The set of blocks {@code coveringBlocks} considers part of the shape, keyed by {@link
         * #packBlock}. Package-visible pure logic, split out from {@code coveringBlocks} so it can be
         * unit-tested against the covered set directly rather than only through a traced-and-rounded
         * final polygon.
         */
        static Set<Long> coveredBlockKeys(List<Vertex> stakesInPlacementOrder) {
            int n = stakesInPlacementOrder.size();
            boolean[] reflex = classifyReflex(stakesInPlacementOrder);
            Set<Long> covered = new HashSet<>();
            for (Vertex v : stakesInPlacementOrder) {
                covered.add(packBlock(v.x(), v.z()));
            }
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                if (!reflex[i] && !reflex[j]) {
                    Vertex a = stakesInPlacementOrder.get(i);
                    Vertex b = stakesInPlacementOrder.get(j);
                    for (Vertex cell : supercoverLine(a, b)) {
                        covered.add(packBlock(cell.x(), cell.z()));
                    }
                }
            }
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (Vertex v : stakesInPlacementOrder) {
                minX = Math.min(minX, v.x());
                maxX = Math.max(maxX, v.x());
                minZ = Math.min(minZ, v.z());
                maxZ = Math.max(maxZ, v.z());
            }
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (containsPointDouble(stakesInPlacementOrder, x + 0.5, z + 0.5)) {
                        covered.add(packBlock(x, z));
                    }
                }
            }
            return covered;
        }

        /** {@code true} at index {@code i} if that vertex is reflex (concave), per the polygon's own winding (signed area). */
        private static boolean[] classifyReflex(List<Vertex> vertices) {
            int n = vertices.size();
            boolean ccw = signedArea(vertices) > 0;
            boolean[] reflex = new boolean[n];
            for (int i = 0; i < n; i++) {
                Vertex prev = vertices.get((i - 1 + n) % n);
                Vertex cur = vertices.get(i);
                Vertex next = vertices.get((i + 1) % n);
                long cross = (long) (cur.x() - prev.x()) * (next.z() - cur.z()) - (long) (cur.z() - prev.z()) * (next.x() - cur.x());
                reflex[i] = ccw ? cross < 0 : cross > 0;
            }
            return reflex;
        }

        private static double signedArea(List<Vertex> vertices) {
            double sum = 0.0;
            int n = vertices.size();
            for (int i = 0; i < n; i++) {
                Vertex a = vertices.get(i);
                Vertex b = vertices.get((i + 1) % n);
                sum += (double) a.x() * b.z() - (double) b.x() * a.z();
            }
            return sum;
        }

        /** Same even-odd ray-casting rule as {@link #contains(int, int)}, but over a double-precision query point -- used to test a block's own center, never its corners, so it never lands exactly on an edge or vertex. */
        private static boolean containsPointDouble(List<Vertex> vertices, double x, double z) {
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

        /**
         * Every block a straight line from {@code a} to {@code b} passes over, "supercover" style: at
         * a purely diagonal step, both of the two corner-adjacent cells are included (not just the
         * cell the line's center passes through), so the walk never has a gap a flood fill could leak
         * through. Public because Settlemynts' live "ghost fence post" preview (rendered while stakes
         * are still being placed, before a polygon can be finalized) needs the exact same primitive.
         */
        public static List<Vertex> supercoverLine(Vertex a, Vertex b) {
            List<Vertex> cells = new ArrayList<>();
            int dx = b.x() - a.x();
            int dz = b.z() - a.z();
            int nx = Math.abs(dx);
            int nz = Math.abs(dz);
            int signX = Integer.signum(dx);
            int signZ = Integer.signum(dz);
            int x = a.x();
            int z = a.z();
            cells.add(new Vertex(x, z));
            int ix = 0;
            int iz = 0;
            while (ix < nx || iz < nz) {
                long decision = (long) (1 + 2 * ix) * nz - (long) (1 + 2 * iz) * nx;
                if (decision == 0) {
                    x += signX;
                    cells.add(new Vertex(x, z));
                    z += signZ;
                    cells.add(new Vertex(x, z));
                    ix++;
                    iz++;
                } else if (decision < 0) {
                    x += signX;
                    cells.add(new Vertex(x, z));
                    ix++;
                } else {
                    z += signZ;
                    cells.add(new Vertex(x, z));
                    iz++;
                }
            }
            return cells;
        }

        private static long packBlock(int x, int z) {
            return ((long) x << 32) | (z & 0xFFFFFFFFL);
        }

        private static int unpackX(long key) {
            return (int) (key >> 32);
        }

        private static int unpackZ(long key) {
            return (int) key;
        }

        /**
         * Traces the outer boundary of {@code covered} (a set of {@link #packBlock}-keyed blocks) into
         * an ordered vertex list, via standard grid-boundary contour tracing: every covered cell emits
         * a boundary edge on each side whose neighbor isn't covered, oriented so the covered region is
         * on the edge's left (a CCW convention, verified against a single covered cell); edges chain
         * head-to-tail into one or more cycles, and collinear runs are merged. If more than one cycle
         * results (a self-crossing input, or a fully-enclosed hole), only the largest by area is kept
         * -- see {@code coveringBlocks}' own doc for why.
         */
        private static List<Vertex> traceOuterBoundary(Set<Long> covered) {
            Map<Long, long[]> nextVertex = new HashMap<>();
            for (long cellKey : covered) {
                int x = unpackX(cellKey);
                int z = unpackZ(cellKey);
                if (!covered.contains(packBlock(x, z - 1))) {
                    nextVertex.put(packBlock(x, z), new long[]{x + 1, z});
                }
                if (!covered.contains(packBlock(x + 1, z))) {
                    nextVertex.put(packBlock(x + 1, z), new long[]{x + 1, z + 1});
                }
                if (!covered.contains(packBlock(x, z + 1))) {
                    nextVertex.put(packBlock(x + 1, z + 1), new long[]{x, z + 1});
                }
                if (!covered.contains(packBlock(x - 1, z))) {
                    nextVertex.put(packBlock(x, z + 1), new long[]{x, z});
                }
            }

            Set<Long> visited = new HashSet<>();
            List<Vertex> best = null;
            double bestArea = -1;
            for (long startKey : nextVertex.keySet()) {
                if (visited.contains(startKey)) {
                    continue;
                }
                List<Vertex> cycle = new ArrayList<>();
                long current = startKey;
                do {
                    visited.add(current);
                    cycle.add(new Vertex(unpackX(current), unpackZ(current)));
                    long[] next = nextVertex.get(current);
                    current = packBlock((int) next[0], (int) next[1]);
                } while (current != startKey && !cycle.isEmpty() && cycle.size() < covered.size() * 4 + 8);
                double area = Math.abs(signedArea(cycle));
                if (area > bestArea) {
                    bestArea = area;
                    best = cycle;
                }
            }
            return simplifyCollinear(best);
        }

        private static List<Vertex> simplifyCollinear(List<Vertex> cycle) {
            int n = cycle.size();
            List<Vertex> result = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Vertex prev = cycle.get((i - 1 + n) % n);
                Vertex cur = cycle.get(i);
                Vertex next = cycle.get((i + 1) % n);
                long cross = (long) (cur.x() - prev.x()) * (next.z() - cur.z()) - (long) (cur.z() - prev.z()) * (next.x() - cur.x());
                if (cross != 0) {
                    result.add(cur);
                }
            }
            return result.isEmpty() ? cycle : result;
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
