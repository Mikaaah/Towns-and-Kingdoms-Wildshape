package nl.townsandkingdoms.wildshape.mixin.streams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import nl.townsandkingdoms.wildshape.compat.streams.StreamsCompat;

/**
 * Scopes the native river feature for MixinWorldGenRegion, restoring the previous
 * state even when a nested feature or failed placement exits exceptionally.
 */
@Pseudo
@Mixin(targets = "dev.streamsreflowing.worldgen.RiverCarverFeature", remap = false)
abstract class MixinRiverCarverFeature {

    @WrapMethod(method = "place", remap = false)
    private boolean wildshape$scopedCarving(FeaturePlaceContext<NoneFeatureConfiguration> context,
            Operation<Boolean> original) {
        return StreamsCompat.whileCarving(() -> original.call(context));
    }
}
