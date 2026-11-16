package nl.townsandkingdoms.wildshape.concurrent.pool;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ThreadLocalPoolTest {
    @Test void oneSlotIsActuallyBorrowedAndClosingTwiceDoesNotAliasActiveCells() {
        AtomicInteger creations = new AtomicInteger(), cleans = new AtomicInteger();
        ThreadLocalPool<Object> pool = new ThreadLocalPool<>(1, () -> {
            creations.incrementAndGet(); return new Object();
        }, ignored -> cleans.incrementAndGet());
        var first = pool.get();
        assertEquals(1, creations.get(), "the preallocated slot was skipped");
        assertTrue(first.isOpen());
        first.close(); first.close();
        assertFalse(first.isOpen());
        assertEquals(1, cleans.get());
        var reused = pool.get();
        var nested = pool.get();
        assertNotSame(reused.get(), nested.get(), "duplicate returns alias active mutable cells");
        reused.close(); nested.close();
    }

    @Test void crossThreadReturnCannotMutateTheOwnersPool() throws Exception {
        AtomicInteger cleans = new AtomicInteger();
        ThreadLocalPool<Object> pool = new ThreadLocalPool<>(1, Object::new, ignored -> cleans.incrementAndGet());
        var borrowed = pool.get();
        Object first = borrowed.get();
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(borrowed::close).get();
        }
        assertFalse(borrowed.isOpen());
        var replacement = pool.get();
        assertNotSame(first, replacement.get());
        replacement.close();
        assertEquals(2, cleans.get());
    }
}
