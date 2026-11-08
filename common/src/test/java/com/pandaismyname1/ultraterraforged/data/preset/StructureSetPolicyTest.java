package com.pandaismyname1.ultraterraforged.data.preset;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.resources.ResourceLocation;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;

class StructureSetPolicyTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    private int spacing(String id, int nativeSpacing) {
        return StructureSetPolicy.spacing(ResourceLocation.parse(id), nativeSpacing);
    }

    @Test void resourcesCaveEcologyAndOtherDimensionsKeepNativeSpacing() {
        assertEquals(32, spacing("ae2:meteorite", 32));
        assertEquals(6, spacing("alexscaves:acid_pit", 6));
        assertEquals(4, spacing("alexscaves:dino_bowl", 4));
        assertEquals(16, spacing("hybrid_aquatic:whale_falls", 16));
        assertEquals(19, spacing("supplementaries:road_signs", 19));
        assertEquals(35, spacing("idas:idas_nether", 35));
        assertEquals(16, spacing("betterdungeons:small_nether_dungeons", 16));
        assertEquals(30, spacing("betterfortresses:fortress", 30));
        assertEquals(25, spacing("mowziesmobs:frostmaw_spawns", 25));
        assertEquals(32, spacing("cataclysm:amethyst_nest", 32));
        assertEquals(12, spacing("unknownmod:landmark", 12));
    }

    @Test void selectedDungeonsSpreadWithoutUndoingPackOverrides() {
        assertEquals(36, spacing("idas:idas_common", 21));
        assertEquals(64, spacing("dungeons_arise:major_structures", 40));
        assertEquals(40, spacing("integrated_villages:regular_villages", 26));
        assertEquals(80, spacing("idas:idas_common", 80));
    }
}
