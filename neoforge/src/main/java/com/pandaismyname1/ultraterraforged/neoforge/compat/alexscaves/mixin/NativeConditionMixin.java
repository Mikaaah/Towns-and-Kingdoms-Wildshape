package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.github.alexmodguy.alexscaves.server.misc.VoronoiGenerator;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.FixConfig;
import com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.TerraforgedAlexCavesFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BiomeGenerationNoiseCondition.class, remap = false)
public abstract class NativeConditionMixin {
    @Inject(method = "test(IIIFLnet/minecraft/world/level/biome/Climate$Sampler;Lnet/minecraft/resources/ResourceKey;Lcom/github/alexmodguy/alexscaves/server/misc/VoronoiGenerator$VoronoiInfo;)Z",
            at = @At("HEAD"), cancellable = true)
    private void tfacfix$selectionOwnedByCompat(int x, int y, int z, float depth, Climate.Sampler sampler,
            ResourceKey<Level> dimension, VoronoiGenerator.VoronoiInfo info, CallbackInfoReturnable<Boolean> cir) {
        if (TerraforgedAlexCavesFix.context(sampler) != null && FixConfig.ENABLE_PLACEMENT.get()) {
            // The higher-precedence TFAC source hook already evaluated all cave conditions.
            // Rejected candidates must continue to vanilla/TerraBlender, not the original incompatible filter.
            cir.setReturnValue(false);
        }
    }
}
