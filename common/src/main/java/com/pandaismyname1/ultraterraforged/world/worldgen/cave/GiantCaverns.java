package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.NoiseUtil;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noise;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;

/**
 * Vast, predominantly deep underground caverns. High terrain increases the
 * CHANCE of a chamber, not its elevation: cavern roofs stay at Y30 or below,
 * with at least 30 blocks of terrain above the sampled footprint.
 *
 * Every chamber is based only on seed and grid position, so adjacent chunks
 * carve the same shape regardless of chunk generation order.
 */
final class GiantCaverns implements CaveFeatures.Feature {
    private static final float GRID = 640.0F;
    private static final int MIN_ROOF_COVER = 30;
    private static final int MAX_ROOF_Y = 30;
    private static final Cavern NONE = new Cavern(0, 0, 0, 0, 0, 0, Integer.MIN_VALUE, 0);

    private final int seed;
    private final float frequency;
    private final Noise walls;
    private final Noise pillars;
    private final SiteCache<Cavern> caverns = SiteCache.make();

    record Cavern(float x, float y, float z, float radiusX, float radiusY,
                  float radiusZ, int roof, int terrainHeight) {}

    GiantCaverns(int seed, float frequency) {
        this.seed = seed;
        this.frequency = NoiseUtil.clamp(frequency, 0.0F, 1.0F);
        this.walls = Noises.perlin(seed + 1, 24, 2);
        this.pillars = Noises.worleyEdge(seed + 2, 27);
    }

    @Override
    public void carve(CaveCarving carving) {
        int gridX = NoiseUtil.floor(carving.minX / GRID);
        int gridZ = NoiseUtil.floor(carving.minZ / GRID);
        for (int gx = gridX - 1; gx <= gridX + 1; gx++) {
            for (int gz = gridZ - 1; gz <= gridZ + 1; gz++) {
                // Cheap early rejection; terrain-dependent acceptance below
                // intentionally favors continents, foothills and mountains.
                float draw = CaveFeatures.random(this.seed, gx, gz, 0);
                if (draw >= this.frequency * 1.0F) continue;
                Cavern cavern = this.caverns.get(gx, gz, (cx, cz) -> this.find(cx, cz, carving));
                if (cavern == NONE) continue;
                float highland = NoiseUtil.clamp((cavern.terrainHeight() - carving.levels.waterLevel - 18.0F) / 120.0F, 0.0F, 1.0F);
                float probability = this.frequency * (0.42F + 0.58F * highland);
                if (draw >= probability) continue;
                float reach = Math.max(cavern.radiusX(), cavern.radiusZ()) * 1.22F;
                if (carving.reaches(cavern.x() - reach, cavern.z() - reach, cavern.x() + reach, cavern.z() + reach)) {
                    this.hollow(cavern, carving);
                }
            }
        }
    }

    private void hollow(Cavern cavern, CaveCarving carving) {
        int lake = (int) (cavern.y() - cavern.radiusY() * 0.53F);
        int y0 = Math.max(carving.bottom + 3, (int) (cavern.y() - cavern.radiusY() * 1.12F));
        int y1 = Math.min(cavern.roof(), (int) (cavern.y() + cavern.radiusY() * 1.12F));
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int x = carving.minX + dx;
                int z = carving.minZ + dz;
                float nx = (x - cavern.x()) / cavern.radiusX();
                float nz = (z - cavern.z()) / cavern.radiusZ();
                float flat = nx * nx + nz * nz;
                if (flat > 1.35F) continue;
                boolean pillar = this.pillars.compute(x, z, 0) < 0.12F && flat < 0.7F;
                // Terrain above the cavern is protected even at the edge of a
                // steep valley that falls between the 9 site-height probes.
                int top = Math.min(y1, carving.ground(x, z) - MIN_ROOF_COVER);
                for (int y = y0; y <= top; y++) {
                    float ny = (y - cavern.y()) / cavern.radiusY();
                    float shape = flat + ny * ny;
                    if (shape >= 1.35F) continue; // no expensive wall noise needed
                    float ragged = (this.walls.compute(x + y * 0.7F, z - y * 0.7F, 0) - 0.5F) * 0.7F;
                    if (shape + ragged >= 1.0F || (pillar && y > lake)) continue;
                    carving.clear(x, y, z, lake);
                }
            }
        }
    }

    Cavern find(int gridX, int gridZ, CaveCarving carving) {
        float margin = 185.0F;
        float x = gridX * GRID + margin + CaveFeatures.random(this.seed, gridX, gridZ, 1) * (GRID - 2 * margin);
        float z = gridZ * GRID + margin + CaveFeatures.random(this.seed, gridX, gridZ, 2) * (GRID - 2 * margin);
        float radiusX = NoiseUtil.lerp(115.0F, 175.0F, CaveFeatures.random(this.seed, gridX, gridZ, 3));
        float radiusZ = NoiseUtil.lerp(105.0F, 165.0F, CaveFeatures.random(this.seed, gridX, gridZ, 4));
        float desiredY = NoiseUtil.lerp(42.0F, 66.0F, CaveFeatures.random(this.seed, gridX, gridZ, 5));

        int lowest = Integer.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            float angle = i * NoiseUtil.PI2 / 8.0F;
            float r = i == 8 ? 0.0F : 1.15F;
            lowest = Math.min(lowest, carving.terrainHeight(x + NoiseUtil.cos(angle) * radiusX * r,
                                                               z + NoiseUtil.sin(angle) * radiusZ * r));
        }
        int roof = Math.min(MAX_ROOF_Y, lowest - MIN_ROOF_COVER);
        // On a vanilla -64 bottom, Y30 allows roughly 80 blocks of cavern;
        // deeper presets naturally support taller caverns up to ~130 blocks.
        float maxRadiusY = (roof - carving.bottom - 9.0F) * 0.49F;
        float radiusY = Math.min(desiredY, maxRadiusY);
        if (radiusY < 18.0F) return NONE;
        float y = roof - radiusY;
        if (y - radiusY < carving.bottom + 5) return NONE;
        return new Cavern(x, y, z, radiusX, radiusY, radiusZ, roof, lowest);
    }
}
