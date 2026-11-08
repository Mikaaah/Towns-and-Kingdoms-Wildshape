package nl.townsandkingdoms.wildshape.mixin;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.BiomeExclusions;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Climate;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.WildshapeClimateSampler;

@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = WildshapeClimateSampler.class, prefix = "wildshape$WildshapeClimateSampler$"))
class MixinClimateSampler {
	private Climate.ParameterList<Holder<Biome>> tnk$biomeFallback;
	private GeneratorContext tnk$generatorContext;
	private Holder<Biome> tnk$cherryGrove;
	private static final ThreadLocal<Cell> tnk$cell = ThreadLocal.withInitial(Cell::new);

	public void wildshape$WildshapeClimateSampler$setBiomeFallback(Climate.ParameterList<Holder<Biome>> fallback) {
		this.tnk$biomeFallback = fallback;
		this.tnk$cherryGrove = fallback == null ? null : fallback.values().stream()
			.map(com.mojang.datafixers.util.Pair::getSecond)
			.filter(biome -> biome.is(net.minecraft.world.level.biome.Biomes.CHERRY_GROVE))
			.findFirst().orElse(null);
	}

	public Climate.ParameterList<Holder<Biome>> wildshape$WildshapeClimateSampler$getBiomeFallback() {
		return this.tnk$biomeFallback;
	}

	public Holder<Biome> wildshape$WildshapeClimateSampler$getCherryGrove() {
		return this.tnk$cherryGrove;
	}

	public void wildshape$WildshapeClimateSampler$copyBiomeSelection(
			nl.townsandkingdoms.wildshape.world.worldgen.biome.WildshapeClimateSampler source) {
		this.tnk$biomeFallback = source.getBiomeFallback();
		this.tnk$cherryGrove = source.getCherryGrove();
		this.tnk$generatorContext = source.getGeneratorContext();
	}

	public void wildshape$WildshapeClimateSampler$setGeneratorContext(GeneratorContext context) {
		this.tnk$generatorContext = context;
	}

	public GeneratorContext wildshape$WildshapeClimateSampler$getGeneratorContext() {
		return this.tnk$generatorContext;
	}

	public Holder<Biome> wildshape$WildshapeClimateSampler$filterBiome(Holder<Biome> selected, int x, int y, int z) {
		if (this.tnk$biomeFallback == null) return selected;
		if (BiomeExclusions.excludes(selected)) return BiomeExclusions.select(selected, this.tnk$biomeFallback,
			() -> ((Climate.Sampler) (Object) this).sample(x, y, z));
		if (this.tnk$cherryGrove != null && this.tnk$generatorContext != null
				&& selected.is(net.minecraft.world.level.biome.Biomes.MEADOW)) {
			Cell cell = this.tnk$cell.get().reset();
			this.tnk$generatorContext.lookup.apply(cell, QuartPos.toBlock(x), QuartPos.toBlock(z));
			if (BiomeExclusions.promoteMeadow(cell.biomeRegionId)) return this.tnk$cherryGrove;
		}
		return selected;
	}

	private BlockPos spawnSearchCenter = BlockPos.ZERO;
	
	public void wildshape$WildshapeClimateSampler$setSpawnSearchCenter(BlockPos spawnSearchCenter) {
		this.spawnSearchCenter = spawnSearchCenter;
	}
	
	public BlockPos wildshape$WildshapeClimateSampler$getSpawnSearchCenter() {
		return this.spawnSearchCenter;
	}
}
