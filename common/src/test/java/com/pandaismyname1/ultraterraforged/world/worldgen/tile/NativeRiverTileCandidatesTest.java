package com.pandaismyname1.ultraterraforged.world.worldgen.tile;

import static org.junit.jupiter.api.Assertions.*;

import com.pandaismyname1.ultraterraforged.concurrent.SimpleResource;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** A tile's conservative fast river hints must not outlive its pooled backing. */
public class NativeRiverTileCandidatesTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test void identifiesRiverValleysBanksAndRaisedWater() {
        Size blocks = Size.blocks(1, 0), chunks = Size.chunks(1, 0);
        Cell[] backing = new Cell[blocks.arraySize()];
        Arrays.setAll(backing, ignored -> {
            Cell c = new Cell();
            c.terrain = TerrainType.PLAINS;
            return c;
        });
        Tile.Chunk[] chunkBacking = new Tile.Chunk[chunks.arraySize()];
        var tile = new Tile(0, 0, 1, 0, blocks, chunks,
                new SimpleResource<>(backing, ignored -> {}),
                new SimpleResource<>(chunkBacking, ignored -> {}));
        int river = 2 + (3 << 4), valley = 5 + (8 << 4), bank = 9 + (9 << 4),
                raised = 10 + (11 << 4), dry = 4 + (4 << 4);
        Tile.Chunk chunk = tile.getChunkReader(0, 0);
        chunk.getCell(2, 3).terrain = TerrainType.RIVER;
        chunk.getCell(5, 8).riverDistance = 0.4F;
        chunk.getCell(9, 9).riverBank = 0.8F;
        chunk.getCell(10, 11).waterLevel = 0.4F;
        long[] candidates = tile.nativeRiverCandidates(0, 0);
        assertNotNull(candidates);
        for (int i : new int[]{river, valley, bank, raised}) {
            assertTrue((candidates[i >>> 6] & (1L << (i & 63))) != 0L,
                    "Missed river candidate at " + i);
        }
        assertEquals(0L, candidates[dry >>> 6] & (1L << (dry & 63)),
                "Purely dry terrain must avoid native river resampling");
        tile.close();
        assertNull(tile.nativeRiverCandidates(0, 0),
                "Closed pooled tiles must not provide stale hints");
    }
}
