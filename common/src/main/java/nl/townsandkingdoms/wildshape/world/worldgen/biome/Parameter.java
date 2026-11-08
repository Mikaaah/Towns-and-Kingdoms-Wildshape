package nl.townsandkingdoms.wildshape.world.worldgen.biome;

import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noises;

public interface Parameter {
	float min();
	
	float max();
	
	default float mid() {
		return (this.min() + this.max()) / 2.0F;
	}
	
	default Noise source() {
		return Noises.constant(this.mid());
	}
}
