package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.test.BuiltinPresetRenderTest;
import com.pandaismyname1.ultraterraforged.test.PresetRenderer;
import com.pandaismyname1.ultraterraforged.test.TestBootstrap;
import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.noise.module.Noises;
import net.minecraft.world.level.block.Blocks;

/**
 * A giant-cavern lake must not remove the base of a rock pillar.
 * Carve only the sampled columns so connecting corridors and other cave
 * features cannot confuse the source of the waterline regression.
 */
public class GiantCavernPillarTest {
    private static final int SEED = 4321;

    @BeforeAll
    static void bootstrap() {
        TestBootstrap.init();
    }

    @Test
    void cavernPillarsContinueBelowLakeSurface() {
        var settings = BuiltinPresetRenderTest.presets().get("default").get();
        CaveWorld world = new CaveWorld(
                GeneratorContext.makeCached(settings, PresetRenderer.SEED, 3, 6, false));
        GiantCaverns generator = new GiantCaverns(SEED, 1.0F);
        CaveCarving probe = world.probe();

        GiantCaverns.Cavern chamber = null;
        for (int gx = -2; gx <= 2 && chamber == null; gx++) {
            for (int gz = -2; gz <= 2; gz++) {
                var found = generator.find(gx, gz, probe);
                if (found.radiusX() > 0) {
                    chamber = found;
                    break;
                }
            }
        }
        assertNotNull(chamber, "Unable to locate a viable cavern");

        // Same x/z noise used to decide which columns become pillars.
        var pillarNoise = Noises.worleyEdge(SEED + 2, 27);
        int px = Integer.MIN_VALUE, pz = Integer.MIN_VALUE;
        int ox = Integer.MIN_VALUE, oz = Integer.MIN_VALUE;
        int centerX = Math.round(chamber.x()), centerZ = Math.round(chamber.z());

        for (int dz = -32; dz <= 32; dz++) {
            for (int dx = -32; dx <= 32; dx++) {
                int x = centerX + dx, z = centerZ + dz;
                float flat = (float) (Math.pow((x - chamber.x()) / chamber.radiusX(), 2)
                        + Math.pow((z - chamber.z()) / chamber.radiusZ(), 2));
                if (flat >= 0.12F) continue;
                float noise = pillarNoise.compute(x, z, 0);
                if (noise < 0.12F && px == Integer.MIN_VALUE) {
                    px = x;
                    pz = z;
                } else if (noise > 0.30F && ox == Integer.MIN_VALUE) {
                    ox = x;
                    oz = z;
                }
            }
        }
        assertNotEquals(Integer.MIN_VALUE, px, "Could not locate a test pillar");
        assertNotEquals(Integer.MIN_VALUE, ox, "Could not locate an open lake column");

        // Record-replay uses same cavern object; only one or two chunks generated.
        final GiantCaverns.Cavern cavern = chamber;
        world.carve(px, pz, px, pz, carving -> generator.hollow(cavern, carving));
        world.carve(ox, oz, ox, oz, carving -> generator.hollow(cavern, carving));

        int waterline = (int) (cavern.y() - cavern.radiusY() * 0.53F);
        assertTrue(world.get(px, waterline + 4, pz).is(Blocks.STONE),
                "Pillar should remain solid above the lake");
        assertTrue(world.get(px, waterline, pz).is(Blocks.STONE),
                "Pillar must not terminate at the waterline");
        assertTrue(world.get(px, waterline - 6, pz).is(Blocks.STONE),
                "Pillar foundation must continue below the lake");
        assertTrue(world.get(px, waterline - 12, pz).is(Blocks.STONE),
                "Pillar should be anchored near the lakebed");

        // Check that preserving the rock column didn't delete the lake itself.
        assertTrue(world.get(ox, waterline - 4, oz).is(Blocks.WATER),
                "Water should still fill open cavern space below the lake surface");
        assertTrue(world.get(ox, waterline + 4, oz).isAir(),
                "Open space above the lake should still be air");
    }
}
