package nl.townsandkingdoms.wildshape.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import nl.townsandkingdoms.wildshape.concurrent.task.LazyCallable;

public class LazyCallableTest {
    @Test void concurrentReadersAlwaysSeeTheOncePublishedValue() throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            for (int round = 0; round < 256; round++) {
                AtomicInteger calls = new AtomicInteger();
                Object expected = new Object();
                var start = new java.util.concurrent.CountDownLatch(1);
                var job = LazyCallable.adapt(() -> {
                    calls.incrementAndGet();
                    Thread.yield();
                    return expected;
                });
                var readers = new java.util.ArrayList<java.util.concurrent.Future<Object>>();
                for (int reader = 0; reader < 8; reader++) readers.add(executor.submit(() -> {
                    start.await();
                    Object result = job.get();
                    for (int i = 0; i < 16; i++) assertSame(expected, job.get());
                    return result;
                }));
                start.countDown();
                for (var reader : readers) assertSame(expected, reader.get());
                assertEquals(1, calls.get());
                assertTrue(job.isDone());
            }
        }
    }

    @Test void nullValuedRunnableCompletesOnce() {
        AtomicInteger calls = new AtomicInteger();
        var job = LazyCallable.adapt((Runnable) calls::incrementAndGet);
        assertFalse(job.isDone());
        assertNull(job.get());
        assertTrue(job.isDone());
        assertNull(job.get());
        assertEquals(1, calls.get());
    }
}
