package nl.townsandkingdoms.wildshape.world.worldgen.cell;

import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;

public interface CellPopulator {
    void apply(Cell cell, float x, float z);
    
    default CellPopulator mapNoise(Noise.Visitor visitor) {
    	return this;
    }
}
