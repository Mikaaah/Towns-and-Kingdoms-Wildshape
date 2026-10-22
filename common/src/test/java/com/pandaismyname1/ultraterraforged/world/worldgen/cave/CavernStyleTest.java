package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import net.minecraft.world.level.block.Blocks;
import com.pandaismyname1.ultraterraforged.test.*;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;

class CavernStyleTest {
    @BeforeAll static void boot() { TestBootstrap.init(); }

    @Test void pillarsFlareAndRemainConnectedBelowTheLake() {
        var c=new GiantCaverns.Cavern(0,0,0,120,40,120,40,100);
        var pillar=new GiantCaverns.Pillar(0,0,5,0.8F,0.9F);
        int waist=0,base=0;
        for (int z=-30;z<=30;z++) for (int x=-30;x<=30;x++) {
            if (GiantCaverns.pillarAt(pillar,x,0,z,c)) waist++;
            if (GiantCaverns.pillarAt(pillar,x,-36,z,c)) base++;
        }
        assertTrue(base>waist*3,"pillar still has an extruded, constant cross-section");
        for(int y=-40;y<=40;y++) assertTrue(GiantCaverns.pillarAt(pillar,0,y,0,c),"unanchored spine at "+y);
    }

    private CaveWorld world() {
        return new CaveWorld(GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(),
                PresetRenderer.SEED,3,6,false));
    }

    @Test void lanternRequiresDryClearanceAndSolidCeiling() {
        var world=world();
        var carving=new CaveCarving(world.chunk(0,0),world.context);
        for(int y=-45;y<=-20;y++) carving.clear(5,y,5,CaveCarving.DRY);
        CavernDecorations.hangLantern(carving,5,-19,5,5);
        assertTrue(world.get(5,-24,5).is(Blocks.LANTERN));
        for(int y=-23;y<=-20;y++) assertTrue(world.get(5,y,5).is(Blocks.CHAIN));
        carving.clear(6,-20,6,CaveCarving.DRY);
        CavernDecorations.hangLantern(carving,6,-20,6,4);
        assertFalse(world.get(6,-24,6).is(Blocks.LANTERN),"lamp hung from air");
        for(int y=-45;y<=-20;y++) carving.clear(7,y,7,-20);
        CavernDecorations.hangLantern(carving,7,-19,7,5);
        assertTrue(world.get(7,-24,7).is(Blocks.WATER),"lamp replaced water");
    }

    @Test void lushChambersGrowMossAndVinesOnExistingRock() {
        var world=world();
        var carving=new CaveCarving(world.chunk(0,0),world.context);
        // A broad dry chamber shelf and ceiling, independent of terrain sampling.
        for(int z=0;z<16;z++) for(int x=0;x<16;x++)
            for(int y=-40;y<=-10;y++) carving.clear(x,y,z,CaveCarving.DRY);
        var c=new GiantCaverns.Cavern(8,-25,8,100,40,100,15,100);
        CavernDecorations.decorate(carving,c,CavernDecorations.Style.LUSH);
        assertTrue(world.count(s->s.is(Blocks.MOSS_BLOCK))>20,"lush style has no moss floor");
        assertTrue(world.count(s->s.is(Blocks.CAVE_VINES))>0,"lush style has no glowberry vines");
    }
}
