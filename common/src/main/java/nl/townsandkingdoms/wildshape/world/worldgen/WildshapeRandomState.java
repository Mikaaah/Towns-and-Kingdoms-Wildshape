package nl.townsandkingdoms.wildshape.world.worldgen;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;

public interface WildshapeRandomState {
	void initialize(RegistryAccess registryAccess);
	
	@Nullable
	RegistryAccess registryAccess();
	
	@Nullable
	Preset preset();

	@Nullable
	GeneratorContext generatorContext();

	// whether the world's terrain reads Wildshape's cells: known from the start, before the preset is loaded
	boolean usesCells();
	
	DensityFunction wrap(DensityFunction function);

	Noise wrap(Noise noise);
}
