package nl.townsandkingdoms.wildshape.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.WorldGenFlags;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import nl.townsandkingdoms.wildshape.world.worldgen.densityfunction.CellSampler;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.Heightmap;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

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

    @Test void structureSearchRestoresBothFlagsAfterNestedCallsAndFailures() throws Exception {
        assertFalse(WorldGenFlags.structureSearch());
        assertThrows(IllegalStateException.class, () -> WorldGenFlags.locatingStructure(() -> {
            assertTrue(WorldGenFlags.fastLookups());
            assertTrue(WorldGenFlags.structureSearch());
            assertTrue(WorldGenFlags.locatingStructure(WorldGenFlags::structureSearch));
            throw new IllegalStateException("search failed");
        }));
        assertFalse(WorldGenFlags.fastLookups());
        assertFalse(WorldGenFlags.structureSearch());
        try (var executor = Executors.newSingleThreadExecutor()) {
            WorldGenFlags.locatingStructure(() -> {
                try { assertFalse(executor.submit(WorldGenFlags::structureSearch).get()); }
                catch (Exception failure) { throw new AssertionError(failure); }
                return null;
            });
        }
        WorldGenFlags.approximately(() -> {
            WorldGenFlags.locatingStructure(() -> null);
            assertTrue(WorldGenFlags.fastLookups());
            assertFalse(WorldGenFlags.structureSearch());
            return null;
        });
    }

    @Test void structureSearchNeverInitializesRiversOrFilteredTiles() {
        var context = GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(), 1111, 3, 6, false);
        Heightmap original = context.localHeightmap.get();
        var continent = spy(original.continent());
        doThrow(new AssertionError("A structure search initialized a river network"))
                .when(continent).getRivermap(anyInt(), anyInt());
        Heightmap guarded = new Heightmap(original.cellProvider(), original.terrain(), original.region(),
                continent, original.climate(), original.levels(), original.controlPoints(), original.terrainFrequency(),
                original.mountainChainAlpha(), original.beachAlpha(), original.landforms());
        context.localHeightmap = ThreadLocal.withInitial(() -> guarded);
        Cell previous = new Cell();
        guarded.cellProvider().setCacheCell(previous);
        WorldGenFlags.locatingStructure(() -> {
            for (int[] position : new int[][] {{42000, -36000}, {-1984, -16}, {7140, 5708}}) {
                Cell cell = new Cell();
                context.lookup.applyAsGenerated(cell, position[0], position[1]);
                assertTrue(Float.isFinite(cell.height));
                assertTrue(Float.isFinite(cell.continentalness));
                context.lookup.apply(cell.reset(), position[0], position[1], true);
                context.lookup.compute(cell.reset(), position[0], position[1]);
                assertSame(previous, guarded.cellProvider().getCacheCell());
            }
            return null;
        });
        verify(continent, never()).getRivermap(anyInt(), anyInt());
        assertEquals(0, context.cache.retainedTileCount());
    }

    @Test void structureEstimateCannotRemainInAnOrdinaryApproximateLookupCache() {
        var context = GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(), 1111, 3, 6, false);
        var lookup = spy(context.lookup);
        doAnswer(call -> {
            Cell cell = call.getArgument(0);
            cell.height = WorldGenFlags.structureSearch() ? 0.25F : 0.75F;
            return false;
        }).when(lookup).applyAsGenerated(any(Cell.class), anyInt(), anyInt());
        var cache = new CellSampler.Cache2d();
        assertEquals(0.25F, WorldGenFlags.locatingStructure(() -> cache.getAndUpdate(lookup, 100, -100, true).height));
        assertEquals(0.75F, WorldGenFlags.approximately(() -> cache.getAndUpdate(lookup, 100, -100, true).height));
        verify(lookup, times(2)).applyAsGenerated(any(Cell.class), anyInt(), anyInt());
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
                WorldGenFlags.locatingStructure(() -> cache.getAndUpdate(context.lookup, x, z, true));
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
