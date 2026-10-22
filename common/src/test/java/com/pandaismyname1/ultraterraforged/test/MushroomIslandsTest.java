package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.world.worldgen.biome.Continentalness;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Heightmap;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.MushroomIslands;

class MushroomIslandsTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test void isolatedDeepOceanIslandsReachLandAndKeepMushroomBiome() {
        Levels levels = new Levels(384, 63);
        MushroomIslands islands = new MushroomIslands((cell, x, z) -> {
            cell.height = levels.water(-25);
            cell.terrain = TerrainType.DEEP_OCEAN;
        }, levels, 0.1F, 12345, Noises.constant(0.5F));
        Heightmap heightmap = new Heightmap(null, null, null, null, null, levels, null, 1, null, null, null);
        Cell cell = new Cell();
        int dry = 0;
        for (int x = -4096; x <= 4096; x += 64) {
            for (int z = -4096; z <= 4096; z += 64) {
                cell.reset();
                cell.continentEdge = 0.0F;
                islands.apply(cell, x, z);
                if (cell.terrain != TerrainType.MUSHROOM_FIELDS) continue;
                assertTrue(cell.height >= levels.water(2));
                heightmap.applyPost(cell, x, z);
                assertTrue(cell.continentalness >= Continentalness.MUSHROOM_FIELDS.min());
                assertTrue(cell.continentalness < Continentalness.DEEP_OCEAN.min());
                dry++;
            }
        }
        assertTrue(dry > 10, "mushroom islands never rise out of deep water");
        cell.reset();
        cell.continentEdge = 0.2F;
        islands.apply(cell, 0, 0);
        assertEquals(TerrainType.DEEP_OCEAN, cell.terrain, "islands must not spread onto ordinary shelves");
    }
}
