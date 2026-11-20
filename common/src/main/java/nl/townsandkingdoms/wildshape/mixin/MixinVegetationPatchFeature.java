package nl.townsandkingdoms.wildshape.mixin;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.VegetationPatchFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;
import nl.townsandkingdoms.wildshape.world.worldgen.cave.LushPoolSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Guard only native lush floor pools; moss patches and foreign cave features remain native. */
@Mixin(VegetationPatchFeature.class)
abstract class MixinVegetationPatchFeature {
    @Inject(method = "placeGround", at = @At("HEAD"), cancellable = true, require = 1)
    private void wildshape$preflightLushPool(WorldGenLevel level, VegetationPatchConfiguration config,
            Predicate<BlockState> replaceable, RandomSource random, BlockPos.MutableBlockPos ground,
            int depth, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this != Feature.WATERLOGGED_VEGETATION_PATCH || config.surface != CaveSurface.FLOOR) return;
        if (!((Object) level.getLevel().getChunkSource().randomState() instanceof WildshapeRandomState wildshape)
                || wildshape.generatorContext() == null) return;
        if (!LushPoolSupport.nativeLush(level.getBiome(ground))) return;
        if (!LushPoolSupport.supported(level, ground, depth)) callback.setReturnValue(false);
    }
}
