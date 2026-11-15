package nl.townsandkingdoms.wildshape.world.worldgen.feature.placement.poisson;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.RandomState;
import nl.townsandkingdoms.wildshape.test.TestBootstrap;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;

class FastPoissonContextTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test void ordinaryDimensionsUseFallbackDensityWithoutTerrainContext() {
        var random = mock(RandomState.class);
        var injected = mock(WildshapeRandomState.class);
        assertNull(injected.generatorContext());
        var modifier = new FastPoissonModifier(4, 1, 0.5F, 0.5F, 100, 0);
        assertSame(BiomeVariance.NONE, modifier.biomeVariance(new ChunkPos(-1, -1), injected));
        var noise = assertDoesNotThrow(() -> modifier.getDensityNoise(1234, new ChunkPos(-1, -1), random));
        assertEquals(1.0F, noise.compute(-16, -16, 0));
    }
}
