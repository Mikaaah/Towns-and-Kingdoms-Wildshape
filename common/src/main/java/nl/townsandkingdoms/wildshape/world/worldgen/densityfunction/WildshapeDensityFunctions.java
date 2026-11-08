package nl.townsandkingdoms.wildshape.world.worldgen.densityfunction;

import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;
import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.townsandkingdoms.wildshape.platform.RegistryUtil;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.CellField;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;	

public class WildshapeDensityFunctions {

	public static void bootstrap() {
		register("noise_sampler", NoiseSampler.Marker.CODEC);
		register("cell", CellSampler.Marker.CODEC);
		register("clamp_to_nearest_unit", ClampToNearestUnit.CODEC);
		register("linear_spline", LinearSplineFunction.CODEC);
	}
	
	public static NoiseSampler.Marker noise(Holder<Noise> noise) {
		return new NoiseSampler.Marker(noise);
	}
	
	public static CellSampler.Marker cell(CellField field) {
		return new CellSampler.Marker(field);
	}
	
	public static ClampToNearestUnit clampToNearestUnit(DensityFunction function, int resolution) {
		return new ClampToNearestUnit(function, resolution);
	}
	
	private static void register(String name, Codec<? extends DensityFunction> type) {
		RegistryUtil.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, name, WildshapeCodecs.entry(type));
	}
}
