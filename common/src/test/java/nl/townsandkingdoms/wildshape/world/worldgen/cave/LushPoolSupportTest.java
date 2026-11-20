package nl.townsandkingdoms.wildshape.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import nl.townsandkingdoms.wildshape.test.TestBootstrap;

class LushPoolSupportTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    private static WorldGenLevel floor(int airAtY) {
        WorldGenLevel level = mock(WorldGenLevel.class);
        when(level.getMinBuildHeight()).thenReturn(-64);
        when(level.getBlockState(any())).thenAnswer(call -> call.<BlockPos>getArgument(0).getY() == airAtY
                ? Blocks.CAVE_AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
        return level;
    }

    @Test void thinLedgeFailsBeforeAnyClayOrPoolWaterCanBeWritten() {
        var level = floor(-22);
        assertFalse(LushPoolSupport.supported(level, new BlockPos(8,-20,8), 3));
        verify(level, never()).setBlock(any(), any(), anyInt());
    }

    @Test void entireRequestedClayDepthAndTheNextSupportingBlockMustBeSolid() {
        assertFalse(LushPoolSupport.supported(floor(-23), new BlockPos(8,-20,8), 3),
                "all three replacement blocks existed but their support was air");
        assertTrue(LushPoolSupport.supported(floor(-24), new BlockPos(8,-20,8), 3),
                "a four-block supported bed was rejected");
        assertTrue(LushPoolSupport.supported(floor(1000), new BlockPos(8,-20,8), 3));
    }

    @Test void fluidAndWaterloggedSupportsDoNotBecomeNewClayTrays() {
        var level = floor(1000);
        when(level.getBlockState(any())).thenReturn(Blocks.WATER.defaultBlockState());
        assertFalse(LushPoolSupport.supported(level,new BlockPos(8,-20,8),3));
        when(level.getBlockState(any())).thenReturn(Blocks.OAK_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED,true));
        assertFalse(LushPoolSupport.supported(level,new BlockPos(8,-20,8),3));
    }

    @Test void preflightReadsOnlyTheCandidateColumnAndHonorsWorldBottom() {
        var level = floor(1000);
        when(level.getBlockState(any())).thenAnswer(call -> {
            BlockPos pos=call.getArgument(0);
            assertEquals(-16,pos.getX()); assertEquals(31,pos.getZ());
            assertTrue(pos.getY()>=-64);
            return Blocks.STONE.defaultBlockState();
        });
        assertFalse(LushPoolSupport.supported(level,new BlockPos(-16,-62,31),3));
        assertTrue(LushPoolSupport.supported(level,new BlockPos(-16,-60,31),3));
    }

    @Test void onlyNativeUnprotectedLushBiomeIsEligible() {
        assertTrue(LushPoolSupport.nativeLush(biome("minecraft:lush_caves")));
        assertFalse(LushPoolSupport.nativeLush(biome("minecraft:dripstone_caves")));
        assertFalse(LushPoolSupport.nativeLush(biome("alexscaves:primordial_caves")));
        assertFalse(LushPoolSupport.nativeLush(biome("regions_unexplored:prismachasm")));
        var protectedLush=biome("minecraft:lush_caves");
        when(protectedLush.is(CaveBiomeProtection.PROTECTED)).thenReturn(true);
        assertFalse(LushPoolSupport.nativeLush(protectedLush));
    }

    @SuppressWarnings("unchecked")
    private static Holder<Biome> biome(String id) {
        Holder<Biome> biome = mock(Holder.class);
        var key=ResourceKey.create(Registries.BIOME,ResourceLocation.parse(id));
        when(biome.unwrapKey()).thenReturn(Optional.of(key));
        when(biome.is(key)).thenReturn(true);
        return biome;
    }
}
