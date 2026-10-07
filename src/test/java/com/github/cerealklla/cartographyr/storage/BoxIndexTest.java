package com.github.cerealklla.cartographyr.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.cerealklla.cartographyr.geo.Geometry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

class BoxIndexTest {

    @Test
    void rebuildThenCandidatesInRangeFindsBoxesWithinBoundingChunks() {
        BoxIndex index = new BoxIndex();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        index.rebuild(Map.of(
                a, GlobalPos.of(Level.OVERWORLD, new BlockPos(5, 64, 5)),
                b, GlobalPos.of(Level.OVERWORLD, new BlockPos(500, 64, 500))
        ));

        Geometry.Bounds nearOrigin = new Geometry.Bounds(0, 0, 15, 15);
        assertEquals(Set.of(a), index.candidatesInRange(Level.OVERWORLD, nearOrigin));
    }

    @Test
    void candidatesInRangeIsEmptyForUnknownDimension() {
        BoxIndex index = new BoxIndex();
        index.rebuild(Map.of(UUID.randomUUID(), GlobalPos.of(Level.OVERWORLD, new BlockPos(5, 64, 5))));

        Geometry.Bounds anywhere = new Geometry.Bounds(-1000, -1000, 1000, 1000);
        assertEquals(Set.of(), index.candidatesInRange(Level.NETHER, anywhere));
    }

    @Test
    void putAddsAndRemoveEvictsFromEveryDimensionCell() {
        BoxIndex index = new BoxIndex();
        UUID id = UUID.randomUUID();
        GlobalPos pos = GlobalPos.of(Level.OVERWORLD, new BlockPos(5, 64, 5));

        index.put(id, pos);
        Geometry.Bounds area = new Geometry.Bounds(0, 0, 15, 15);
        assertEquals(Set.of(id), index.candidatesInRange(Level.OVERWORLD, area));

        index.remove(id);
        assertEquals(Set.of(), index.candidatesInRange(Level.OVERWORLD, area));
    }
}
