package nl.townsandkingdoms.wildshape.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import nl.townsandkingdoms.wildshape.test.BuiltinPresetRenderTest;
import nl.townsandkingdoms.wildshape.test.PresetRenderer;
import nl.townsandkingdoms.wildshape.test.TestBootstrap;
import nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext;

public class MassiveCavernTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }
    @Test void hugeButBelowTerrainAndY30() {
        var settings = BuiltinPresetRenderTest.presets().get("default").get();
        CaveWorld world = new CaveWorld(GeneratorContext.makeCached(settings, PresetRenderer.SEED, 3, 6, false));
        GiantCaverns generator = new GiantCaverns(4321, 1.0F);
        CaveCarving probe = world.probe();
        int count = 0;
        for (int gx = -3; gx <= 3; gx++) {
            for (int gz = -3; gz <= 3; gz++) {
                GiantCaverns.Cavern cavern = generator.find(gx, gz, probe);
                if (cavern.radiusX() <= 0) continue;
                assertTrue(cavern.radiusX() >= 115.0F);
                assertTrue(cavern.radiusZ() >= 105.0F);
                assertTrue(cavern.roof() <= 30);
                assertTrue(cavern.roof() <= cavern.terrainHeight() - 30);
                assertTrue(cavern.y() - cavern.radiusY() > probe.bottom + 4);
                count++;
            }
        }
        assertTrue(count > 0, "no viable caverns in surveyed grid");
    }
}
