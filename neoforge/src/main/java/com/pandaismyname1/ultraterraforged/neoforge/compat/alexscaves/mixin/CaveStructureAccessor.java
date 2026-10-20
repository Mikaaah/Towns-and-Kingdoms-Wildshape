package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.level.structure.AbstractCaveGenerationStructure;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractCaveGenerationStructure.class, remap = false)
public interface CaveStructureAccessor {
    @Accessor("matchingBiome") ResourceKey<Biome> tfacfix$matchingBiome();
}
