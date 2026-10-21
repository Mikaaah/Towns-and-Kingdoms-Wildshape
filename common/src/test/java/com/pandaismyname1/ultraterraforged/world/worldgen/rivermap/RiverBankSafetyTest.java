package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Random;
import org.junit.jupiter.api.Test;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.rivermap.river.RiverConfig;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.RiverPopulator;
class RiverBankSafetyTest {
    @org.junit.jupiter.api.BeforeAll static void bootstrap() { com.pandaismyname1.ultraterraforged.test.TestBootstrap.init(); }
    @Test void carverCannotBuildHighLeveeAcrossLowTerrain() throws Exception {
        Levels levels = new Levels(384,63);
        var carver = RiverPopulator.create(0,0,1000,0,RiverConfig.builder(levels).build(),levels,new Random(1));
        var carve = RiverPopulator.class.getDeclaredMethod("carveRaised",Cell.class,float.class,float.class,float.class,float.class,float.class);
        carve.setAccessible(true);
        Cell cell = new Cell(); cell.height=70/384f; cell.terrain=TerrainType.PLAINS;
        carve.invoke(carver,cell,81f,100f,94/384f,100/384f,.5f);
        assertEquals(70,levels.scale(cell.height),"the carver must not build a 31-block levee");
    }
}
