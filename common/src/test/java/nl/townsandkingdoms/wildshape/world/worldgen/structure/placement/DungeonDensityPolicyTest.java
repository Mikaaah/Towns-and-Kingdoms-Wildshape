package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DungeonDensityPolicyTest {
    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
    private static JsonObject nativeSet(int spacing, int separation) {
        return JsonParser.parseString("""
                {"structures":[{"structure":"example:dungeon","weight":7}],
                "placement":{"type":"minecraft:random_spread","spacing":%d,"separation":%d,
                "salt":3456745,"frequency":0.75,"spread_type":"triangular",
                "exclusion_zone":{"other_set":"example:other","chunk_count":8}}}
                """.formatted(spacing,separation)).getAsJsonObject();
    }
    @Test void areaMultiplierUsesSquareRootInsteadOfMakingStructuresRarer() {
        var grid = DungeonDensityPolicy.grid(40,30,1.25);
        assertEquals(36,grid.spacing()); assertEquals(27,grid.separation());
        assertEquals(1.25,1.0/Math.pow(1.0/Math.sqrt(1.25),2),1.0e-12);
        double actual = Math.pow(40.0/grid.spacing(),2);
        assertTrue(actual>1.20 && actual<1.30,"Integer chunk spacing approximates the requested candidate density");
    }
    @Test void gridBoundsRemainValidForEveryNativeSpacingAndBoundarySeparation() {
        for (int spacing=1;spacing<=4096;spacing++) for (int separation : new int[]{0,spacing/2,spacing-1}) {
            for (double multiplier : new double[]{0.25,1.0,1.25,4.0}) {
                var grid = DungeonDensityPolicy.grid(spacing,separation,multiplier);
                assertTrue(grid.spacing()>=1 && grid.spacing()<=4096);
                assertTrue(grid.separation()>=0 && grid.separation()<grid.spacing());
                if (multiplier==1.0) { assertEquals(spacing,grid.spacing()); assertEquals(separation,grid.separation()); }
            }
        }
    }
    @Test void onlySelectedAdventureSetsChangeAndUserPresetEntriesWin() {
        var policy = DungeonDensityPolicy.settings();
        assertTrue(policy.selected(id("epic:medium_dungeons"),Set.of()));
        assertFalse(policy.selected(id("epic:medium_dungeons"),Set.of(id("epic:medium_dungeons"))));
        for (String untouched : new String[]{"minecraft:villages","ae2:meteorite","alexscaves:acid_pit","idas:idas_nether",
                "idas:idas_small_nether","integrated_villages:regular_villages","epic:witch_hut","apotheosis:towers",
                "skylands:arid_archipelago","cataclysm:burning_arena","cataclysm:ruined_citadel","irons_spellbooks:citadel"}) {
            assertFalse(policy.selected(id(untouched),Set.of()),untouched);
        }
    }
    @Test void linkedDungeonAndObeliskGridsKeepTheirSharedOriginAndExplicitOptouts() {
        var a = nativeSet(61,60); var b = a.deepCopy();
        assertTrue(DungeonDensityPolicy.patch(id("epic:large_dungeons"),a,Set.of()));
        assertTrue(DungeonDensityPolicy.patch(id("epic:large_dungeons_obelisks"),b,Set.of()));
        assertEquals(a.getAsJsonObject("placement"),b.getAsJsonObject("placement"));
        assertEquals(55,a.getAsJsonObject("placement").get("spacing").getAsInt());
        assertEquals(54,a.getAsJsonObject("placement").get("separation").getAsInt());
        assertFalse(DungeonDensityPolicy.patch(id("epic:large_dungeons_obelisks"),nativeSet(61,60),Set.of(id("epic:large_dungeons"))));
    }
    @Test void sourceReopeningNeverCompoundsAndNativeFieldsSurvive() {
        var source = nativeSet(45,31); var adjusted = source.deepCopy();
        assertTrue(DungeonDensityPolicy.patch(id("epic:medium_dungeons"),adjusted,Set.of()));
        var once = adjusted.deepCopy();
        assertFalse(DungeonDensityPolicy.patch(id("epic:medium_dungeons"),adjusted,Set.of()));
        assertEquals(once,adjusted);
        var nextStartup = source.deepCopy();
        assertTrue(DungeonDensityPolicy.patch(id("epic:medium_dungeons"),nextStartup,Set.of()));
        assertEquals(once,nextStartup);
        assertEquals(source.get("structures"),adjusted.get("structures"));
        for (String field : new String[]{"salt","frequency","spread_type","exclusion_zone"})
            assertEquals(source.getAsJsonObject("placement").get(field),adjusted.getAsJsonObject("placement").get(field));
        var unrecognized = nativeSet(45,31); unrecognized.getAsJsonObject("placement").addProperty("type","example:custom_placement");
        assertFalse(DungeonDensityPolicy.patch(id("epic:medium_dungeons"),unrecognized,Set.of()));
    }
    @Test void exactJsonProfilesOverrideServerDefaultAndRejectDivergentLinkedPairs() {
        var json = JsonParser.parseString("""
                {"placementTypes":["minecraft:random_spread"],"sets":{
                "epic:medium_dungeons":{"candidateOpportunityMultiplier":1.0},"epic:small_dungeons":{}}}
                """).getAsJsonObject();
        var settings = DungeonDensityPolicy.parse(json,1.25);
        assertFalse(settings.selected(id("epic:medium_dungeons"),Set.of()));
        assertEquals(1.25,settings.multiplier(id("epic:small_dungeons")));
        var invalid = JsonParser.parseString("""
                {"placementTypes":["minecraft:random_spread"],"sets":{
                "epic:large_dungeons":{"candidateOpportunityMultiplier":1.0},"epic:large_dungeons_obelisks":{}},
                "linkedGroups":{"pair":["epic:large_dungeons","epic:large_dungeons_obelisks"]}}
                """).getAsJsonObject();
        assertThrows(IllegalArgumentException.class,()->DungeonDensityPolicy.parse(invalid,1.25));
        assertThrows(IllegalArgumentException.class,()->DungeonDensityPolicy.grid(40,30,Double.NaN));
    }
}
