package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves;

/** Pure range rules, kept separate from Minecraft for focused tests. */
public final class PlacementRules {
    private PlacementRules() {}

    public static boolean matches(float value, float[] range) {
        return range == null || range.length < 2 || (value >= range[0] && value <= range[1]);
    }

    public static boolean isRange(float[] range, float low, float high) {
        return range != null && range.length == 2
                && Math.abs(range[0] - low) < 0.00001f
                && Math.abs(range[1] - high) < 0.00001f;
    }

    public static boolean matchesDepth(float value, float[] range, float compatibleMaximum) {
        if (isRange(range, 0.05f, 2.0f)) {
            return value >= range[0] && value <= compatibleMaximum;
        }
        return matches(value, range);
    }

    public static boolean matchesContinentalness(String biome, float value, float[] range,
                                               boolean ocean, boolean adapt) {
        if (adapt) {
            boolean originalInland = (biome.equals("primordial_caves") && isRange(range, 0.4f, 1.0f))
                    || ((biome.equals("toxic_caves") || biome.equals("candy_cavity"))
                    && isRange(range, 0.5f, 1.0f));
            if (originalInland) return !ocean;
            if (biome.equals("abyssal_chasm") && isRange(range, -0.95f, -0.65f)) return ocean;
        }
        return matches(value, range);
    }

    public static boolean outsideSpawnExclusion(long x, long z, int distance) {
        return (double) x * x + (double) z * z >= (double) distance * distance;
    }
}
