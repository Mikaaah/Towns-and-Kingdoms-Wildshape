package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileCache;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileGenerator;

/** Regression: a transient async terrain failure must not poison its tile coordinates. */
public class FailedTileRetryTest {
    @Test void failedFutureEvictsForTheNextRequest() {
        AtomicInteger submissions = new AtomicInteger();
        TileGenerator failing = new TileGenerator(null, null, 3, 1, 6) {
            @Override public CompletableFuture<Tile> generate(int x, int z) {
                submissions.incrementAndGet();
                return CompletableFuture.failedFuture(new IllegalStateException("simulated worldgen failure"));
            }
        };
        TileCache cache = new TileCache(3, true, failing);
        assertThrows(RuntimeException.class, () -> cache.provide(0, 0));
        assertThrows(RuntimeException.class, () -> cache.provide(0, 0));
        assertEquals(2, submissions.get(), "failed tile was kept in the cache");
    }
}
