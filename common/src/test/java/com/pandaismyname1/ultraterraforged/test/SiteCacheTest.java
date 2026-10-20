package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.pandaismyname1.ultraterraforged.world.worldgen.landform.SiteCache;

public class SiteCacheTest {
    @Test void retainsFrequentlyReadSitesAfterCapacity() {
        SiteCache<String> cache = SiteCache.make();
        AtomicInteger computations = new AtomicInteger();
        for (int i=0; i<5000; i++) {
            cache.get(i, 0, (x,z) -> {computations.incrementAndGet(); return "s"+x;});
        }
        assertEquals("s4999", cache.get(4999,0,(x,z) -> {computations.incrementAndGet(); return "unexpected";}));
        assertEquals(5000, computations.get());
    }
}
