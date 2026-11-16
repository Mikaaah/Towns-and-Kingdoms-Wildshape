package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

/**
 * Stateless ranking for structure candidates. Coordinates are chunk coordinates;
 * clearances are blocks. A candidate survives only when no conflicting candidate
 * has a better rank, so requesting chunks in a different order cannot change it.
 */
public final class RegionalClearance {
    private RegionalClearance() {}

    /** Stable across JVMs; does not consume a generator's mutable random source. */
    public static long priority(long seed, String setId, int chunkX, int chunkZ) {
        long idHash = 0xcbf29ce484222325L;
        for (int i = 0; i < setId.length(); i++) {
            idHash = (idHash ^ setId.charAt(i)) * 0x100000001b3L;
        }
        long coordinates = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
        return mix(seed ^ mix(idHash) ^ mix(coordinates));
    }

    /**
     * Each pair uses the larger requested clearance. Axis rejection happens
     * before squaring, keeping even extreme signed chunk coordinates safe.
     * Candidates exactly on the clearance boundary are allowed.
     */
    public static boolean conflicts(int xA, int zA, int clearanceA,
            int xB, int zB, int clearanceB) {
        int minimum = Math.max(clearanceA, clearanceB);
        if (minimum <= 0) return false;
        long dx = ((long) xA - xB) * 16L;
        long dz = ((long) zA - zB) * 16L;
        if (dx <= -(long) minimum || dx >= minimum
                || dz <= -(long) minimum || dz >= minimum) return false;
        return dx * dx + dz * dz < (long) minimum * minimum;
    }

    /** Strict total order, including deterministic tie-breaking for hash collisions. */
    public static boolean wins(long priorityA, String idA, int xA, int zA,
            long priorityB, String idB, int xB, int zB) {
        int order = Long.compareUnsigned(priorityA, priorityB);
        if (order == 0) order = idA.compareTo(idB);
        if (order == 0) order = Integer.compare(xA, xB);
        if (order == 0) order = Integer.compare(zA, zB);
        return order < 0;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
