package nl.townsandkingdoms.wildshape.mixin.terrablender;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.TargetPoint;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.townsandkingdoms.wildshape.compat.terrablender.TBClimateSampler;
import nl.townsandkingdoms.wildshape.compat.terrablender.TBTargetPoint;

@Mixin(Climate.Sampler.class)
@Implements(@Interface(iface = TBClimateSampler.class, prefix = "wildshape$TBClimateSampler$"))
class MixinClimateSampler {
	@Nullable
	private DensityFunction uniqueness;
	
	@Inject(
		at = @At("RETURN"), 
		method = "sample",
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	public void sample(int i, int j, int k, CallbackInfoReturnable<TargetPoint> callback, int l, int m, int n, DensityFunction.SinglePointContext ctx) {
		if(this.uniqueness != null && (Object) callback.getReturnValue() instanceof TBTargetPoint tbTargetPoint) {
			tbTargetPoint.setUniqueness(this.uniqueness.compute(ctx));
		}
	}
	
	public void wildshape$TBClimateSampler$setUniqueness(DensityFunction uniqueness) {
		this.uniqueness = uniqueness;
	}
	
	public DensityFunction wildshape$TBClimateSampler$getUniqueness() {
		return this.uniqueness;
	}
}
