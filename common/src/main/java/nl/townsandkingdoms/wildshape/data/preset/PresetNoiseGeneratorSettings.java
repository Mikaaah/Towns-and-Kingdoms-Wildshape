package nl.townsandkingdoms.wildshape.data.preset;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.townsandkingdoms.wildshape.data.preset.settings.CaveSettings;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.data.preset.settings.WorldSettings;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;

public class PresetNoiseGeneratorSettings {
	
	public static void bootstrap(Preset preset, BootstrapContext<NoiseGeneratorSettings> ctx) {
		HolderGetter<DensityFunction> densityFunctions = ctx.lookup(Registries.DENSITY_FUNCTION);
		HolderGetter<NormalNoise.NoiseParameters> noiseParams = ctx.lookup(Registries.NOISE);
		HolderGetter<Noise> noises = ctx.lookup(WildshapeRegistries.NOISE);
		
		WorldSettings worldSettings = preset.world();
		WorldSettings.Properties properties = worldSettings.properties;
		int worldHeight = properties.worldHeight;
		int worldDepth = properties.worldDepth;
//    	Levels levels = new Levels(properties.terrainScaler(), properties.seaLevel);
    	
		CaveSettings caveSettings = preset.caves();

		ctx.register(NoiseGeneratorSettings.OVERWORLD, new NoiseGeneratorSettings(
			NoiseSettings.create(-worldDepth, worldDepth + worldHeight, 1, 2), 
			Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), 
			PresetNoiseRouterData.overworld(preset, densityFunctions, noiseParams, noises),
			PresetSurfaceRuleData.overworld(preset, noises),
			properties.spawnType.getParameterPoints(), 
			properties.seaLevel, 
			false, 
			true, 
			caveSettings.largeOreVeins, 
			false
		));
    }
}
