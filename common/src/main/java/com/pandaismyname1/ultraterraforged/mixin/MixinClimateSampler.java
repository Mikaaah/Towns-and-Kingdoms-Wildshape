package com.pandaismyname1.ultraterraforged.mixin;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.BiomeExclusions;
import net.minecraft.world.level.biome.Climate;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.UTFClimateSampler;

@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = UTFClimateSampler.class, prefix = "ultraterraforged$UTFClimateSampler$"))
class MixinClimateSampler {
	private Climate.ParameterList<Holder<Biome>> tnk$biomeFallback;

	public void ultraterraforged$UTFClimateSampler$setBiomeFallback(Climate.ParameterList<Holder<Biome>> fallback) {
		this.tnk$biomeFallback = fallback;
	}

	public Climate.ParameterList<Holder<Biome>> ultraterraforged$UTFClimateSampler$getBiomeFallback() {
		return this.tnk$biomeFallback;
	}

	public Holder<Biome> ultraterraforged$UTFClimateSampler$filterBiome(Holder<Biome> selected, int x, int y, int z) {
		if (this.tnk$biomeFallback == null || !BiomeExclusions.excludes(selected)) return selected;
		return BiomeExclusions.select(selected, this.tnk$biomeFallback,
			() -> ((Climate.Sampler) (Object) this).sample(x, y, z));
	}

	private BlockPos spawnSearchCenter = BlockPos.ZERO;
	
	public void ultraterraforged$UTFClimateSampler$setSpawnSearchCenter(BlockPos spawnSearchCenter) {
		this.spawnSearchCenter = spawnSearchCenter;
	}
	
	public BlockPos ultraterraforged$UTFClimateSampler$getSpawnSearchCenter() {
		return this.spawnSearchCenter;
	}
}
