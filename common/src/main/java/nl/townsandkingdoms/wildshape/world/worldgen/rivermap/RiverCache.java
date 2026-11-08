package nl.townsandkingdoms.wildshape.world.worldgen.rivermap;

import java.util.concurrent.TimeUnit;

import nl.townsandkingdoms.wildshape.concurrent.cache.Cache;
import nl.townsandkingdoms.wildshape.concurrent.cache.CacheManager;
import nl.townsandkingdoms.wildshape.concurrent.cache.map.StampedLongMap;
import nl.townsandkingdoms.wildshape.world.worldgen.util.PosUtil;

public class RiverCache {
    protected RiverGenerator generator;
    protected Cache<Rivermap> cache;

    public RiverCache(RiverGenerator generator) {
        this.cache = CacheManager.createCache(32, 5L, 1L, TimeUnit.MINUTES, StampedLongMap::new);
        this.generator = generator;
    }

    public Rivermap getRivers(int x, int z) {
        Rivermap rivermap = this.cache.computeIfAbsent(PosUtil.pack(x, z), id -> {
	return this.generator.generateRivers(PosUtil.unpackLeft(id), PosUtil.unpackRight(id), id);
        });
        rivermap.touch();
        return rivermap;
    }
}
