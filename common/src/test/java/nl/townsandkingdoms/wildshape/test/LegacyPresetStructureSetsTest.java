package nl.townsandkingdoms.wildshape.test;

import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import nl.townsandkingdoms.wildshape.data.PresetPacks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LegacyPresetStructureSetsTest {
    @Test
    void oldPresetResourcesRestoreTheHighestNonPresetPack() {
        var dungeon = ResourceLocation.parse("dungeons_arise:worldgen/structure_set/major_structures.json");
        var retired = ResourceLocation.parse("wildshape:worldgen/structure_set/retired.json");
        var nativeSet = resource("mod/dungeons_arise");
        var packOverride = resource("file/server_structures.zip");
        var oldPreset = resource("file/wildshape-preset.zip");
        var exportedPreset = resource("file/wildshape-preset-old.zip");
        var resources = mock(ResourceManager.class);
        when(resources.getResourceStack(dungeon)).thenReturn(List.of(nativeSet, oldPreset, packOverride, exportedPreset));
        when(resources.getResourceStack(retired)).thenReturn(List.of(oldPreset));

        var selected = Map.of(dungeon, exportedPreset, retired, oldPreset);
        assertEquals(Map.of(dungeon, packOverride), PresetPacks.withoutLegacyStructureSets(resources, selected));
    }

    @Test
    void ordinaryStructureSetsUseTheSelectedPack() {
        var file = ResourceLocation.parse("minecraft:worldgen/structure_set/villages.json");
        var selected = Map.of(file, resource("file/server_structures.zip"));
        var resources = mock(ResourceManager.class);

        assertSame(selected, PresetPacks.withoutLegacyStructureSets(resources, selected));
        verifyNoInteractions(resources);
    }

    private static Resource resource(String packId) {
        var resource = mock(Resource.class);
        when(resource.sourcePackId()).thenReturn(packId);
        return resource;
    }
}
