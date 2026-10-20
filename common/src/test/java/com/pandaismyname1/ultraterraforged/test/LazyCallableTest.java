package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.pandaismyname1.ultraterraforged.concurrent.task.LazyCallable;

public class LazyCallableTest {
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
