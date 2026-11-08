package nl.townsandkingdoms.wildshape.world.worldgen.noise.domain;

import nl.townsandkingdoms.wildshape.data.WildshapeCodecs;

import com.mojang.serialization.Codec;

import nl.townsandkingdoms.wildshape.registries.WildshapeBuiltInRegistries;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;

public interface Domain {
    public static final Codec<Domain> CODEC = WildshapeCodecs.dispatch(WildshapeBuiltInRegistries.DOMAIN_TYPE, Domain::codec);
	
    float getOffsetX(float x, float z, int seed);
    
    float getOffsetZ(float x, float z, int seed);
    
    Domain mapAll(Noise.Visitor visitor);
    
    Codec<? extends Domain> codec();

    default float getX(float x, float z, int seed) {
        return x + this.getOffsetX(x, z, seed);
    }
    
    default float getZ(float x, float z, int seed) {
        return z + this.getOffsetZ(x, z, seed);
    }
}
