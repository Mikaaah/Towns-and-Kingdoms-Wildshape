package nl.townsandkingdoms.wildshape.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.google.common.base.Suppliers;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.BiomeExclusions;
import nl.townsandkingdoms.wildshape.world.worldgen.biome.WildshapeClimateSampler;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.compat.alexscaves.ACClimateSampler;
import nl.townsandkingdoms.wildshape.compat.alexscaves.AlexsCavesCompat;
import nl.townsandkingdoms.wildshape.compat.terrablender.TBClimateSampler;
import nl.townsandkingdoms.wildshape.compat.terrablender.TBCompat;
import nl.townsandkingdoms.wildshape.compat.terrablender.TBNoiseRouterData;
import nl.townsandkingdoms.wildshape.concurrent.ThreadPools;
import nl.townsandkingdoms.wildshape.config.PerformanceConfig;
import nl.townsandkingdoms.wildshape.data.preset.PresetData;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;
import nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState;
import nl.townsandkingdoms.wildshape.world.worldgen.densityfunction.CellSampler;
import nl.townsandkingdoms.wildshape.world.worldgen.densityfunction.NoiseSampler;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noises;

@Mixin(RandomState.class)
@Implements(@Interface(iface = WildshapeRandomState.class, prefix = "wildshape$WildshapeRandomState$"))
class MixinRandomState {
	@Shadow
	@Final
	private Climate.Sampler sampler;
	@Shadow
	@Final
    private SurfaceSystem surfaceSystem;

	@Nullable
	private Preset preset;
	@Nullable
	private NoiseSampler globalSampler;
	@Nullable
	private RegistryAccess registryAccess;

	@Deprecated
	private boolean hasContext;
	@Deprecated
	@Nullable
	private GeneratorContext generatorContext;

	private long seed;
	private DensityFunction.Visitor densityFunctionWrapper;

	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/NoiseRouter;"
		),
		method = "<init>",
		require = 1
	)
	private NoiseRouter RandomState(NoiseRouter router, DensityFunction.Visitor visitor, NoiseGeneratorSettings noiseGeneratorSettings, HolderGetter<NormalNoise.NoiseParameters> params, final long seed) {
		this.seed = seed;
		this.densityFunctionWrapper = new DensityFunction.Visitor() {

			@Override
			public DensityFunction apply(DensityFunction function) {
				if(function instanceof NoiseSampler.Marker marker) {
					return new NoiseSampler(marker.noise(), (int) seed);
				}
				if(function instanceof CellSampler.Marker marker) {
					MixinRandomState.this.hasContext |= true;
					return new CellSampler(Suppliers.memoize(() -> MixinRandomState.this.generatorContext), marker.field());
				}
				return visitor.apply(function);
			}

			@Override
			public NoiseHolder visitNoise(NoiseHolder noiseHolder) {
	            return visitor.visitNoise(noiseHolder);
	        }
		};
		return router.mapAll(this.densityFunctionWrapper);
	}

	public void wildshape$WildshapeRandomState$initialize(RegistryAccess registryAccess) {
		this.registryAccess = registryAccess;
		if (this.hasContext) {
			var overworld = registryAccess.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
				.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, net.minecraft.resources.ResourceLocation.withDefaultNamespace("overworld"))).value();
			((WildshapeClimateSampler) (Object) this.sampler).setBiomeFallback(BiomeExclusions.fallback(overworld.parameters()));
		}

		RegistryLookup<Preset> presets = registryAccess.lookupOrThrow(WildshapeRegistries.PRESET);
		RegistryLookup<DensityFunction> functions = registryAccess.lookupOrThrow(Registries.DENSITY_FUNCTION);

		// only in a world whose terrain reads the cells: mapping the uniqueness, a cell too, would count any other
		// dimension TerraBlender places biomes in, such as the Nether, as Wildshape's and generate it as one
		if((Object) this.sampler instanceof TBClimateSampler tbClimateSampler && TBCompat.isEnabled() && this.hasContext) {
			functions.get(TBNoiseRouterData.UNIQUENESS).ifPresent((uniqueness) -> {
				tbClimateSampler.setUniqueness(uniqueness.value().mapAll(this.densityFunctionWrapper));
			});
		}
		if(this.hasContext && AlexsCavesCompat.isEnabled()) {
			((ACClimateSampler) (Object) this.sampler).wildshape$setTerrain(true);
		}

		presets.get(PresetData.PRESET).ifPresent((presetHolder) -> {
			this.preset = presetHolder.value();

			if(this.hasContext) {
				//TODO move this somewhere else
				// Cached contexts are independent; no global cache reset on new worlds.

				PerformanceConfig config = PerformanceConfig.read(PerformanceConfig.DEFAULT_FILE_PATH)
					.resultOrPartial(WildshapeCommon.LOGGER::error)
					.orElseGet(PerformanceConfig::makeDefault);
                ThreadPools.configure(config.threadCount());
				this.generatorContext = GeneratorContext.makeCached(this.preset, (int) this.seed, config.tileSize(), config.batchCount(), ThreadPools.availableProcessors() > 4);
				((WildshapeClimateSampler) (Object) this.sampler).setGeneratorContext(this.generatorContext);
			}
		});
	}

	public boolean wildshape$WildshapeRandomState$usesCells() {
		return this.hasContext;
	}

	@Nullable
	public RegistryAccess wildshape$WildshapeRandomState$registryAccess() {
		return this.registryAccess;
	}

	@Nullable
	public Preset wildshape$WildshapeRandomState$preset() {
		return this.preset;
	}

	@Nullable
	public GeneratorContext wildshape$WildshapeRandomState$generatorContext() {
		return this.generatorContext;
	}

	@Nullable
	public DensityFunction wildshape$WildshapeRandomState$wrap(DensityFunction function) {
		return function.mapAll(this.densityFunctionWrapper);
	}

	public Noise wildshape$WildshapeRandomState$wrap(Noise noise) {
		return Noises.shiftSeed(noise, (int) this.seed);
	}
}
