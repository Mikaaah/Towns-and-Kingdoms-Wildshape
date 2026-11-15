package nl.townsandkingdoms.wildshape.compat.skylands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.Test;

class SkylandsDimensionResourcesTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");

    private static JsonObject dimension(int minY, int height) {
        return JsonParser.parseString("""
                {"min_y":%d,"height":%d,"logical_height":%d,
                 "effects":"minecraft:overworld","coordinate_scale":1.0,
                 "has_skylight":true,"monster_spawn_light_level":{"type":"minecraft:uniform",
                 "min_inclusive":0,"max_inclusive":7}}
                """.formatted(minY, height, height)).getAsJsonObject();
    }

    @Test void existingNormalPresetGetsSkySpaceAndKeepsAllOtherFields() {
        var json = dimension(-64, 384);
        var expected = json.deepCopy();
        expected.addProperty("height", 592);
        expected.addProperty("logical_height", 592);
        assertTrue(SkylandsDimensionResources.expand(json, 528));
        assertEquals(expected, json);
        assertFalse(SkylandsDimensionResources.expand(json, 528));
    }

    @Test void deeperPresetsKeepTheirBottomAndTallerPresetsNeverShrink() {
        var deep = dimension(-128, 640);
        assertTrue(SkylandsDimensionResources.expand(deep, 528));
        assertEquals(-128, deep.get("min_y").getAsInt());
        assertEquals(656, deep.get("height").getAsInt());
        var tall = dimension(-128, 1152);
        var before = tall.deepCopy();
        assertFalse(SkylandsDimensionResources.expand(tall, 528));
        assertEquals(before, tall);
    }

    @Test void nonSectionAlignedRequestsRoundUpWithoutLosingBuildSpace() {
        var json = dimension(-64, 384);
        assertTrue(SkylandsDimensionResources.expand(json, 530));
        assertEquals(608, json.get("height").getAsInt());
    }

    @Test void optionalModAbsentDoesNotReadResources() throws Exception {
        var manager = mock(ResourceManager.class);
        var original = mock(Resource.class);
        assertSame(original, SkylandsDimensionResources.patch(manager, OVERWORLD, original, false));
        verifyNoInteractions(manager, original);
    }

    @Test void otherDimensionsAreNeverReadOrChanged() throws Exception {
        var manager = mock(ResourceManager.class);
        var original = mock(Resource.class);
        for (String id : List.of("minecraft:the_nether", "minecraft:the_end", "deeperdarker:otherside"))
            assertSame(original, SkylandsDimensionResources.patch(manager, ResourceLocation.parse(id), original, true));
        verifyNoInteractions(manager, original);
    }

    @Test void vanillaWorldWithoutWildshapePresetKeepsItsNativeDimension() throws Exception {
        var manager = mock(ResourceManager.class);
        when(manager.getResource(any())).thenReturn(Optional.empty());
        var original = mock(Resource.class);
        assertSame(original, SkylandsDimensionResources.patch(manager, OVERWORLD, original, true));
        verifyNoInteractions(original);
    }

    @Test void existingWorldResourceIsPatchedInMemoryAndKeepsItsPackAndOriginalBytes() throws Exception {
        var manager = mock(ResourceManager.class);
        when(manager.getResource(any())).thenReturn(Optional.of(mock(Resource.class)));
        var pack = mock(PackResources.class);
        var json = dimension(-64, 384);
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        var original = new Resource(pack, () -> new ByteArrayInputStream(bytes));
        var higherPack = new Resource(mock(PackResources.class),
                () -> new ByteArrayInputStream(dimension(-64, 704).toString().getBytes(StandardCharsets.UTF_8)));
        when(manager.getResourceStack(any())).thenReturn(List.of(higherPack, original));
        var patched = SkylandsDimensionResources.patch(manager, OVERWORLD, original, true);
        assertNotSame(original, patched);
        assertSame(pack, patched.source());
        try (var reader = patched.openAsReader()) {
            var result = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals(704, result.get("height").getAsInt(), "retain higher installed resource ceiling");
            assertEquals(-64, result.get("min_y").getAsInt());
        }
        try (var reader = original.openAsReader()) {
            assertEquals(json, JsonParser.parseReader(reader), "the saved preset is never rewritten");
        }
    }
}
