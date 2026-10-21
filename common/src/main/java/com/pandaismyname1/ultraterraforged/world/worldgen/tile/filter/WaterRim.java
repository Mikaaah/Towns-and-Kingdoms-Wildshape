package com.pandaismyname1.ultraterraforged.world.worldgen.tile.filter;

import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;

/**
 * Bounds raised water against the finished banks after landforms, erosion and smoothing: dry ground right beside
 * it that's lower than the water, where a bank was worn down, is raised to the water's surface, and single spots of
 * the bed left standing out of the water are sunk back under it.
 */
public record WaterRim(Levels levels) implements Filter {
    private static final int MAX_FILL = 2;
    // A finite neighborhood fits inside generation tile borders. Unlike an
    // unbounded flood, results in the tile interior do not depend on tile size.
    private static final int SUPPORT_RADIUS = 12;

    private void constrainWater(Tile tile, int size) {
        float[] limit = new float[size * size];
        int[] wet = new int[limit.length];
        int count = 0;
        for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) {
            Cell cell = tile.getCellRaw(x, z);
            if (this.wet(cell) && cell.terrain != TerrainType.CIRQUE
                    && cell.waterLevel > this.levels.water) {
                int index = z * size + x;
                wet[count++] = index;
                // Unsupported deep source columns are not made by the block filler.
                limit[index] = Math.min(cell.waterLevel, cell.height + 24 * this.levels.unit);
            }
        }
        if (count == 0) return;
        float[] next = limit.clone();
        float slope = 0.25F * this.levels.unit;
        float fill = MAX_FILL * this.levels.unit;
        for (int pass = 0; pass < SUPPORT_RADIUS; pass++) {
            boolean changed = false;
            for (int n = 0; n < count; n++) {
                int index = wet[n], x = index % size, z = index / size;
                float cap = limit[index];
                if (x > 0) cap = Math.min(cap, this.neighborLimit(tile, x - 1, z, limit[index - 1], slope, fill));
                if (x + 1 < size) cap = Math.min(cap, this.neighborLimit(tile, x + 1, z, limit[index + 1], slope, fill));
                if (z > 0) cap = Math.min(cap, this.neighborLimit(tile, x, z - 1, limit[index - size], slope, fill));
                if (z + 1 < size) cap = Math.min(cap, this.neighborLimit(tile, x, z + 1, limit[index + size], slope, fill));
                cap = Math.max(this.levels.water, cap);
                next[index] = cap;
                changed |= cap < limit[index];
            }
            float[] swap = limit; limit = next; next = swap;
            if (!changed) break;
        }
        for (int n = 0; n < count; n++) {
            int index = wet[n];
            Cell cell = tile.getCellRaw(index % size, index / size);
            cell.waterLevel = limit[index] > cell.height ? limit[index] : 0.0F;
        }
    }

    private float neighborLimit(Tile tile, int x, int z, float limit, float slope, float fill) {
        Cell neighbor = tile.getCellRaw(x, z);
        if (limit > 0.0F) return limit + slope;
        if (this.wet(neighbor)) return neighbor.waterLevel + slope;
        return Math.max(this.levels.water, neighbor.height + fill);
    }


	@Override
	public void apply(Tile tile, int regionX, int regionZ, int iterationsPerChunks) {
		int size = tile.getBlockSize().total();
        this.constrainWater(tile, size);
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++) {
				Cell cell = tile.getCellRaw(x, z);
				int y = this.levels.scale(cell.height);
				if (cell.waterLevel > 0.0F && !this.wet(cell) && x > 0 && z > 0 && x < size - 1 && z < size - 1) {
					int around = this.wetCount(tile.getCellRaw(x - 1, z)) + this.wetCount(tile.getCellRaw(x + 1, z)) + this.wetCount(tile.getCellRaw(x, z - 1)) + this.wetCount(tile.getCellRaw(x, z + 1));
					if (around >= 3 && cell.height - cell.waterLevel <= MAX_FILL * this.levels.unit) {
						cell.height = Math.min(cell.height, cell.waterLevel - this.levels.unit);
						continue;
					}
				}
				// wet ground, and ground under the sea, stay as they are; so does the lip of a cirque, where its lake spills
				// over down the mountain
				if (this.wet(cell) || y < this.levels.waterY || cell.terrain == TerrainType.CIRQUE) {
					continue;
				}
				float water = 0.0F;
				if (x > 0) water = Math.max(water, this.surface(tile.getCellRaw(x - 1, z)));
				if (x < size - 1) water = Math.max(water, this.surface(tile.getCellRaw(x + 1, z)));
				if (z > 0) water = Math.max(water, this.surface(tile.getCellRaw(x, z - 1)));
				if (z < size - 1) water = Math.max(water, this.surface(tile.getCellRaw(x, z + 1)));
				if (y < this.levels.scale(water) && water - cell.height <= MAX_FILL * this.levels.unit + 1.0E-6F) {
					cell.height = water;
				}
			}
		}
	}

	private boolean wet(Cell cell) {
		return cell.waterLevel > 0.0F && this.levels.scale(cell.height) < this.levels.scale(cell.waterLevel);
	}

	private int wetCount(Cell cell) {
		return this.wet(cell) ? 1 : 0;
	}

	// the water surface over a cell, or 0 if it's dry
	private float surface(Cell cell) {
		return this.wet(cell) ? cell.waterLevel : 0.0F;
	}
}
