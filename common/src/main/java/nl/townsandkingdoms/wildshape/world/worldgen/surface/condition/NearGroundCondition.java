package nl.townsandkingdoms.wildshape.world.worldgen.surface.condition;

import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Within the given depth of the chunk's actual solid surface, including the sea floor.
 * A predicted terrain cell can differ after density shaping or structure adjustment;
 * using it here leaves exposed strata where the real ground needs soil.
 * The top-of-column height also excludes cave floors beneath an intact roof.
 */
class NearGroundCondition extends SurfaceRules.LazyYCondition {
	private final int depth;
	private final ChunkAccess chunk;

	NearGroundCondition(SurfaceRules.Context context, int depth) {
		super(context);
		this.depth = depth;
		this.chunk = context.chunk;
	}

	@Override
	protected boolean compute() {
		int ground = this.chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,
                this.context.blockX & 15, this.context.blockZ & 15);
		int y = this.context.blockY;
		return y <= ground + 1 && y >= ground - this.depth;
	}

	public record Source(int depth) implements SurfaceRules.ConditionSource {
		public static final Codec<Source> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("depth").forGetter(Source::depth)
		).apply(instance, Source::new));

		@Override
		public NearGroundCondition apply(SurfaceRules.Context ctx) {
			return new NearGroundCondition(ctx, this.depth);
		}

		@Override
		public KeyDispatchDataCodec<Source> codec() {
			return WildshapeCodecs.keyDispatch(CODEC);
		}
	}
}
