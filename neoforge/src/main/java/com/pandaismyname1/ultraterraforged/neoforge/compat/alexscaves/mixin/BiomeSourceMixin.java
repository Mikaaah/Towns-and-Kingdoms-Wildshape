package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.FixConfig;
import com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.PlacementEngine;
import com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.TerraforgedAlexCavesFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Applied after the port's -69420 mixin so this HEAD hook is placed before its callback and TerraBlender's.
@Mixin(value = MultiNoiseBiomeSource.class, priority = -100000, remap = false)
public abstract class BiomeSourceMixin {
    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"), cancellable = true)
    private void tfacfix$select(int x, int y, int z, Climate.Sampler sampler,
                               CallbackInfoReturnable<Holder<Biome>> cir) {
        var context = TerraforgedAlexCavesFix.context(sampler);
        if (context == null || !FixConfig.ENABLE_PLACEMENT.get()) return;
        Holder<Biome> selected = PlacementEngine.pick(context, sampler, x, y, z);
        // Fall through to the original XYZ pipeline, including TerraBlender's positional region selection.
        // NativeConditionMixin prevents the port from independently reintroducing a rejected cave candidate.
        if (selected != null) cir.setReturnValue(selected);
    }
}
