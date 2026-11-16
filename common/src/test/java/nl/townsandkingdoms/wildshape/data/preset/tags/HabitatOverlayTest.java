package nl.townsandkingdoms.wildshape.data.preset.tags;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HabitatOverlayTest {
    @Test void currentHabitatBridgesAreAvailableWithoutRegeneratingSavedPreset() throws Exception {
        var current = new LinkedHashMap<String, List<String>>();
        CurrentPackHabitats.addTo(current);
        for (var entry : current.entrySet())
            assertEquals(Set.copyOf(entry.getValue()), overlayValues(entry.getKey()), entry.getKey());
    }

    @Test void bloomingCropAndLinkedDungeonHabitatsAlsoHaveAdditiveSavedWorldResources() throws Exception {
        for (var entry : PresetBiomeCompat.BRIDGES.entrySet()) {
            var blooming = entry.getValue().stream().filter(value -> value.startsWith("bloomingnature:")).toList();
            if (!blooming.isEmpty())
                assertTrue(overlayValues(entry.getKey()).containsAll(blooming), entry.getKey());
        }
        assertEquals(overlayValues("epic:large_dungeon"), overlayValues("epic:plains_obelisk"));
        assertEquals(overlayValues("epic:large_sand_dungeon"), overlayValues("epic:sand_obelisk"));
    }

    private static Set<String> overlayValues(String id) throws Exception {
        String[] split = id.split(":", 2);
        String path = "/data/" + split[0] + "/tags/worldgen/biome/" + split[1] + ".json";
        try (var input = HabitatOverlayTest.class.getResourceAsStream(path)) {
            assertNotNull(input, "Missing saved-world overlay " + path);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            assertFalse(json.get("replace").getAsBoolean(), "Native tags must be extended: " + id);
            var result = new LinkedHashSet<String>();
            for (var value : json.getAsJsonArray("values")) {
                assertTrue(value.isJsonObject(), "Optional mod references must never be mandatory: " + id);
                assertFalse(value.getAsJsonObject().get("required").getAsBoolean(), id);
                assertTrue(result.add(value.getAsJsonObject().get("id").getAsString()), "Duplicate reference in " + id);
            }
            return result;
        }
    }
}
