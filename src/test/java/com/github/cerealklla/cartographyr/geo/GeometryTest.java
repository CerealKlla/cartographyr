package com.github.cerealklla.cartographyr.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
