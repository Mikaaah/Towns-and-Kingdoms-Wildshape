package com.pandaismyname1.ultraterraforged.world.worldgen.surface.rule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.world.level.block.Blocks;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;

class StrataMaterialsTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test void resourcesCannotBecomeStoneLayers() {
        assertTrue(StrataMaterials.isResource(Blocks.DIAMOND_ORE.defaultBlockState()));
        assertTrue(StrataMaterials.isResource(Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()));
        assertTrue(StrataMaterials.isResource(Blocks.IRON_ORE.defaultBlockState()));
        assertTrue(StrataMaterials.oreName("zenalite_ore"));
        assertTrue(StrataMaterials.oreName("ore_copper"));
        assertTrue(StrataMaterials.oreName("rich_ore_stone"));
    }

    @Test void ordinaryAndCreateDecorativeStoneRemainEligible() {
        assertFalse(StrataMaterials.isResource(Blocks.ANDESITE.defaultBlockState()));
        assertFalse(StrataMaterials.isResource(Blocks.CALCITE.defaultBlockState()));
        for (String path : new String[] {"asurine", "crimsite", "limestone", "ochrum", "scorchia", "scoria", "veridium"}) {
            assertFalse(StrataMaterials.oreName(path), path);
        }
    }
}
