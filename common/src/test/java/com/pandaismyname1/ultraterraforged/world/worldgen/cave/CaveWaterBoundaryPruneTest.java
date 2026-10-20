package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.pandaismyname1.ultraterraforged.test.BuiltinPresetRenderTest;
import com.pandaismyname1.ultraterraforged.test.PresetRenderer;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import net.minecraft.world.level.block.Blocks;

/** An interior water source must be retained even when adjoining air masks are pruned. */
public class CaveWaterBoundaryPruneTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test void waterIntroducedAfterCarvingStillFloodsConnectedAir() {
        var settings = BuiltinPresetRenderTest.presets().get("default").get();
        CaveWorld world = new CaveWorld(GeneratorContext.makeCached(settings,
                PresetRenderer.SEED, 3, 6, false));
        CaveCarving carving = new CaveCarving(world.chunk(0, 0), world.context);
        carving.hollow(8.0F, -20.0F, 8.0F, 6.0F, 6.0F, 6.0F,
                CaveCarving.DRY, CaveCarving.OPEN);
        assertTrue(world.get(9, -20, 8).isAir(), "Initial cave must be dry");
        carving.set(8, -20, 8, Blocks.WATER.defaultBlockState());
        carving.reconcileWater();
        assertTrue(world.get(9, -20, 8).is(Blocks.WATER),
                "Later water source must reach neighboring carved air");
        assertTrue(world.get(10, -20, 8).is(Blocks.WATER),
                "Water must continue across the carved cavern at source elevation");
        assertTrue(world.get(9, -17, 8).isAir(),
                "Flood cannot rise above its actual source-water elevation");
    }
}
