package nl.townsandkingdoms.wildshape.world.worldgen.heightmap;

import nl.townsandkingdoms.wildshape.world.worldgen.noise.module.Noise;

public record RegionConfig(int seed, int scale, Noise warpX, Noise warpZ, float warpStrength) {
}
