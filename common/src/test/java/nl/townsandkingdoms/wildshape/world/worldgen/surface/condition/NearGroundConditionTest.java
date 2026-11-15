package nl.townsandkingdoms.wildshape.world.worldgen.surface.condition;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import nl.townsandkingdoms.wildshape.test.TestBootstrap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class NearGroundConditionTest {
    @BeforeAll static void boot() { TestBootstrap.init(); }
    @Test void loweredActualSurfaceReceivesSoilWithoutPaintingCaveFloors() throws Exception {
        var chunk = mock(ChunkAccess.class);
        when(chunk.getPos()).thenReturn(new ChunkPos(-7, -24));
        when(chunk.getHeight(eq(Heightmap.Types.OCEAN_FLOOR_WG), eq(13), eq(15))).thenReturn(91);
        var context = mock(SurfaceRules.Context.class);
        set(context, "chunk", chunk);
        context.blockX = -99;
        context.blockZ = -369;
        var condition = new NearGroundCondition(context, 5);
        for (int y = 86; y <= 92; y++) {
            context.blockY = y;
            assertTrue(condition.compute(), "actual surface depth " + y);
        }
        for (int y : new int[]{40, 85, 93, 130}) {
            context.blockY = y;
            assertFalse(condition.compute(), "underground floor or predicted height " + y);
        }
    }

    private static void set(Object target, String name, Object value) throws Exception {
        var field = SurfaceRules.Context.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
