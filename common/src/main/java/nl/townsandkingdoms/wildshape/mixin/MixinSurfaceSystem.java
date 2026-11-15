package nl.townsandkingdoms.wildshape.mixin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.compat.terrablender.TBCompat;
import nl.townsandkingdoms.wildshape.world.worldgen.surface.WildshapeSurfaceSystem;
import nl.townsandkingdoms.wildshape.world.worldgen.surface.rule.StrataStack;
import terrablender.worldgen.surface.NamespacedSurfaceRuleSource;

@Mixin(SurfaceSystem.class)
@Implements(@Interface(iface = WildshapeSurfaceSystem.class, prefix = WildshapeCommon.MOD_ID + "$WildshapeSurfaceSystem$"))
class MixinSurfaceSystem {
	private static final ResourceLocation STRATA_RANDOM = WildshapeCommon.location("strata");
	private RandomSource strataRandom;
    private RandomState wildshapeSurfaceState;
    private final Map<SurfaceRules.RuleSource, SurfaceRules.RuleSource> wildshapeModRules = new ConcurrentHashMap<>();
	private Map<ResourceLocation, List<StrataStack>> strata;
	
	@Inject(
		at = @At("TAIL"),
		method = "<init>"
	)
    public void SurfaceSystem(RandomState randomState, BlockState blockState, int i, PositionalRandomFactory positionalRandomFactory, CallbackInfo callback) {
        this.wildshapeSurfaceState = randomState;
    	this.strataRandom = randomState.random.fromHashOf(STRATA_RANDOM);
    	this.strata = new ConcurrentHashMap<>();
	}
	
	@ModifyVariable(
		at = @At("HEAD"),
		method = "buildSurface",
		name = "ruleSource",
		ordinal = 0,
		index = 7,
		argsOnly = true
	)
	public SurfaceRules.RuleSource buildSurface(SurfaceRules.RuleSource source) {
        if ((Object)this.wildshapeSurfaceState instanceof nl.townsandkingdoms.wildshape.world.worldgen.WildshapeRandomState wildshape
                && wildshape.generatorContext() != null && wildshape.registryAccess() != null) {
            return this.wildshapeModRules.computeIfAbsent(source, rules ->
                    nl.townsandkingdoms.wildshape.world.worldgen.surface.ModSurfaceRules.wrap(rules,wildshape.registryAccess()));
        }
		return source;
	}

	public List<StrataStack> wildshape$WildshapeSurfaceSystem$getOrCreateStrata(ResourceLocation cacheId, Function<RandomSource, List<StrataStack>> factory) {
		return this.strata.computeIfAbsent(cacheId, (k) -> {
			return factory.apply(this.strataRandom.fork());
		});
	}
}
