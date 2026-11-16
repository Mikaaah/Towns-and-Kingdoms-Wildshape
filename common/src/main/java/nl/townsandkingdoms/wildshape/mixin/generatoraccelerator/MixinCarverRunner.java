package nl.townsandkingdoms.wildshape.mixin.generatoraccelerator;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.RandomState;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;
import nl.townsandkingdoms.wildshape.world.worldgen.rivermap.RaisedWaterGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** GA2 bypasses vanilla applyCarvers, so protect raised water in its own aquifer paths. */
@Pseudo
@Mixin(targets = "dev.denismasterherobrine.generatoraccelerator2.carver.CarverRunner", remap = false)
abstract class MixinCarverRunner {
    @ModifyExpressionValue(method = "apply", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/NoiseChunk;aquifer()Lnet/minecraft/world/level/levelgen/Aquifer;",
            remap = true), require = 1)
    private static Aquifer wildshape$guardCpuAquifer(Aquifer aquifer,
            @Local(argsOnly = true) RandomState randomState,
            @Local(argsOnly = true) ChunkAccess chunk) {
        return guard(aquifer, randomState, chunk);
    }

    @ModifyExpressionValue(method = "apply", at = @At(value = "INVOKE", target =
            "Ldev/denismasterherobrine/generatoraccelerator2/gpu/GpuTerrain;aquifer()Lnet/minecraft/world/level/levelgen/Aquifer;"),
            require = 1)
    private static Aquifer wildshape$guardGpuAquifer(Aquifer aquifer,
            @Local(argsOnly = true) RandomState randomState,
            @Local(argsOnly = true) ChunkAccess chunk) {
        return guard(aquifer, randomState, chunk);
    }

    @Unique
    private static Aquifer guard(Aquifer aquifer, RandomState randomState, ChunkAccess chunk) {
        if ((Object) randomState instanceof WildshapeRandomState wildshape
                && wildshape.generatorContext() != null)
            return new RaisedWaterGuard(chunk, wildshape.generatorContext()).wrap(aquifer);
        return aquifer;
    }
}
