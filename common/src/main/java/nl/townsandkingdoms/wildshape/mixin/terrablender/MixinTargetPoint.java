package nl.townsandkingdoms.wildshape.mixin.terrablender;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.biome.Climate;
import nl.townsandkingdoms.wildshape.compat.terrablender.TBTargetPoint;

@Mixin(Climate.TargetPoint.class)
@Implements(@Interface(iface = TBTargetPoint.class, prefix = "wildshape$TBTargetPoint$"))
class MixinTargetPoint {
	private double uniqueness = Double.NaN;

	public double wildshape$TBTargetPoint$getUniqueness() {
		return this.uniqueness;
	}
	
	public void wildshape$TBTargetPoint$setUniqueness(double uniqueness) {
		this.uniqueness = uniqueness;
	}
}
