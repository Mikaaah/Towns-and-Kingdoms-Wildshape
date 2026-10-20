package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.mixin;

import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.SamplerAccess;
import com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.WorldContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Climate.Sampler.class, remap = false)
public abstract class ClimateSamplerMixin implements SamplerAccess {
    @Unique private volatile WorldContext tfacfix$context;
    @Override public WorldContext tfacfix$getContext() { return tfacfix$context; }
    @Override public void tfacfix$setContext(WorldContext context) { tfacfix$context = context; }
}
