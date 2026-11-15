package nl.townsandkingdoms.wildshape.concurrent.cache;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import nl.townsandkingdoms.wildshape.concurrent.cache.map.StampedLongMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CacheTrimTest {
    private static final class Entry implements ExpiringEntry {
        volatile long timestamp;
        final AtomicInteger reads = new AtomicInteger();
        boolean closed;
        Entry(long timestamp) { this.timestamp = timestamp; }
        public long getTimestamp() { reads.incrementAndGet(); return timestamp; }
        public void close() { closed = true; }
    }

    private static Cache<Entry> cache(StampedLongMap<Entry> map) {
        return new Cache<>(16, Long.MAX_VALUE, 20, TimeUnit.MINUTES, ignored -> map);
    }

    @Test void eachEligibleTimestampIsReadOnceAndEvictionDoesNotClosePublishedData() {
        var map = new StampedLongMap<Entry>(16);
        var entries = new ArrayList<Entry>();
        try (var cache = cache(map)) {
            for (int i = 0; i < 256; i++) {
                var entry = new Entry(i);
                entries.add(entry);
                map.put(i, entry);
            }
            assertEquals(192, cache.trimWithoutClosing(64, entry -> entry != entries.getFirst()));
            assertSame(entries.getFirst(), cache.get(0), "in-use entry is ineligible");
            for (int i = 0; i < entries.size(); i++) {
                assertEquals(i == 0 ? 0 : 1, entries.get(i).reads.get(), "timestamp reads for " + i);
                assertFalse(entries.get(i).closed, "published contents must remain usable");
                assertEquals(i == 0 || i >= 193, cache.get(i) != null, "LRU entry " + i);
            }
        }
    }

    @Test void evictionDoesNotRemoveAReplacementInstalledAfterTheSnapshot() {
        var replacement = new Entry(200);
        var map = new StampedLongMap<Entry>(16) {
            int scans;
            @Override public int removeIf(Predicate<Entry> predicate) {
                if (++scans == 2) put(1, replacement);
                return super.removeIf(predicate);
            }
        };
        try (var cache = cache(map)) {
            map.put(1, new Entry(1));
            map.put(2, new Entry(2));
            assertEquals(0, cache.trimWithoutClosing(1, entry -> true));
            assertSame(replacement, cache.get(1));
            assertFalse(replacement.closed);
        }
    }

    @Test void concurrentReadersCannotChangeTheOrderingDuringSort() throws Exception {
        var map = new StampedLongMap<Entry>(4096);
        var entries = new ArrayList<Entry>();
        for (int i = 0; i < 4096; i++) entries.add(new Entry(i));
        var running = new java.util.concurrent.atomic.AtomicBoolean(true);
        Thread reader = Thread.ofPlatform().daemon().start(() -> {
            long clock = 0;
            while (running.get()) {
                for (Entry entry : entries) entry.timestamp = clock++;
            }
        });
        try (var cache = cache(map)) {
            for (int round = 0; round < 24; round++) {
                for (int i = 0; i < entries.size(); i++) map.put(i, entries.get(i));
                assertDoesNotThrow(() -> cache.trimWithoutClosing(64, entry -> true));
                assertEquals(64, cache.size());
            }
        } finally {
            running.set(false);
            reader.join(5000);
            assertFalse(reader.isAlive());
        }
    }
}
