package com.pandaismyname1.ultraterraforged.data.preset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.pandaismyname1.ultraterraforged.test.BuiltinPresetRenderTest;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;

class StructureSetJsonPatcherTest {
    private static JsonObject structure() {
        return JsonParser.parseString("""
                {"structures":[{"structure":"idas:test","weight":3}],"placement":{
                "type":"integrated_api:advanced_random_spread","spacing":21,"separation":12,
                "salt":41,"super_exclusion_zone":{"other_set":"#idas:avoid","chunk_count":10}}}
                """).getAsJsonObject();
    }

    @Test void worldsWithoutPresetResourcesDoNotOpenOrChangeStructureData() throws Exception {
        TestBootstrap.init();
        var resources = org.mockito.Mockito.mock(net.minecraft.server.packs.resources.ResourceManager.class);
        org.mockito.Mockito.when(resources.getResource(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
        var original = org.mockito.Mockito.mock(net.minecraft.server.packs.resources.Resource.class);
        assertSame(original, PresetStructureResources.patch(resources, ResourceLocation.parse("idas:idas_common"), original));
        org.mockito.Mockito.verifyNoInteractions(original);
    }

    @Test void lateBindingKeepsNativeTypeWeightsAndExclusionZones() {
        TestBootstrap.init();
        var preset = BuiltinPresetRenderTest.presets().get("default").get();
        var json = structure(); var before = json.deepCopy();
        assertTrue(StructureSetJsonPatcher.patch(preset, ResourceLocation.parse("idas:idas_common"), json));
        assertEquals(36, json.getAsJsonObject("placement").get("spacing").getAsInt());
        before.getAsJsonObject("placement").addProperty("spacing", 36);
        assertEquals(before, json);
        assertFalse(StructureSetJsonPatcher.patch(preset, ResourceLocation.parse("idas:idas_common"), json), "patch must be idempotent");
    }

    @Test void explicitPresetOverridesWinAndKeepSeparationValid() {
        TestBootstrap.init();
        var preset = BuiltinPresetRenderTest.presets().get("default").get();
        var id = ResourceLocation.parse("idas:idas_common");
        var entry = preset.structures().getOrCreate(id); entry.spacing = 9; entry.separation = 20;
        var json = structure();
        assertTrue(StructureSetJsonPatcher.patch(preset, id, json));
        assertEquals(9, json.getAsJsonObject("placement").get("spacing").getAsInt());
        assertEquals(8, json.getAsJsonObject("placement").get("separation").getAsInt());
    }

    @Test void unselectedResourceAndOtherDimensionSetsStayNative() {
        TestBootstrap.init();
        var preset = BuiltinPresetRenderTest.presets().get("default").get();
        for (String id : new String[]{"ae2:meteorite", "alexscaves:acid_pit", "idas:idas_nether", "example:unknown"}) {
            var json = structure(); var before = json.deepCopy();
            assertFalse(StructureSetJsonPatcher.patch(preset, ResourceLocation.parse(id), json));
            assertEquals(before, json);
        }
    }
}
