package com.pandaismyname1.ultraterraforged.world.worldgen.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

public interface UTFClimateSampler {
	void setBiomeFallback(Climate.ParameterList<Holder<Biome>> fallback);

	Holder<Biome> filterBiome(Holder<Biome> biome, int x, int y, int z);

	void setSpawnSearchCenter(BlockPos center);
	
	BlockPos getSpawnSearchCenter();
}
