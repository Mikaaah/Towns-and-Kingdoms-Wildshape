package nl.townsandkingdoms.wildshape.world.worldgen.cave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;

/** Local preflight for native waterlogged lush patches before they replace any ground. */
public final class LushPoolSupport {
    private LushPoolSupport() {}

    public static boolean nativeLush(Holder<Biome> biome) {
        return biome != null && biome.is(Biomes.LUSH_CAVES) && !CaveBiomeProtection.protects(biome);
    }

    /**
     * Vanilla accepts a partial replacement column and later tests just one
     * block below its water. On a thin ledge that leaves a clay tray suspended
     * above a larger cavern. Validate the complete replacement depth and an
     * additional supporting block before the first write. Existing supported
     * pools keep vanilla's random draws and placement unchanged.
     */
    public static boolean supported(WorldGenLevel level, BlockPos ground, int depth) {
        long required = Math.max(1L, depth) + 1L;
        if ((long) ground.getY() - required + 1L < level.getMinBuildHeight()) return false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int offset = 0; offset < required; offset++) {
            cursor.set(ground.getX(), ground.getY() - offset, ground.getZ());
            BlockState state = level.getBlockState(cursor);
            if (!state.getFluidState().isEmpty() || !state.isFaceSturdy(level, cursor, Direction.UP)) return false;
        }
        return true;
    }
}
