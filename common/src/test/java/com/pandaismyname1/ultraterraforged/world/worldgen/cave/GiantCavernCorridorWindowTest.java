package com.pandaismyname1.ultraterraforged.world.worldgen.cave;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Exact projected culling may skip only corridor steps that cannot touch a chunk. */
public class GiantCavernCorridorWindowTest {
    @Test void windowRetainsAllReachableCorridorSteps() {
        Random random = new Random(20261020L);
        for (int trial = 0; trial < 900; trial++) {
            float x0 = random.nextInt(1800) - 900, z0 = random.nextInt(1800) - 900;
            float angle = random.nextFloat() * (float)(Math.PI * 2);
            float length = 120.0F + random.nextFloat() * 750.0F;
            float dx = (float)Math.cos(angle) * length;
            float dz = (float)Math.sin(angle) * length;
            int steps = Math.max(1, (int)Math.ceil(length / 12.0));
            float swayOffset = (random.nextFloat() - 0.5F) * 38.0F;
            int minX = (Math.round(x0 + random.nextFloat() * dx + random.nextInt(161) - 80) >> 4) << 4;
            int minZ = (Math.round(z0 + random.nextFloat() * dz + random.nextInt(161) - 80) >> 4) << 4;
            float radius = 17.0F;
            long range = GiantCaverns.corridorStepWindow(x0, z0, dx, dz,
                    minX, minZ, steps, radius + 3.0F);
            int first = (int)(range >>> 32), last = (int)range;
            float sideX = -dz / length, sideZ = dx / length;
            for (int i = 0; i <= steps; i++) {
                float t = i / (float)steps;
                float sway = (float)Math.sin(t * Math.PI) * swayOffset;
                float x = x0 + dx * t + sideX * sway;
                float z = z0 + dz * t + sideZ * sway;
                boolean reaches = x + radius + 3 >= minX && x - radius - 3 < minX + 16
                        && z + radius + 3 >= minZ && z - radius - 3 < minZ + 16;
                if (reaches) assertTrue(i >= first && i <= last,
                        "Missing corridor step " + i + " for trial " + trial);
            }
        }
    }
}
