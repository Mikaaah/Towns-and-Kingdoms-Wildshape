package nl.townsandkingdoms.wildshape.world.worldgen.biome.modifier;

import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;

import com.mojang.serialization.Codec;

import nl.townsandkingdoms.wildshape.registries.WildshapeBuiltInRegistries;

public interface BiomeModifier {
    public static final Codec<BiomeModifier> CODEC = WildshapeCodecs.dispatch(WildshapeBuiltInRegistries.BIOME_MODIFIER_TYPE, BiomeModifier::codec);
    
	Codec<? extends BiomeModifier> codec();
}
