package com.github.cerealklla.cartographyr.geo;

/**
 * Whether a {@link Geometry.Polygon} (a Settlemynts plot's staked-out shape, live or finalized) is
 * big enough to fit a Blueprynts Construction Site, and a per-cell BLUE/GREEN/YELLOW classification
 * for the ghost-item overlay a Town Planner sees while sketching or moving a Construction Box.
 *
 * <p>Stateless and callable identically from server (enforcement) and client (rendering) -- only the
 * polygon itself needs to cross the network, never a raw cell grid, so the two can never disagree
 * about a plot up to thousands of cells across.
 *
 * <p>BLUE is a <b>union</b>, not one chosen square: every cell that is part of at least one valid
 * SMALL (15x15) or LARGE (50x50) placement anywhere in the polygon is BLUE, however many such
 * placements exist -- e.g. a 55x55 clear region is entirely BLUE, since every one of its cells
 * participates in some sliding placement of either size. GREEN is inside the polygon but not part of
 * any such placement (the buffer ring a Construction Box can actually stand on). YELLOW is outside
 * the polygon entirely.
 */
public final class PlotValidity {

    public static final int SMALL_SIZE = 15;
    public static final int LARGE_SIZE = 50;

    // A BLUE square must have this many extra rings of polygon-interior ground on every side beyond
    // its own SMALL_SIZE/LARGE_SIZE footprint -- user requirement, 2026-09-29: a plot's buildable area
    // needs a 1-block buffer from the plot boundary on all sides, not just barely fit edge-to-edge.
    public static final int REQUIRED_BUFFER = 1;

    private PlotValidity() {
    }

    public enum Cell {
        YELLOW, GREEN, BLUE
    }

    /** A classification window over {@code [minX..maxX] x [minZ..maxZ]} (inclusive), block-precise. */
    public record Grid(int minX, int minZ, int maxX, int maxZ, Cell[][] cells) {
        public Cell at(int x, int z) {
            int ix = x - minX;
            int iz = z - minZ;
            if (ix < 0 || iz < 0 || ix >= cells.length || iz >= cells[0].length) {
                return Cell.YELLOW;
            }
            return cells[ix][iz];
        }
    }

    /**
     * True the moment any 15x15 fits -- both the pass/fail bar for Finalize and sufficient on its own,
     * since a valid 50x50 always contains 15x15s too.
     */
    public static boolean hasValidArea(Geometry.Polygon polygon) {
        return hasArea(polygon, SMALL_SIZE);
    }

    /** True if a 50x50 fits anywhere -- used to filter Large-sized Blueprints out of a picker when a plot can't actually fit one. */
    public static boolean hasLargeArea(Geometry.Polygon polygon) {
        return hasArea(polygon, LARGE_SIZE);
    }

    private static boolean hasArea(Geometry.Polygon polygon, int size) {
        int[] b = blockBounds(polygon);
        int minX = b[0];
        int minZ = b[1];
        int maxX = b[2];
        int maxZ = b[3];
        int outerSize = size + 2 * REQUIRED_BUFFER;
        int w = maxX - minX + 1;
        int h = maxZ - minZ + 1;
        if (w < outerSize || h < outerSize) {
            return false;
        }
        int[][] dp = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < h; z++) {
                if (!polygon.contains(minX + x, minZ + z)) {
                    continue;
                }
                dp[x][z] = (x == 0 || z == 0) ? 1 : 1 + Math.min(dp[x - 1][z], Math.min(dp[x][z - 1], dp[x - 1][z - 1]));
                if (dp[x][z] >= outerSize) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Classifies every cell in {@code [minX..maxX] x [minZ..maxZ]} (the caller's render-radius window,
     * not necessarily the whole plot). Internally runs the maximal-square DP over the polygon's own
     * full bounding box regardless of the requested window, since a qualifying square corner outside
     * the window can still cover cells inside it.
     */
    public static Grid classify(Geometry.Polygon polygon, int minX, int minZ, int maxX, int maxZ) {
        int[] pb = blockBounds(polygon);
        int fMinX = Math.min(pb[0], minX);
        int fMinZ = Math.min(pb[1], minZ);
        int fMaxX = Math.max(pb[2], maxX);
        int fMaxZ = Math.max(pb[3], maxZ);
        int fw = fMaxX - fMinX + 1;
        int fh = fMaxZ - fMinZ + 1;

        boolean[][] inside = new boolean[fw][fh];
        for (int x = 0; x < fw; x++) {
            for (int z = 0; z < fh; z++) {
                inside[x][z] = polygon.contains(fMinX + x, fMinZ + z);
            }
        }

        int[][] dp = new int[fw][fh];
        for (int x = 0; x < fw; x++) {
            for (int z = 0; z < fh; z++) {
                if (!inside[x][z]) {
                    continue;
                }
                dp[x][z] = (x == 0 || z == 0) ? 1 : 1 + Math.min(dp[x - 1][z], Math.min(dp[x][z - 1], dp[x - 1][z - 1]));
            }
        }

        // 2D difference array: union every qualifying square's footprint in O(1) per corner, resolved
        // via a 2D prefix sum afterward, rather than naively stamping n^2 cells per corner. Each
        // qualifying square requires REQUIRED_BUFFER extra rings of polygon-interior ground beyond its
        // own size on every side (dp[x][z] is checked against the larger outerSize), but only the
        // inner size x size square -- inset by REQUIRED_BUFFER from the qualifying corner -- is
        // actually marked BLUE, so the buffer ring itself stays GREEN (buildable-adjacent, not
        // buildable) rather than being counted as part of the buildable area.
        int[][] diff = new int[fw + 1][fh + 1];
        for (int size : new int[]{SMALL_SIZE, LARGE_SIZE}) {
            int outerSize = size + 2 * REQUIRED_BUFFER;
            for (int x = 0; x < fw; x++) {
                for (int z = 0; z < fh; z++) {
                    if (dp[x][z] < outerSize) {
                        continue;
                    }
                    int x0 = x - size - REQUIRED_BUFFER + 1;
                    int z0 = z - size - REQUIRED_BUFFER + 1;
                    int x1 = x - REQUIRED_BUFFER;
                    int z1 = z - REQUIRED_BUFFER;
                    diff[x0][z0] += 1;
                    diff[x0][z1 + 1] -= 1;
                    diff[x1 + 1][z0] -= 1;
                    diff[x1 + 1][z1 + 1] += 1;
                }
            }
        }
        for (int x = 0; x <= fw; x++) {
            for (int z = 1; z <= fh; z++) {
                diff[x][z] += diff[x][z - 1];
            }
        }
        for (int z = 0; z <= fh; z++) {
            for (int x = 1; x <= fw; x++) {
                diff[x][z] += diff[x - 1][z];
            }
        }

        int ow = maxX - minX + 1;
        int oh = maxZ - minZ + 1;
        Cell[][] result = new Cell[ow][oh];
        for (int x = 0; x < ow; x++) {
            for (int z = 0; z < oh; z++) {
                int fx = (minX + x) - fMinX;
                int fz = (minZ + z) - fMinZ;
                if (!inside[fx][fz]) {
                    result[x][z] = Cell.YELLOW;
                } else if (diff[fx][fz] > 0) {
                    result[x][z] = Cell.BLUE;
                } else {
                    result[x][z] = Cell.GREEN;
                }
            }
        }
        return new Grid(minX, minZ, maxX, maxZ, result);
    }

    /** {@code [minX, minZ, maxX, maxZ]} over the polygon's own vertices, block-precise (not chunk-shifted). */
    private static int[] blockBounds(Geometry.Polygon polygon) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Geometry.Polygon.Vertex v : polygon.vertices()) {
            minX = Math.min(minX, v.x());
            minZ = Math.min(minZ, v.z());
            maxX = Math.max(maxX, v.x());
            maxZ = Math.max(maxZ, v.z());
        }
        return new int[]{minX, minZ, maxX, maxZ};
    }
}
