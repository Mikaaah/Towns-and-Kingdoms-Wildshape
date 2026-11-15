package nl.townsandkingdoms.wildshape.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.RandomState;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;
import nl.townsandkingdoms.wildshape.world.worldgen.rivermap.FrozenFalls;

// Features finish synchronously before the completed future is returned.
@Mixin(ChunkStatusTasks.class)
public class MixinChunkStatusTasks {

	@Nullable
	private static GeneratorContext wildshape$context(WorldGenContext worldGenContext) {
		RandomState randomState = worldGenContext.level().getChunkSource().randomState();
		if((Object) randomState instanceof WildshapeRandomState wildshapeRandomState) {
			return wildshapeRandomState.generatorContext();
		}
		return null;
	}

	//structure starts
	@Inject(
		at = @At("HEAD"),
		method = "generateStructureStarts"
	)
	private static void wildshape$structureStartsHead(WorldGenContext worldGenContext, ChunkStep step, StaticCache2D<GenerationChunkHolder> cache, ChunkAccess centerChunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> callback) {
		@Nullable
		GeneratorContext context = wildshape$context(worldGenContext);
		if(context != null) {
			ChunkPos chunkPos = centerChunk.getPos();
			context.cache.queueAtChunk(chunkPos.x, chunkPos.z);
		}
	}

	//features
	@Inject(
		at = @At("TAIL"),
		method = "generateFeatures"
	)
	private static void wildshape$featuresTail(WorldGenContext worldGenContext, ChunkStep step, StaticCache2D<GenerationChunkHolder> cache, ChunkAccess centerChunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> callback) {
		@Nullable
		GeneratorContext context = wildshape$context(worldGenContext);
		if(context != null) {
			// after freezing, so falls on frozen rivers freeze too
			if(context.preset.rivers().raisedWater) {
				List<ChunkAccess> region = new ArrayList<>();
				cache.forEach((holder) -> {
					ChunkAccess chunk = holder.getLatestChunk();
					if(chunk != null) {
						region.add(chunk);
					}
				});
				FrozenFalls.apply(centerChunk, region, context);
			}
			ChunkPos chunkPos = centerChunk.getPos();
			context.cache.dropAtChunk(chunkPos.x, chunkPos.z);
		}
	}
}
