package nl.townsandkingdoms.wildshape.compat.streams;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.townsandkingdoms.wildshape.test.TestBootstrap;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.Levels;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.WorldLookup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StreamsCarvingGuardTest {
    @BeforeAll static void boot() { TestBootstrap.init(); }

    @Test void streamsCanLowerTheirOwnWaterOutsideTheNativeRaisedWaterVolume() {
        verifyColumn(false, 95, false);
        verifyColumn(true, 105, false);
    }

    @Test void nativeRaisedWaterAndItsBedStayIntact() {
        verifyColumn(true, 95, true);
        verifyColumn(true, 90, true);
    }

    @Test void exceptionsAndNestedFeaturesRestoreThePreviousCarvingScope() {
        assertFalse(StreamsCompat.carving());
        StreamsCompat.whileCarving(() -> {
            assertTrue(StreamsCompat.carving());
            assertThrows(IllegalStateException.class, () -> StreamsCompat.whileCarving(() -> {
                throw new IllegalStateException("failed feature");
            }));
            assertTrue(StreamsCompat.carving());
            return null;
        });
        assertFalse(StreamsCompat.carving());
        assertThrows(IllegalStateException.class, () -> StreamsCompat.whileCarving(() -> {
            throw new IllegalStateException("failed outer feature");
        }));
        assertFalse(StreamsCompat.carving());
    }

    private static void verifyColumn(boolean nativeWater, int y, boolean protectedWater) {
        var level = mock(ServerLevel.class);
        var region = mock(WorldGenRegion.class);
        var context = mock(GeneratorContext.class);
        context.levels = new Levels(384, 63);
        context.lookup = mock(WorldLookup.class);
        when(context.lookup.apply(any(), anyInt(), anyInt(), eq(true))).thenAnswer(call -> {
            Cell cell = call.getArgument(0);
            cell.waterLevel = nativeWater ? 100.01F / context.levels.worldHeight : 0;
            return true;
        });
        when(region.getLevel()).thenReturn(level);
        when(region.getHeight(eq(Heightmap.Types.OCEAN_FLOOR_WG), anyInt(), anyInt())).thenReturn(90);
        when(region.getBlockState(any())).thenAnswer(call ->
                call.<BlockPos>getArgument(0).getY() <= 90 ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
        when(region.getFluidState(any())).thenAnswer(call -> region.getBlockState(call.getArgument(0)).getFluidState());
        try (var adapter = mockStatic(StreamsCompat.class, CALLS_REAL_METHODS)) {
            adapter.when(() -> StreamsCompat.context(level)).thenReturn(context);
            assertEquals(protectedWater, StreamsCompat.keeps(region, new BlockPos(8, y, 8), Blocks.AIR.defaultBlockState()));
            assertFalse(StreamsCompat.keeps(region, new BlockPos(8, y, 8), Blocks.WATER.defaultBlockState()),
                    "a native stream joining a river can still place its own water");
        }
    }
}
