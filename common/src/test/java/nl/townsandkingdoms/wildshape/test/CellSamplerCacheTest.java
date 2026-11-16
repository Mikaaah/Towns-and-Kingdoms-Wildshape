package nl.townsandkingdoms.wildshape.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import nl.townsandkingdoms.wildshape.world.worldgen.cell.Cell;
import nl.townsandkingdoms.wildshape.world.worldgen.densityfunction.CellSampler;
import nl.townsandkingdoms.wildshape.world.worldgen.heightmap.WorldLookup;

class CellSamplerCacheTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test void changingColumnsDoesNotAllocateAnotherWeakWorldIdentity() throws Exception {
        WorldLookup lookup = mock(WorldLookup.class);
        doAnswer(call -> { ((Cell) call.getArgument(0)).height = call.getArgument(1, Integer.class); return false; })
                .when(lookup).apply(any(Cell.class), anyInt(), anyInt());
        var cache = new CellSampler.Cache2d();
        cache.getAndUpdate(lookup, 0, 0);
        Field identity = CellSampler.Cache2d.class.getDeclaredField("lastLookup");
        identity.setAccessible(true);
        Object firstIdentity = identity.get(cache);
        for (int x = 4; x <= 256; x += 4) {
            assertEquals((float) x, cache.getAndUpdate(lookup, x, 0).height);
            assertSame(firstIdentity, identity.get(cache));
        }
    }

    @Test void failedColumnNeverPublishesPartiallyWrittenCellAsACacheHit() {
        WorldLookup lookup = mock(WorldLookup.class);
        AtomicInteger calls = new AtomicInteger();
        doAnswer(call -> {
            Cell cell = call.getArgument(0);
            cell.height = 123;
            if (calls.getAndIncrement() == 0) throw new IllegalStateException("first lookup failed");
            cell.height = 456;
            return false;
        }).when(lookup).apply(any(Cell.class), anyInt(), anyInt());
        var cache = new CellSampler.Cache2d();
        assertThrows(IllegalStateException.class, () -> cache.getAndUpdate(lookup, 4, 8));
        assertEquals(456, cache.getAndUpdate(lookup, 4, 8).height);
        assertEquals(456, cache.getAndUpdate(lookup, 4, 8).height);
        assertEquals(2, calls.get());
    }
}
