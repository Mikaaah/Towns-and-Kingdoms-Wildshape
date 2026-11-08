package nl.townsandkingdoms.wildshape.data.preset;

import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noises;

public class PresetNoiseData {

	public static void bootstrap(Preset preset, BootstrapContext<Noise> ctx) {
		PresetTerrainNoise.bootstrap(preset, ctx);
		PresetClimateNoise.bootstrap(preset, ctx);
		PresetSurfaceNoise.bootstrap(preset, ctx);
		PresetStrataNoise.bootstrap(preset, ctx);
		PresetFeatureNoise.bootstrap(preset, ctx);
	}
	
	public static Noise getNoise(HolderGetter<Noise> noiseLookup, ResourceKey<Noise> key) {
		return new Noises.HolderHolder(noiseLookup.getOrThrow(key));
	}
	
	public static Noise registerAndWrap(BootstrapContext<Noise> ctx, ResourceKey<Noise> key, Noise noise) {
		return new Noises.HolderHolder(ctx.register(key, noise));
	}
	
	public static ResourceKey<Noise> createKey(String name) {
        return ResourceKey.create(WildshapeRegistries.NOISE, WildshapeCommon.location(name));
	}
}
