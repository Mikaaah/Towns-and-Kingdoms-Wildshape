package nl.townsandkingdoms.wildshape.world.worldgen.cave;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import nl.townsandkingdoms.wildshape.test.TestBootstrap;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.Levels;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CaveFluidIntersectionTest {
    @BeforeAll static void boot() { TestBootstrap.init(); }

    private static final class World {
        final ChunkAccess chunk = mock(ChunkAccess.class);
        final Map<BlockPos, BlockState> blocks = new HashMap<>();
        final Set<BlockPos> updates = new HashSet<>();
        final CaveCarving carving;
        World() {
            when(chunk.getPos()).thenReturn(new ChunkPos(0, 0));
            when(chunk.getMinBuildHeight()).thenReturn(-64);
            when(chunk.getMaxBuildHeight()).thenReturn(320);
            when(chunk.getAllStarts()).thenReturn(Map.of());
            when(chunk.getHeight(any(), anyInt(), anyInt())).thenReturn(100);
            when(chunk.getBlockState(any())).thenAnswer(call -> {
                BlockPos pos = call.getArgument(0);
                assertTrue(pos.getX() >= 0 && pos.getX() < 16 && pos.getZ() >= 0 && pos.getZ() < 16);
                return get(pos);
            });
            when(chunk.setBlockState(any(), any(), anyBoolean())).thenAnswer(call -> {
                BlockPos pos = call.<BlockPos>getArgument(0).immutable();
                BlockState old = get(pos);
                blocks.put(pos, call.getArgument(1));
                return old;
            });
            doAnswer(call -> { updates.add(call.<BlockPos>getArgument(0).immutable()); return null; })
                    .when(chunk).markPosForPostprocessing(any());
            var context = mock(GeneratorContext.class);
            context.levels = new Levels(384, 63);
            carving = new CaveCarving(chunk, context);
        }
        BlockState get(BlockPos pos) {
            return blocks.getOrDefault(pos, pos.getY() > 100 ? Blocks.AIR.defaultBlockState()
                    : pos.getY() <= -64 ? Blocks.BEDROCK.defaultBlockState() : Blocks.STONE.defaultBlockState());
        }
        void put(int x, int y, int z, BlockState state) { blocks.put(new BlockPos(x, y, z), state); }
    }

    @Test void anIntersectedUndergroundAquiferCannotLeaveFloatingWaterAndClay() {
        World world = new World();
        world.put(8, 9, 8, Blocks.CLAY.defaultBlockState());
        world.put(8, 10, 8, Blocks.WATER.defaultBlockState());
        world.put(8, 11, 8, Blocks.WATER.defaultBlockState());
        world.carving.hollow(8.5F, 10.5F, 8.5F, 3.5F, 7, 3.5F, CaveCarving.DRY, 30);
        for (int y = 9; y <= 11; y++)
            assertTrue(world.get(new BlockPos(8, y, 8)).isAir(), "suspended aquifer remnant at " + y);
        assertTrue(world.get(new BlockPos(8, 2, 8)).is(Blocks.STONE), "real chamber floor remains");
    }

    @Test void lavaInsideTheCutIsRemovedAndFluidOutsideItReceivesAnUpdate() {
        World world = new World();
        world.put(8, 10, 8, Blocks.LAVA.defaultBlockState());
        BlockPos outside = new BlockPos(12, 10, 8);
        world.blocks.put(outside, Blocks.LAVA.defaultBlockState());
        world.carving.hollow(8.5F, 10.5F, 8.5F, 3.5F, 7, 3.5F, CaveCarving.DRY, 30);
        world.carving.reconcileWater();
        assertTrue(world.get(new BlockPos(8, 10, 8)).isAir());
        assertTrue(world.get(outside).is(Blocks.LAVA));
        assertTrue(world.updates.contains(outside), "remaining exposed lava must flow normally");
    }

    @Test void surfaceWaterItsBedAndBedrockRemainProtected() {
        World world = new World();
        world.put(8, 101, 8, Blocks.WATER.defaultBlockState());
        world.carving.clear(8, 100, 8, CaveCarving.DRY);
        world.carving.clear(8, 101, 8, CaveCarving.DRY);
        world.carving.clear(8, -64, 8, CaveCarving.DRY);
        assertTrue(world.get(new BlockPos(8, 100, 8)).is(Blocks.STONE));
        assertTrue(world.get(new BlockPos(8, 101, 8)).is(Blocks.WATER));
        assertTrue(world.get(new BlockPos(8, -64, 8)).is(Blocks.BEDROCK));
    }

    @Test void waterloggedDecorationIsNotMistakenForAnUndergroundAquifer() {
        World world = new World();
        var state = Blocks.OAK_SLAB.defaultBlockState().setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true);
        world.put(8, 10, 8, state);
        world.carving.clear(8, 10, 8, CaveCarving.DRY);
        assertSame(state, world.get(new BlockPos(8, 10, 8)));
    }
}
