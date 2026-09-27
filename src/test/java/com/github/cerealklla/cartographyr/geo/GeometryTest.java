package com.github.cerealklla.cartographyr.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.serialization.DataResult;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

class GeometryTest {

    @Test
    void regionRejectsEmptyCellSet() {
        assertThrows(IllegalArgumentException.class, () -> new Geometry.Region(Set.of()));
    }

    @Test
    void regionContainsChecksCellMembershipAtChunkGranularity() {
        // Cell (0,0) covers blocks x/z 0..15
        Geometry.Region region = new Geometry.Region(Set.of(ChunkPos.pack(0, 0)));

        assertTrue(region.contains(0, 0));
        assertTrue(region.contains(15, 15));
        assertFalse(region.contains(16, 0));
        assertFalse(region.contains(-1, 0));
    }

    @Test
    void regionMinMaxChunkIsBoundingBoxOverCells() {
        Geometry.Region region = new Geometry.Region(Set.of(
                ChunkPos.pack(2, -3),
                ChunkPos.pack(5, 1),
                ChunkPos.pack(-1, 4)
        ));

        assertEquals(new ChunkPos(-1, -3), region.minChunk());
        assertEquals(new ChunkPos(5, 4), region.maxChunk());
    }

    @Test
    void regionRoundTripsThroughCodec() {
        Geometry.Region original = new Geometry.Region(Set.of(
                ChunkPos.pack(0, 0), ChunkPos.pack(1, 0), ChunkPos.pack(0, 1)
        ));

        DataResult<Tag> encodeResult = Geometry.CODEC.encodeStart(NbtOps.INSTANCE, original);
        Tag encoded = encodeResult.result().orElseThrow(() -> new AssertionError("Encode failed: " + encodeResult.error()));

        DataResult<Geometry> decodeResult = Geometry.CODEC.parse(NbtOps.INSTANCE, encoded);
        Geometry decoded = decodeResult.result().orElseThrow(() -> new AssertionError("Decode failed: " + decodeResult.error()));

        assertEquals(original, decoded);
    }

    @Test
    void polygonRejectsFewerThanThreeVertices() {
        assertThrows(IllegalArgumentException.class, () -> new Geometry.Polygon(List.of(
                new Geometry.Polygon.Vertex(0, 0), new Geometry.Polygon.Vertex(1, 1)
        )));
    }

    @Test
    void polygonContainsUsesRayCasting() {
        // A 10x10 square, (0,0) -> (10,0) -> (10,10) -> (0,10)
        Geometry.Polygon square = new Geometry.Polygon(List.of(
                new Geometry.Polygon.Vertex(0, 0),
                new Geometry.Polygon.Vertex(10, 0),
                new Geometry.Polygon.Vertex(10, 10),
                new Geometry.Polygon.Vertex(0, 10)
        ));

        assertTrue(square.contains(5, 5));
        assertTrue(square.contains(9, 5));
        assertFalse(square.contains(11, 5));
        assertFalse(square.contains(-1, -1));
        assertFalse(square.contains(20, 20));
    }

    @Test
    void polygonMinMaxChunkIsBoundingBoxOverVertices() {
        Geometry.Polygon polygon = new Geometry.Polygon(List.of(
                new Geometry.Polygon.Vertex(2, -3),
                new Geometry.Polygon.Vertex(20, 17),
                new Geometry.Polygon.Vertex(-5, 30)
        ));

        assertEquals(new ChunkPos(-1, -1), polygon.minChunk());
        assertEquals(new ChunkPos(1, 1), polygon.maxChunk());
    }

    @Test
    void coveringBlocksIncludesEveryCornerOfARectangle() {
        // A 5x5: corner blocks at 10..14 inclusive on each axis (matches a user-reported plot-stake
        // test case, 2026-09-27) -- every one of the four corner blocks, not just the near two under
        // raw ray-casting, must resolve as contained.
        Geometry.Polygon polygon = Geometry.Polygon.coveringBlocks(List.of(
                new Geometry.Polygon.Vertex(10, 10),
                new Geometry.Polygon.Vertex(14, 10),
                new Geometry.Polygon.Vertex(14, 14),
                new Geometry.Polygon.Vertex(10, 14)
        ));

        for (int x = 10; x <= 14; x++) {
            for (int z = 10; z <= 14; z++) {
                assertTrue(polygon.contains(x, z), "Expected block (" + x + "," + z + ") to be contained");
            }
        }
        assertFalse(polygon.contains(9, 12));
        assertFalse(polygon.contains(15, 12));
        assertFalse(polygon.contains(12, 9));
        assertFalse(polygon.contains(12, 15));
        assertFalse(polygon.contains(15, 15));
    }

    @Test
    void coveringBlocksRejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> Geometry.Polygon.coveringBlocks(List.of()));
    }

    @Test
    void coveringBlocksHandlesDiagonalEdgesNotJustAxisAlignedRectangles() {
        // An octagon (four axis-aligned edges, four 45-degree diagonal edges) -- regression coverage
        // for the 2026-09-27 rewrite from a centroid-relative push (only correct for rectangles) to a
        // local-outward-normal push (edge-tangent-aware, fixes a live "lumpy diagonal wall" report on
        // a many-vertex settlement shape). Every vertex's own block must still be contained, same
        // requirement as the rectangle case, now proven for diagonal edges too.
        List<Geometry.Polygon.Vertex> stakeBlocks = List.of(
                new Geometry.Polygon.Vertex(3, 0), new Geometry.Polygon.Vertex(9, 0),
                new Geometry.Polygon.Vertex(12, 3), new Geometry.Polygon.Vertex(12, 9),
                new Geometry.Polygon.Vertex(9, 12), new Geometry.Polygon.Vertex(3, 12),
                new Geometry.Polygon.Vertex(0, 9), new Geometry.Polygon.Vertex(0, 3));
        Geometry.Polygon polygon = Geometry.Polygon.coveringBlocks(stakeBlocks);

        for (Geometry.Polygon.Vertex v : stakeBlocks) {
            assertTrue(polygon.contains(v.x(), v.z()), "Expected stake block (" + v.x() + "," + v.z() + ") to be contained");
        }
        assertTrue(polygon.contains(6, 6)); // center, clearly interior
        assertFalse(polygon.contains(-2, 6));
        assertFalse(polygon.contains(6, -2));
        assertFalse(polygon.contains(14, 6));
        assertFalse(polygon.contains(6, 14));
    }

    @Test
    void coveringBlocksExcludesUnstakedNotchBlocksAtAReflexCorner() {
        // An L-shape: a 4x2 bottom strip plus a 2x2 upper-left strip, meeting at a reflex (concave)
        // corner at (2,2). Regression coverage for the 2026-09-27 rewrite from a per-vertex outward
        // push (verified by hand to leak block (3,2) -- an un-staked notch block -- into the covered
        // set) to rasterize-then-contour. The reflex vertex's OWN block must still be included (a
        // stake is never a special excluded case), but nothing in the excluded notch should be.
        List<Geometry.Polygon.Vertex> stakes = List.of(
                new Geometry.Polygon.Vertex(0, 0), new Geometry.Polygon.Vertex(4, 0),
                new Geometry.Polygon.Vertex(4, 2), new Geometry.Polygon.Vertex(2, 2),
                new Geometry.Polygon.Vertex(2, 4), new Geometry.Polygon.Vertex(0, 4));
        Geometry.Polygon polygon = Geometry.Polygon.coveringBlocks(stakes);

        // Bottom strip (x 0..3, z 0..1) and upper-left strip (x 0..1, z 2..3).
        for (int x = 0; x <= 3; x++) {
            for (int z = 0; z <= 1; z++) {
                assertTrue(polygon.contains(x, z), "Expected bottom strip block (" + x + "," + z + ") to be contained");
            }
        }
        for (int x = 0; x <= 1; x++) {
            for (int z = 2; z <= 3; z++) {
                assertTrue(polygon.contains(x, z), "Expected upper strip block (" + x + "," + z + ") to be contained");
            }
        }
        assertTrue(polygon.contains(2, 2), "The reflex vertex's own stake block must be contained");
        assertFalse(polygon.contains(3, 2), "An un-staked notch block must not leak into the covered set");
        assertFalse(polygon.contains(2, 3), "An un-staked notch block must not leak into the covered set");
        assertFalse(polygon.contains(3, 3), "An un-staked notch block must not leak into the covered set");
    }

    @Test
    void coveringBlocksHandlesACShapeWithoutSelfIntersecting() {
        // A thick "C": an outer 7x7 square with a 3-wide notch bitten out of the right side, stakes
        // placed in a genuine walked path (not star-shaped from the shape's own centroid, which sits
        // in the notch's open mouth) -- exactly the case angular sorting cannot handle, per the
        // user's original question ("would a C-shaped stake layout come out as a C, or circular?").
        List<Geometry.Polygon.Vertex> stakes = List.of(
                new Geometry.Polygon.Vertex(0, 0), new Geometry.Polygon.Vertex(6, 0),
                new Geometry.Polygon.Vertex(6, 2), new Geometry.Polygon.Vertex(3, 2),
                new Geometry.Polygon.Vertex(3, 4), new Geometry.Polygon.Vertex(6, 4),
                new Geometry.Polygon.Vertex(6, 6), new Geometry.Polygon.Vertex(0, 6));
        Geometry.Polygon polygon = Geometry.Polygon.coveringBlocks(stakes);

        for (Geometry.Polygon.Vertex v : stakes) {
            assertTrue(polygon.contains(v.x(), v.z()), "Expected stake block (" + v.x() + "," + v.z() + ") to be contained");
        }
        assertTrue(polygon.contains(1, 3)); // deep in the C's own body, left side
        assertFalse(polygon.contains(5, 3)); // inside the notch (the C's open mouth) -- must stay excluded
        assertFalse(polygon.contains(8, 3)); // clearly outside
    }

    @Test
    void outerRingSitsOutsideANotchWithoutCuttingAcrossIt() {
        List<Geometry.Polygon.Vertex> stakes = List.of(
                new Geometry.Polygon.Vertex(0, 0), new Geometry.Polygon.Vertex(4, 0),
                new Geometry.Polygon.Vertex(4, 2), new Geometry.Polygon.Vertex(2, 2),
                new Geometry.Polygon.Vertex(2, 4), new Geometry.Polygon.Vertex(0, 4));
        Geometry.Polygon polygon = Geometry.Polygon.coveringBlocks(stakes);
        List<Geometry.Polygon.Vertex> ring = Geometry.Polygon.outerRing(polygon);

        for (Geometry.Polygon.Vertex v : ring) {
            assertFalse(polygon.contains(v.x(), v.z()), "Wall ring block (" + v.x() + "," + v.z() + ") must not overlap the covered area");
        }
        // The ring must actually hug the notch, not skip past it -- (3,2) sits directly outside the
        // covered bottom strip's top edge, right next to the reflex corner.
        assertTrue(ring.contains(new Geometry.Polygon.Vertex(3, 2)));
    }

    @Test
    void expandedByFollowsAConcaveShapeInsteadOfCuttingAcrossItsNotch() {
        // A "U" shape opening upward: a wide notch (x 6..13, z 4..19) between two arms (x 0..5 and
        // x 14..19). Regression coverage for a live playtest report -- the plot buffer previously
        // used radial scale-from-centroid, which cuts straight across a concave notch like this
        // instead of following the shape's own boundary.
        List<Geometry.Polygon.Vertex> uShape = List.of(
                new Geometry.Polygon.Vertex(0, 0), new Geometry.Polygon.Vertex(20, 0),
                new Geometry.Polygon.Vertex(20, 20), new Geometry.Polygon.Vertex(14, 20),
                new Geometry.Polygon.Vertex(14, 4), new Geometry.Polygon.Vertex(6, 4),
                new Geometry.Polygon.Vertex(6, 20), new Geometry.Polygon.Vertex(0, 20));
        Geometry.Polygon polygon = Geometry.Polygon.coveringBlocks(uShape);

        Geometry.Polygon buffered2 = Geometry.Polygon.expandedBy(polygon, 2);
        Geometry.Polygon buffered5 = Geometry.Polygon.expandedBy(polygon, 5);

        // Deep in the notch's open mouth (chebyshev distance 4 from the nearest arm) -- a buffer of
        // 2 must not bridge across it, but one wide enough (5) eventually reaches across.
        assertFalse(buffered2.contains(9, 10), "A small buffer must not cut across the notch");
        assertTrue(buffered5.contains(9, 10));
        // The buffer must still contain everything the original shape did.
        assertTrue(buffered2.contains(2, 10));
        assertTrue(buffered2.contains(17, 10));
    }

    @Test
    void supercoverLineHasNoDiagonalGap() {
        List<Geometry.Polygon.Vertex> cells = Geometry.Polygon.supercoverLine(
                new Geometry.Polygon.Vertex(0, 0), new Geometry.Polygon.Vertex(3, 3));

        for (int i = 1; i < cells.size(); i++) {
            Geometry.Polygon.Vertex a = cells.get(i - 1);
            Geometry.Polygon.Vertex b = cells.get(i);
            int stepX = Math.abs(b.x() - a.x());
            int stepZ = Math.abs(b.z() - a.z());
            assertTrue(stepX <= 1 && stepZ <= 1 && stepX + stepZ >= 1, "Consecutive cells must be orthogonally or diagonally adjacent");
        }
        assertTrue(cells.contains(new Geometry.Polygon.Vertex(0, 0)));
        assertTrue(cells.contains(new Geometry.Polygon.Vertex(3, 3)));
    }

    @Test
    void polygonRoundTripsThroughCodec() {
        Geometry.Polygon original = new Geometry.Polygon(List.of(
                new Geometry.Polygon.Vertex(0, 0),
                new Geometry.Polygon.Vertex(10, 0),
                new Geometry.Polygon.Vertex(5, 10)
        ));

        DataResult<Tag> encodeResult = Geometry.CODEC.encodeStart(NbtOps.INSTANCE, original);
        Tag encoded = encodeResult.result().orElseThrow(() -> new AssertionError("Encode failed: " + encodeResult.error()));

        DataResult<Geometry> decodeResult = Geometry.CODEC.parse(NbtOps.INSTANCE, encoded);
        Geometry decoded = decodeResult.result().orElseThrow(() -> new AssertionError("Decode failed: " + decodeResult.error()));

        assertEquals(original, decoded);
    }

    @Test
    void convexHullOfSquarePlusInteriorPointDropsTheInteriorPoint() {
        Geometry.Polygon.Vertex bottomLeft = new Geometry.Polygon.Vertex(0, 0);
        Geometry.Polygon.Vertex bottomRight = new Geometry.Polygon.Vertex(10, 0);
        Geometry.Polygon.Vertex topRight = new Geometry.Polygon.Vertex(10, 10);
        Geometry.Polygon.Vertex topLeft = new Geometry.Polygon.Vertex(0, 10);
        Geometry.Polygon.Vertex center = new Geometry.Polygon.Vertex(5, 5);

        Geometry hull = Geometry.Polygon.convexHull(List.of(bottomLeft, bottomRight, topRight, topLeft, center));

        if (!(hull instanceof Geometry.Polygon polygon)) {
            fail("Expected a Polygon, got " + hull);
            return;
        }
        assertEquals(4, polygon.vertices().size());
        assertEquals(Set.of(bottomLeft, bottomRight, topRight, topLeft), Set.copyOf(polygon.vertices()));
    }

    @Test
    void convexHullOfCollinearPointsFallsBackToBounds() {
        Geometry hull = Geometry.Polygon.convexHull(List.of(
                new Geometry.Polygon.Vertex(0, 0),
                new Geometry.Polygon.Vertex(5, 0),
                new Geometry.Polygon.Vertex(10, 0)
        ));

        if (!(hull instanceof Geometry.Bounds bounds)) {
            fail("Expected Bounds fallback, got " + hull);
            return;
        }
        assertEquals(new Geometry.Bounds(0, 0, 10, 0), bounds);
    }
}
