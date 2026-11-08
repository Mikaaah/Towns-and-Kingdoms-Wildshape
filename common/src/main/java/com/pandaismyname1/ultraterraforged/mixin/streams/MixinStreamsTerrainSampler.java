package com.pandaismyname1.ultraterraforged.mixin.streams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.pandaismyname1.ultraterraforged.world.worldgen.WorldGenFlags;

/** Streams surveys large ungenerated areas; those probes must not construct full filtered tiles. */
@Pseudo
@Mixin(targets = "dev.streamsreflowing.worldgen.StreamsWorldgen$1", remap = false)
abstract class MixinStreamsTerrainSampler {
    @WrapMethod(method = "height", remap = false)
    private double wildshape$surveyHeight(int x, int z, Operation<Double> original) {
        return WorldGenFlags.approximately(() -> original.call(x, z));
    }

    @WrapMethod(method = {"heightsBatch", "groundHeightsBatch"}, remap = false)
    private void wildshape$surveyBatch(int[] x, int[] z, double[] heights, int size, Operation<Void> original) {
        WorldGenFlags.approximately(() -> {
            original.call(x, z, heights, size);
            return null;
        });
    }
}
