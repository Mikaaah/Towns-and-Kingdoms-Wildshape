package nl.townsandkingdoms.wildshape.test;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import nl.townsandkingdoms.wildshape.world.worldgen.climate.BiomeNoise;
import nl.townsandkingdoms.wildshape.world.worldgen.continent.Continent;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.Levels;
import nl.townsandkingdoms.wildshape.world.worldgen.util.Seed;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class BiomeNoiseCacheTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    private BiomeNoise noise(int seed, float bias, AtomicInteger calls) {
        var preset = BuiltinPresetRenderTest.presets().get("default").get();
        Continent continent = mock(Continent.class);
        when(continent.getLandValue(anyFloat(), anyFloat())).thenAnswer(call -> {
            calls.incrementAndGet();
            float x = call.getArgument(0), z = call.getArgument(1);
            return 0.5F + bias + (float) Math.sin(x * 0.003 + z * 0.007) * 0.3F;
        });
        return new BiomeNoise(new Seed(seed), continent, preset.world().controlPoints,
                preset.climate(), new Levels(576, 63));
    }

    private static void compare(Cell expected, Cell actual) {
        assertEquals(expected.continentNoise, actual.continentNoise);
        assertEquals(expected.terrainMask, actual.terrainMask);
        assertEquals(expected.biomeVariant, actual.biomeVariant);
        assertEquals(expected.biomeRegionId, actual.biomeRegionId);
        assertEquals(expected.biomeRegionEdge, actual.biomeRegionEdge);
        assertEquals(expected.regionMoisture, actual.regionMoisture);
        assertEquals(expected.regionTemperature, actual.regionTemperature);
        assertEquals(expected.temperature, actual.temperature);
        assertEquals(expected.moisture, actual.moisture);
        assertEquals(expected.biomeType, actual.biomeType);
    }

    @Test void repeatedRegionCalculationsAreAvoidedWithoutChangingClimate() throws Exception {
        var cachedCalls = new AtomicInteger();
        var rawCalls = new AtomicInteger();
        BiomeNoise cached = noise(1111, 0, cachedCalls), reference = noise(1111, 0, rawCalls);
        Field field = BiomeNoise.class.getDeclaredField("lastLandValue");
        field.setAccessible(true);
        ThreadLocal<?> referenceCache = (ThreadLocal<?>) field.get(reference);
        for (int z = -32; z < 32; z++) for (int x = -32; x < 32; x++) {
            Cell expected = new Cell(), actual = new Cell();
            expected.height = actual.height = 0.6F;
            // Force the reference to evaluate the original continent expression
            // on every sample; compare all climate fields with the cached path.
            referenceCache.remove();
            reference.apply(expected, x, z, x, z);
            cached.apply(actual, x, z, x, z);
            compare(expected, actual);
        }
        assertEquals(4096, rawCalls.get());
        assertTrue(cachedCalls.get() < rawCalls.get() / 4,
                "Repeated coastline evaluations were not eliminated: " + cachedCalls);
    }

    @Test void cachesStaySeparateBetweenWorkersAndWorlds() throws Exception {
        BiomeNoise first = noise(1111, 0, new AtomicInteger());
        BiomeNoise other = noise(1111, 0.1F, new AtomicInteger());
        try (var pool = Executors.newFixedThreadPool(3)) {
            for (int[] p : new int[][] {{0,0}, {-2000,32}, {42000,-36000}, {0,0}}) {
                for (BiomeNoise world : new BiomeNoise[] {first, other}) {
                    Cell expected = new Cell();
                    world.apply(expected, p[0], p[1], p[0], p[1]);
                    Cell actual = pool.submit(() -> {
                        Cell cell = new Cell();
                        world.apply(cell, p[0], p[1], p[0], p[1]);
                        return cell;
                    }).get();
                    compare(expected, actual);
                }
            }
        }
    }
}
