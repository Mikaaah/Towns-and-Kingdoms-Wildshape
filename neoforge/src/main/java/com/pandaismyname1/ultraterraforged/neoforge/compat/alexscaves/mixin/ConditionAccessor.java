package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(value = BiomeGenerationNoiseCondition.class, remap = false)
public interface ConditionAccessor {
    @Accessor("continentalness") float[] tfacfix$continentalness();
    @Accessor("depth") float[] tfacfix$depth();
    @Accessor("erosion") float[] tfacfix$erosion();
    @Accessor("temperature") float[] tfacfix$temperature();
    @Accessor("humidity") float[] tfacfix$humidity();
    @Accessor("weirdness") float[] tfacfix$weirdness();
    @Accessor("dimensions") List<String> tfacfix$dimensions();
}
