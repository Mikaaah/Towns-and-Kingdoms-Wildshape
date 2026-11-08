package nl.townsandkingdoms.wildshape.world.worldgen.noise.function;

import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;

import com.mojang.serialization.Codec;

import nl.townsandkingdoms.wildshape.registries.WildshapeBuiltInRegistries;

public interface CurveFunction {
    public static final Codec<CurveFunction> CODEC = WildshapeCodecs.dispatch(WildshapeBuiltInRegistries.CURVE_FUNCTION_TYPE, CurveFunction::codec);
	
	float apply(float f);
	
	Codec<? extends CurveFunction> codec();
}
