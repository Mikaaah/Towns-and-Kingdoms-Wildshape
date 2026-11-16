package nl.townsandkingdoms.wildshape.world.worldgen.tile.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import org.junit.jupiter.api.Test;
import nl.townsandkingdoms.wildshape.data.preset.settings.FilterSettings;

class ErosionBrushTest {
    @Test void relativeKernelsMatchLegacyAbsoluteIndicesAndFloatWeightsAtEveryColumn() throws Exception {
        for (int size : new int[] {4, 9, 16, 31, 64}) {
            Erosion erosion = new Erosion(42, size, new FilterSettings.Erosion(4, 20, 1, 1, .2F, .3F), v -> 1);
            int[][] offsets = offsets(erosion);
            float[][] actualWeights = weights(erosion);
            int radius = 4;
            // Independent direct geometric reference, including all clipped edge/corner cases.
            for (int centre = 0; centre < size * size; centre++) {
                int cx = centre % size, cz = centre / size;
                int[] expectedIndices = new int[64];
                float[] expectedWeights = new float[64];
                int count = 0;
                float sum = 0;
                for (int z = -radius; z <= radius; z++) for (int x = -radius; x <= radius; x++) {
                    float squareDistance = x * x + z * z;
                    if (squareDistance < radius * radius && cx + x >= 0 && cx + x < size
                            && cz + z >= 0 && cz + z < size) {
                        float weight = 1 - (float) Math.sqrt(squareDistance) / radius;
                        expectedIndices[count] = (cz + z) * size + cx + x;
                        expectedWeights[count++] = weight;
                        sum += weight;
                    }
                }
                assertEquals(count, offsets[centre].length);
                for (int j = 0; j < count; j++) {
                    assertEquals(expectedIndices[j], centre + offsets[centre][j]);
                    assertEquals(Float.floatToIntBits(expectedWeights[j] / sum),
                            Float.floatToIntBits(actualWeights[centre][j]), "kernel weight changed");
                }
            }
        }
    }

    @Test void retainedKernelArraysStayBoundedWhenTheTileAreaGrows() throws Exception {
        int size = 192;
        Erosion erosion = new Erosion(42, size, new FilterSettings.Erosion(4, 20, 1, 1, .2F, .3F), v -> 1);
        var distinctOffsets = Collections.newSetFromMap(new IdentityHashMap<int[], Boolean>());
        var distinctWeights = Collections.newSetFromMap(new IdentityHashMap<float[], Boolean>());
        Collections.addAll(distinctOffsets, offsets(erosion));
        Collections.addAll(distinctWeights, weights(erosion));
        assertTrue(distinctOffsets.size() <= 81, "one offset array per column was retained");
        assertTrue(distinctWeights.size() <= 81, "one weight array per column was retained");
        System.out.printf("EROSION_KERNELS size=%d legacyArrays=%d sharedArrays=%d%n", size,
                size * size * 2, distinctOffsets.size() + distinctWeights.size());
    }

    private static int[][] offsets(Erosion erosion) throws Exception {
        Field field = Erosion.class.getDeclaredField("erosionBrushOffsets");
        field.setAccessible(true);
        return (int[][]) field.get(erosion);
    }
    private static float[][] weights(Erosion erosion) throws Exception {
        Field field = Erosion.class.getDeclaredField("erosionBrushWeights");
        field.setAccessible(true);
        return (float[][]) field.get(erosion);
    }
}
