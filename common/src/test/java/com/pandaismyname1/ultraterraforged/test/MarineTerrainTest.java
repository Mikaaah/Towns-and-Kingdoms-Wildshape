package com.pandaismyname1.ultraterraforged.test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.pandaismyname1.ultraterraforged.world.worldgen.*;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import com.pandaismyname1.ultraterraforged.world.worldgen.terrain.TerrainType;
import com.pandaismyname1.ultraterraforged.world.worldgen.biome.Continentalness;
class MarineTerrainTest {
 @BeforeAll static void boot(){TestBootstrap.init();}
 @Test void coastAndMarineClimateFollowFinalGroundRatherThanAnOldIslandTag(){
  var preset=BuiltinPresetRenderTest.presets().get("default").get();
  var ctx=GeneratorContext.makeUncached(preset,1234,3,1,6);
  var h=ctx.localHeightmap.get();var l=ctx.levels;
  Cell wet=new Cell();wet.height=l.water(-8);wet.terrain=TerrainType.COASTAL_ISLAND;wet.continentNoise=.8f;
  h.applyPost(wet,0,0);
  assertTrue(wet.continentalness<Continentalness.COAST.min(),"a submerged island needs marine climate");
  Cell dry=new Cell();dry.height=l.water;dry.terrain=TerrainType.DEEP_OCEAN;dry.continentNoise=.01f;
  h.applyPost(dry,0,0);
  assertTrue(dry.continentalness>=Continentalness.COAST.min(),"dry shoreline cannot remain cold ocean");
 }
 @Test void broadCoastalBedHasReliefAndStaysBelowTheWaterSurface(){
  var levels=new com.pandaismyname1.ultraterraforged.world.worldgen.heightmap.Levels(256,63);
  var coast=com.pandaismyname1.ultraterraforged.world.worldgen.terrain.populator.Populators.makeCoast(1234,levels);
  float low=Float.MAX_VALUE,high=-Float.MAX_VALUE;
  Cell cell=new Cell();
  for(int z=-512;z<=512;z+=8)for(int x=-512;x<=512;x+=8){
   coast.apply(cell,x,z);
   float y=cell.height*levels.worldHeight;
   assertTrue(y<levels.waterY,"coastal bed emerged as a dry ocean plateau");
   low=Math.min(low,y);high=Math.max(high,y);
  }
  assertTrue(high-low>1.5F,"coastal bed is still flat");
 }

}
