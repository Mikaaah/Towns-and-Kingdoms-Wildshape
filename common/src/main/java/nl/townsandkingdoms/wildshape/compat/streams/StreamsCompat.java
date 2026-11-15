package nl.townsandkingdoms.wildshape.compat.streams;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.platform.ModLoaderUtil;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;

/**
 * Streams Reflowing moves river water along as currents. Its own idea of where rivers run and which way is downhill
 * comes from vanilla's river biomes and the sea level, so Wildshape's rivers, raised ones above all, got
 * currents running the wrong way or none. Here Wildshape tells it, through Streams' own API, which way the water
 * of its rivers runs. After Mikaaah's Wildshape-Streams-Currents add-on, whose idea this is (discussion #3).
 *
 * This class mentions none of Streams' classes, so it can be loaded without it; {@link RiverCurrents} does.
 */
public class StreamsCompat {
	public static final String MOD_ID = "streamsreflowing";

	public static boolean isEnabled() {
		return ModLoaderUtil.isLoaded(MOD_ID);
	}

	public static void bootstrap() {
		GeneratorContext.retainRiverData = true;
		try {
			RiverCurrents.register();
		} catch (LinkageError e) {
			// a Streams Reflowing whose API changed: no currents rather than a crash
			WildshapeCommon.LOGGER.warn("This version of Streams Reflowing isn't supported, Wildshape's rivers won't have its currents", e);
		}
	}

	// set on a worldgen thread while Streams carves one of its own rivers (MixinRiverCarverFeature)
	private static final ThreadLocal<CarvingState> CARVING = new ThreadLocal<>();

    private static final class CarvingState {
        final java.util.Map<Long, WaterColumn> columns = new java.util.HashMap<>();
    }

    private record WaterColumn(int bed, int surface) {
        static final WaterColumn NONE = new WaterColumn(0, 0);
        boolean contains(int y) { return y > bed && y <= surface; }
    }

	public static boolean carving() {
		return CARVING.get() != null;
	}

	public static void setCarving(boolean carving) {
		if (carving) CARVING.set(new CarvingState());
        else CARVING.remove();
	}

    public static <T> T whileCarving(java.util.function.Supplier<T> action) {
        CarvingState previous = CARVING.get();
        CARVING.set(new CarvingState());
        try {
            return action.get();
        } finally {
            if (previous == null) CARVING.remove();
            else CARVING.set(previous);
        }
    }

	/**
	 * Streams also carves rivers of its own, which in an Wildshape world cut into its rivers and lakes above the
	 * sea. Protect only those native raised-water volumes and their existing beds.
     * Streams must still be able to lower its own water and clear obsolete sources
     * above a native waterline; preserving every fluid block leaves water columns.
	 */
	public static boolean keeps(net.minecraft.server.level.WorldGenRegion region, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		if (!state.getFluidState().isEmpty()) return false;
        GeneratorContext context = context(region.getLevel());
        if (context == null) return false;
        CarvingState carving = CARVING.get();
        long key = net.minecraft.core.BlockPos.asLong(pos.getX(), 0, pos.getZ());
        WaterColumn column = carving == null ? waterColumn(region, context, pos)
                : carving.columns.computeIfAbsent(key, ignored -> waterColumn(region, context, pos));
        if (column == WaterColumn.NONE) return false;
        boolean keeps = (column.contains(pos.getY()) && water(region.getBlockState(pos)))
                || (state.isAir() && pos.getY() == column.bed()
                    && !region.getBlockState(pos).isAir()
                    && water(region.getBlockState(pos.above())));
		if (keeps) {
			KEPT.incrementAndGet();
		}
		return keeps;
	}

    private static boolean water(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.WATER)
                || state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
    }

    private static WaterColumn waterColumn(net.minecraft.server.level.WorldGenRegion region,
            GeneratorContext context, net.minecraft.core.BlockPos pos) {
        var cell = new nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell();
        context.lookup.apply(cell, pos.getX(), pos.getZ(), true);
        if (cell.waterLevel <= 0.0F) return WaterColumn.NONE;
        int surface = context.levels.scale(cell.waterLevel);
        if (surface <= context.levels.waterY) return WaterColumn.NONE;
        int bed = region.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,
                pos.getX(), pos.getZ());
        return bed < surface ? new WaterColumn(bed, surface) : WaterColumn.NONE;
    }

	// how many blocks keeps has kept this session, for tests
	private static final java.util.concurrent.atomic.AtomicLong KEPT = new java.util.concurrent.atomic.AtomicLong();

	public static long kept() {
		return KEPT.get();
	}

	/**
	 * The generator of a world Wildshape makes, or null. Streams only gives the overworld currents.
	 */
	@Nullable
	public static GeneratorContext context(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return null;
		}
		return (Object) level.getChunkSource().randomState() instanceof WildshapeRandomState randomState ? randomState.generatorContext() : null;
	}
}
