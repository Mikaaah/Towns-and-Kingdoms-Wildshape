package nl.townsandkingdoms.wildshape.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.WorldGenFlags;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import nl.townsandkingdoms.wildshape.world.worldgen.densityfunction.CellSampler;

class WorldGenLookupTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test void searchScopesRestoreAfterNestedCallsAndFailures() throws Exception {
        assertFalse(WorldGenFlags.fastLookups());
        assertThrows(IllegalStateException.class, () -> WorldGenFlags.approximately(() -> {
            assertTrue(WorldGenFlags.fastLookups());
            assertTrue(WorldGenFlags.approximately(WorldGenFlags::fastLookups));
            throw new IllegalStateException("search failed");
        }));
        assertFalse(WorldGenFlags.fastLookups());
        try (var executor = Executors.newSingleThreadExecutor()) {
            WorldGenFlags.approximately(() -> {
                try { assertFalse(executor.submit(WorldGenFlags::fastLookups).get()); }
                catch (Exception failure) { throw new AssertionError(failure); }
                return null;
            });
        }
    }

    @Test void broadSurveyScopesDoNotAllocateFilteredTiles() {
        var context = GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(), 1111, 3, 6, false);
        Cell cell = new Cell();
        assertEquals(0, context.cache.retainedTileCount());
        WorldGenFlags.approximately(() -> context.lookup.applyAsGenerated(cell, 42000, -36000));
        assertTrue(Float.isFinite(cell.height));
        assertEquals(0, context.cache.retainedTileCount());
        assertFalse(WorldGenFlags.fastLookups());
    }

    @Test void generatedColumnsUseFilteredTilesOnEveryThread() throws Exception {
        var preset = BuiltinPresetRenderTest.presets().get("default").get();
        var context = GeneratorContext.makeCached(preset, 1111, 3, 6, false);
        int[][] positions = {{12, 0}, {128, 124}, {-4, -16}};
        try (var executor = Executors.newFixedThreadPool(2)) {
            for (int[] position : positions) {
                int x = position[0], z = position[1];
                Cell expected = new Cell();
                context.lookup.apply(expected, x, z, true);
                float height = expected.height;
                var cache = new CellSampler.Cache2d();
                // Warm the same position with a cheap search, then read real terrain.
                WorldGenFlags.approximately(() -> cache.getAndUpdate(context.lookup, x, z, true));
                assertEquals(height, cache.getAndUpdate(context.lookup, x, z, true).height);
                assertEquals(height, executor.submit(() -> new CellSampler.Cache2d()
                        .getAndUpdate(context.lookup, x, z, true).height).get());
                Cell approximate = new Cell();
                context.lookup.compute(approximate, x, z);
                assertEquals(approximate.height, WorldGenFlags.approximately(() ->
                        cache.getAndUpdate(context.lookup, x, z, true).height));
            }
        }
    }
}
