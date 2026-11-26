package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StructurePlacementProfilesTest {
    @Test void bundledProfilesOnlyValidateNamedStructuresAndKeepCatacombProtection() {
        var settings = StructurePlacementProfiles.settings();
        assertTrue(settings.structure(ResourceLocation.parse("mowziesmobs:monastery")).surface());
        assertTrue(settings.structure(ResourceLocation.parse("irons_spellbooks:catacombs")).rejectCavernVoid());
        assertNull(settings.structure(ResourceLocation.parse("idas:unlisted_dungeon")));
        assertNull(settings.structure(ResourceLocation.parse("cataclysm:sunken_city")));
        assertThrows(UnsupportedOperationException.class, () -> settings.structures().clear());
    }

    @Test void terrainBoundsAndLayerNamesStillRejectInvalidAuthoredProfiles() {
        assertThrows(IllegalArgumentException.class, () -> StructurePlacementProfiles.parse(JsonParser.parseString(
                "{\"structures\":{\"example:bad\":{\"maxSlopeDelta\":99999}}}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> StructurePlacementProfiles.parse(JsonParser.parseString(
                "{\"structures\":{\"example:bad\":{\"maxAnchorOffset\":-1}}}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> StructurePlacementProfiles.parse(JsonParser.parseString(
                "{\"structures\":{\"example:bad\":{\"layer\":\"typo\"}}}").getAsJsonObject()));
    }

    @Test void oldSpacingProfilesAreIgnoredWhileTheirTerrainChoicesSurvive() {
        var old = StructurePlacementProfiles.parse(JsonParser.parseString(
                "{\"approximateBiomeScreening\":true,\"namespaces\":{\"irons_spellbooks\":{\"clearance\":99999}},"
                + "\"sets\":{\"idas:idas_common\":{\"layer\":\"retired\",\"clearance\":99999}},"
                + "\"structures\":{\"example:den\":{\"layer\":\"surface\",\"size\":\"retired\",\"clearance\":99999,"
                + "\"linkedGroup\":\"old\",\"maxSlopeDelta\":18,\"maxAnchorOffset\":6,\"rejectWater\":true}}}")
                .getAsJsonObject());
        var den = old.structure(ResourceLocation.parse("example:den"));
        assertEquals(18, den.maxSlopeDelta()); assertEquals(6, den.maxAnchorOffset()); assertTrue(den.rejectWater());
        assertTrue(old.structure(ResourceLocation.parse("irons_spellbooks:catacombs")).rejectCavernVoid());
        assertNull(old.structure(ResourceLocation.parse("idas:idas_common")));
    }

    @Test void explicitCatacombOptOutAndLegacyFallbackNamespaceDisableArePreserved() {
        var explicit = StructurePlacementProfiles.parse(JsonParser.parseString(
                "{\"structures\":{\"irons_spellbooks:catacombs\":{\"layer\":\"underground\",\"rejectCavernVoid\":false}}}")
                .getAsJsonObject());
        assertFalse(explicit.structure(ResourceLocation.parse("irons_spellbooks:catacombs")).rejectCavernVoid());
        var disabled = StructurePlacementProfiles.parse(JsonParser.parseString(
                "{\"namespaces\":{\"irons_spellbooks\":{\"layer\":\"disabled\"}}}").getAsJsonObject());
        assertNull(disabled.structure(ResourceLocation.parse("irons_spellbooks:catacombs")));
    }

    @Test void legacyEnabledSwitchIsRetainedWithoutOverwritingExistingJson(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("structure-placement.json");
        String original = "{\"sets\":{\"idas:idas_common\":{\"clearance\":256}},\"structures\":{}}";
        Files.writeString(file, original);
        try {
            StructurePlacementProfiles.configure(false, file);
            assertFalse(StructurePlacementProfiles.settings().enabled());
            assertEquals(original, Files.readString(file));
            assertTrue(StructurePlacementProfiles.settings().structure(ResourceLocation.parse("irons_spellbooks:catacombs")).rejectCavernVoid());
        } finally {
            StructurePlacementProfiles.configure(true, null);
        }
    }

    @Test void newCopiedDefaultsContainOnlyTerrainProfiles(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("structure-placement.json");
        try {
            StructurePlacementProfiles.configure(true, file);
            var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            assertTrue(json.has("structures"));
            assertFalse(json.has("sets")); assertFalse(json.has("namespaces"));
            for (var entry : json.getAsJsonObject("structures").entrySet()) {
                assertFalse(entry.getValue().getAsJsonObject().has("clearance"));
                assertFalse(entry.getValue().getAsJsonObject().has("linkedGroup"));
            }
        } finally {
            StructurePlacementProfiles.configure(true, null);
        }
    }
}
