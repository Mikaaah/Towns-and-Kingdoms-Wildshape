package nl.townsandkingdoms.wildshape.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.Climate.Sampler;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.spawn.SpawnFinderFix;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.WildshapeClimateSampler;

/** Custom spawn search is scoped to active Wildshape terrain. */
@Mixin(Climate.class)
class MixinSpawnFinder {
    @Inject(at = @At("HEAD"), method = "findSpawnPosition", cancellable = true)
    private static void findSpawnPosition(List<ParameterPoint> points, Sampler sampler, CallbackInfoReturnable<BlockPos> callback) {
        if ((Object) sampler instanceof WildshapeClimateSampler wildshape && wildshape.getGeneratorContext() != null) {
            SpawnFinderFix.Result result = nl.townsandkingdoms.wildshape.world.worldgen.WorldGenFlags.approximately(
                    () -> new SpawnFinderFix(points, sampler).result);
            if (result != null) callback.setReturnValue(result.location());
        }
    }
}
