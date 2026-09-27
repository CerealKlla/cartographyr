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
