package nl.townsandkingdoms.wildshape.compat.alexscaves;

// a climate sampler that knows whether it samples a world Wildshape makes (mixin.alexscaves.MixinClimateSampler)
public interface ACClimateSampler {

	boolean wildshape$isTerrain();

	void wildshape$setTerrain(boolean terrain);
}
