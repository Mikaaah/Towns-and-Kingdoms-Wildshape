package nl.townsandkingdoms.wildshape.mixin.alexscaves;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.level.biome.Climate;
import nl.townsandkingdoms.wildshape.compat.alexscaves.ACClimateSampler;

// marked by MixinRandomState when it makes a sampler for a world Wildshape makes
@Mixin(Climate.Sampler.class)
class MixinClimateSampler implements ACClimateSampler {
	@Unique
	private boolean terrain;

	@Override
	public boolean wildshape$isTerrain() {
		return this.terrain;
	}

	@Override
	public void wildshape$setTerrain(boolean terrain) {
		this.terrain = terrain;
	}
}
