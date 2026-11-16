package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class StructurePlacementProfilesTest {
    @Test void bundledPairsKeepNativeCompanionOrigins() {
        var settings = StructurePlacementProfiles.settings();
        assertFalse(settings.approximateBiomeScreening(), "Native biome validators must remain authoritative by default");
        assertEquals(settings.set(ResourceLocation.parse("epic:large_dungeons")).linkedGroup(), settings.set(ResourceLocation.parse("epic:large_dungeons_obelisks")).linkedGroup());
        assertFalse(settings.set(ResourceLocation.parse("epic:large_dungeons")).linkedGroup().isEmpty());
        String dungeonKey = StructurePlacementCoordinator.rankingKey(ResourceLocation.parse("epic:large_dungeons"), settings.set(ResourceLocation.parse("epic:large_dungeons")));
        String obeliskKey = StructurePlacementCoordinator.rankingKey(ResourceLocation.parse("epic:large_dungeons_obelisks"), settings.set(ResourceLocation.parse("epic:large_dungeons_obelisks")));
        assertEquals(dungeonKey, obeliskKey, "Companions must also win/lose together against external sets");
        assertEquals(RegionalClearance.priority(123,dungeonKey,10,20), RegionalClearance.priority(123,obeliskKey,10,20));
        assertEquals("underground", settings.set(ResourceLocation.parse("mowziesmobs:wrought_chambers")).layer());
        assertEquals("ocean", settings.set(ResourceLocation.parse("cataclysm:sunken_city")).layer());
        assertNull(settings.set(ResourceLocation.parse("ec_ad:coralosaur_nest")));
    }
    @Test void settingsAreImmutableAndBounded() {
        assertThrows(UnsupportedOperationException.class, () -> StructurePlacementProfiles.settings().sets().clear());
        assertThrows(IllegalArgumentException.class, () -> StructurePlacementProfiles.parse(JsonParser.parseString("{\"namespaces\":{\"example\":{\"clearance\":99999}}}").getAsJsonObject(),256,112,8192));
        assertThrows(IllegalArgumentException.class, () -> StructurePlacementProfiles.parse(JsonParser.parseString("{\"sets\":{\"example:bad\":{\"layer\":\"typo\"}}}").getAsJsonObject(),256,112,8192));
    }
}
