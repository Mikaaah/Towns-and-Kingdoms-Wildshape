package nl.townsandkingdoms.wildshape.world.worldgen.biome;

import nl.townsandkingdoms.wildshape.world.worldgen.noise.NoiseUtil;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noises;

public interface BiomeParameter {
	float min();
	
	float max();
	
	default float lerp(float alpha) {
		return NoiseUtil.lerp(this.min(), this.max(), alpha);
	}
	
	default float midpoint() {
		return (this.min() + this.max()) / 2.0F;
	}
	
	default Noise source() {
		return Noises.constant(this.midpoint());
	}
}
