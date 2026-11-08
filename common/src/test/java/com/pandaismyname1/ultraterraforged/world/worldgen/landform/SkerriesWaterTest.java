package com.pandaismyname1.ultraterraforged.world.worldgen.landform;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.pandaismyname1.ultraterraforged.test.BuiltinPresetRenderTest;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;

class SkerriesWaterTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test void emptySpaceBetweenIsletsStaysUnderwater() {
        var preset = BuiltinPresetRenderTest.presets().get("default").get();
        var levels = GeneratorContext.makeUncached(preset, 1111, 1, 0, 1).levels;
        var climate = mock(ClimateGrid.class);
        when(climate.get(anyFloat(), anyFloat(), isNull())).thenReturn(1.0F);
        var skerries = new Skerries(4321, 0.5F, Noises.one(), Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Noises.zero(), levels, climate);
        int untouched = 0, raised = 0;
        for (int x = -160; x <= 160; x += 4) for (int z = -160; z <= 160; z += 4) {
            var cell = new Cell();
            cell.height = levels.water(-10);
            cell.continentEdge = 0.45F;
            cell.terrain = TerrainType.SHALLOW_OCEAN;
            skerries.apply(cell, x, z, null);
            if (cell.height == levels.water(-10)) untouched++;
            if (cell.height >= levels.water) raised++;
        }
        assertTrue(raised > 0, "no skerries generated");
        assertTrue(untouched > 3000, "water between islets became flat ground: only " + untouched + " unchanged cells");
    }
}
