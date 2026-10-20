package com.pandaismyname1.ultraterraforged.world.worldgen.rivermap;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.RiverPopulator;

class WaterContinuityTest {
    @Test void forcedRiverJunctionDropSpreadsOverFeasibleReaches() {
        Levels levels = new Levels(384, 63);
        int[] profile = {100, 100, 100, 100, 90};
        float[] land = {104/384f, 104/384f, 104/384f, 104/384f, 94/384f};
        WaterLevels.easeMouthTransition(profile, land, 32/384f, levels);
        assertArrayEquals(new int[]{100, 99, 96, 93, 90}, profile);
    }

    @Test void gorgeLimitStillAppliesAtImpossibleJunctions() {
        Levels levels = new Levels(384, 63);
        int[] profile = {107, 105, 100, 69};
        float[] land = {110/384f, 108/384f, 105/384f, 72/384f};
        WaterLevels.easeMouthTransition(profile, land, 8/384f, levels);
        assertEquals(97, profile[2]);
        assertEquals(69, profile[3]);
    }

    @Test void gentleWaterProfilesRemainUnchanged() {
        Levels levels = new Levels(384, 63);
        int[] profile = {90, 88, 86, 84};
        float[] land = {94/384f, 92/384f, 90/384f, 88/384f};
        WaterLevels.easeMouthTransition(profile, land, 32/384f, levels);
        assertArrayEquals(new int[]{90, 88, 86, 84}, profile);
    }

    @Test void interpolatedWaterTransitionStaysBetweenEndpoints() throws Exception {
        var method = RiverPopulator.class.getDeclaredMethod("sample", float[].class, float.class);
        method.setAccessible(true);
        float[] endpoints = {100f,95f};
        assertEquals(100f, (float)method.invoke(null,endpoints,0f), .01f);
        assertEquals(97.5f, (float)method.invoke(null,endpoints,.5f), .01f);
        assertEquals(95f, (float)method.invoke(null,endpoints,1f), .01f);
    }
}
