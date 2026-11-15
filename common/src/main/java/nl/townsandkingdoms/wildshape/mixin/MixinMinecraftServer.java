package nl.townsandkingdoms.wildshape.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.ServerLevelData;
import nl.townsandkingdoms.wildshape.data.preset.PresetData;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.WildshapeClimateSampler;

@Mixin(MinecraftServer.class)
class MixinMinecraftServer {

	@Inject(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/Climate$Sampler;findSpawnPosition()Lnet/minecraft/core/BlockPos;"
		),
		method = "setInitialSpawn"
	)
    private static void findSpawnPosition(ServerLevel serverLevel, ServerLevelData serverLevelData, boolean bl, boolean bl2, CallbackInfo callback) {
		RandomState randomState = serverLevel.getChunkSource().randomState();
		Climate.Sampler sampler = randomState.sampler();
		serverLevel.registryAccess().lookup(WildshapeRegistries.PRESET).flatMap((registry) -> {
			return registry.get(PresetData.PRESET);
		}).ifPresent((preset) -> {
			if((Object) randomState instanceof WildshapeRandomState wildshapeRandomState && (Object) sampler instanceof WildshapeClimateSampler wildshapeClimateSampler) {
				// an overworld that doesn't read the cells (a datapack's own noise settings) has no terrain of ours to
				// search: vanilla's search, from 0,0
				if(wildshapeRandomState.generatorContext() != null) {
					BlockPos searchCenter = preset.value().world().properties.spawnType.getSearchCenter(wildshapeRandomState.generatorContext());
					wildshapeClimateSampler.setSpawnSearchCenter(searchCenter);
				}
			} else {
				throw new IllegalStateException();
			}
		});
    }
}
