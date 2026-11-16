package nl.townsandkingdoms.wildshape.mixin;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

class SurfaceStrataConcurrencyTest {
    @Test void rockLayerRandomnessIsIndependentOfFirstUseAndWorkerOrder() throws Exception {
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i < 128; i++) ids.add(ResourceLocation.fromNamespaceAndPath("test", "strata_" + i));
        Map<ResourceLocation, Long> forward = sample(ids, false);
        Collections.reverse(ids);
        assertEquals(forward, sample(ids, false));
        assertEquals(forward, sample(ids, true));
    }

    private static Map<ResourceLocation, Long> sample(List<ResourceLocation> ids, boolean parallel) throws Exception {
        var surface = new MixinSurfaceSystem();
        Field random = MixinSurfaceSystem.class.getDeclaredField("strataRandom");
        random.setAccessible(true);
        random.set(surface, new XoroshiroRandomSource(445566).forkPositional());
        Field cache = MixinSurfaceSystem.class.getDeclaredField("strata");
        cache.setAccessible(true);
        cache.set(surface, new ConcurrentHashMap<>());
        Map<ResourceLocation, Long> draws = new ConcurrentHashMap<>();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (ResourceLocation id : ids) {
                Runnable task = () -> surface.wildshape$WildshapeSurfaceSystem$getOrCreateStrata(id, source -> {
                    assertNull(draws.putIfAbsent(id, source.nextLong()));
                    return List.of();
                });
                if (parallel) futures.add(executor.submit(task)); else task.run();
            }
            for (var future : futures) future.get();
            for (ResourceLocation id : ids) surface.wildshape$WildshapeSurfaceSystem$getOrCreateStrata(id,
                    source -> { throw new AssertionError("cached strata built twice"); });
        }
        return draws;
    }
}
