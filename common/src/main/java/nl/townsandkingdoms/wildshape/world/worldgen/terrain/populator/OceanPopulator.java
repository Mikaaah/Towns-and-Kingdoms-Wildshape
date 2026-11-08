package nl.townsandkingdoms.wildshape.world.worldgen.terrain.populator;

import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.CellPopulator;
import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;
import nl.townsandkingdoms.wildshape.world.worldgen.terrain.Terrain;

public record OceanPopulator(Terrain terrainType, Noise height) implements CellPopulator {

	@Override
	public void apply(Cell cell, float x, float z) {
		cell.terrain = this.terrainType;
		cell.height = Math.max(this.height.compute(x, z, 0), 0.0F);
		
		//TODO dont do this
		cell.erosion = -1.1F;
		cell.weirdness = -1.1F;
	}
}
