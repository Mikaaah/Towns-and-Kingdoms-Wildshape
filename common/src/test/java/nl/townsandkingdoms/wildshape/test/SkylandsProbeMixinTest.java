package nl.townsandkingdoms.wildshape.test;

import nl.townsandkingdoms.wildshape.mixin.plugin.MixinPlugin;
import nl.townsandkingdoms.wildshape.platform.ModLoaderUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SkylandsProbeMixinTest {
    @Test void nativeSkyProbesAreOptionalAndRegisteredTogether() {
        var plugin = new MixinPlugin();
        assertEquals(2, MixinPlugin.SKYLANDS_MIXINS.size());
        for (boolean installed : new boolean[] {false, true}) {
            try (var platform = mockStatic(ModLoaderUtil.class)) {
                platform.when(() -> ModLoaderUtil.isLoaded("skylands")).thenReturn(installed);
                for (String mixin : MixinPlugin.SKYLANDS_MIXINS) {
                    assertEquals(installed, plugin.shouldApplyMixin("unused", mixin));
                    assertTrue(plugin.getMixins().contains(mixin.substring(
                            "nl.townsandkingdoms.wildshape.mixin.".length())));
                }
            }
        }
    }
}
