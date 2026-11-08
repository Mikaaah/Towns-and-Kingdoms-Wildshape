package nl.townsandkingdoms.wildshape.test;

import nl.townsandkingdoms.wildshape.data.PresetPacks;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresetPackIdentityTest {
    @Test void packsUseOnlyTheWildshapeIdentity() {
        assertEquals("wildshape-preset.zip", PresetPacks.WORLD_PACK_NAME);
        assertFalse(PresetPacks.isPresetPack("file/ultraterraforged-preset.zip"));
        assertFalse(PresetPacks.isPresetPack("file/tnkterraforged-preset.zip"));
        assertTrue(PresetPacks.isPresetPack("file/wildshape-preset.zip"));
        assertTrue(PresetPacks.isPresetPack("file/wildshape-preset-123abc.zip"));
        assertFalse(PresetPacks.isPresetPack("file/other-preset.zip"));
        assertFalse(PresetPacks.isPresetPack("file/wildshape-preset.txt"));
    }
}
