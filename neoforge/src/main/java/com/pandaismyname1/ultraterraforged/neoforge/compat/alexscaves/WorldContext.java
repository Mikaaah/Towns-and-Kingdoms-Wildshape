package com.pandaismyname1.ultraterraforged.neoforge.compat.alexscaves;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.cell.Cell;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WorldContext {
    public final long seed;
    public final ResourceKey<Level> dimension;
    public final GeneratorContext terrain;
    public final Map<ResourceKey<Biome>, Holder<Biome>> biomes;
    private final ThreadLocal<Cell> cell = ThreadLocal.withInitial(Cell::new);
    private final ThreadLocal<Map<Long, Boolean>> oceans = ThreadLocal.withInitial(() ->
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                    return size() > 4096;
                }
            });

    public WorldContext(long seed, ResourceKey<Level> dimension, GeneratorContext terrain,
                        Map<ResourceKey<Biome>, Holder<Biome>> biomes) {
        this.seed = seed;
        this.dimension = dimension;
        this.terrain = terrain;
        this.biomes = Map.copyOf(biomes);
    }

    public boolean isOcean(int x, int z) {
        long key = ((long) x << 32) ^ (z & 0xffffffffL);
        Map<Long, Boolean> cache = oceans.get();
        Boolean known = cache.get(key);
        if (known != null) return known;
        Cell sample = cell.get().reset();
        // A direct deterministic heightmap query avoids depending on whether a distant tile has been generated.
        terrain.lookup.compute(sample, x, z);
        boolean ocean = sample.terrain != null
                ? sample.terrain.isDeepOcean() || sample.terrain.isShallowOcean()
                : terrain.levels.scale(sample.height) < terrain.levels.waterY;
        cache.put(key, ocean);
        return ocean;
    }
}
