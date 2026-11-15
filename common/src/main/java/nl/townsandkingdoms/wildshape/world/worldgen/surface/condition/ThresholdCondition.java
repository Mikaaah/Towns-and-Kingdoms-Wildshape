package nl.townsandkingdoms.wildshape.world.worldgen.surface.condition;

import net.minecraft.world.level.levelgen.SurfaceRules.Context;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;

abstract class ThresholdCondition extends CellCondition {
	private Noise threshold;
	private Noise variance;
	
	public ThresholdCondition(Context context, Noise threshold, Noise variance) {
		super(context);
		
		this.threshold = threshold;
		this.variance = variance;
	}

	@Override
	public boolean test(Cell cell, int x, int z) {
		return this.sample(cell) + this.variance.compute(x, z, 0) > this.threshold.compute(x, z, 0);
	}
	
	protected abstract float sample(Cell cell);
}
