package nl.townsandkingdoms.wildshape.concurrent.cache;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.function.LongFunction;

import nl.townsandkingdoms.wildshape.concurrent.ThreadPools;
import nl.townsandkingdoms.wildshape.concurrent.cache.map.LongMap;

public class Cache<V extends ExpiringEntry> implements AutoCloseable {
	public static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(ThreadPools.daemonFactory("Wildshape-CacheScheduler"));

    private LongMap<V> map;
    private long lifetimeMS;
    private volatile long timeout;
    private ScheduledFuture<?> poll;

    public Cache(int capacity, long expireTime, long pollInterval, TimeUnit unit, IntFunction<LongMap<V>> mapFunc) {
        this.timeout = 0L;
        this.map = mapFunc.apply(capacity);
        this.lifetimeMS = unit.toMillis(expireTime);

        long intervalMillis = unit.toMillis(pollInterval);
        WeakReference<Cache<V>> cacheRef = new WeakReference<>(this);
        AtomicReference<ScheduledFuture<?>> handle = new AtomicReference<>();
        if (expireTime == Long.MAX_VALUE) {
            // Non-expiring tile caches do not need a scheduler tick.
            this.poll = null;
        } else {
            this.poll = SCHEDULER.scheduleAtFixedRate(() -> {
                Cache<V> cached = cacheRef.get();
                if (cached != null) cached.poll();
                else {
                    ScheduledFuture<?> scheduled = handle.get();
                    if (scheduled != null) scheduled.cancel(false);
                }
            }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
            handle.set(this.poll);
        }
    }

    public void remove(long key) {
        this.map.remove(key, ExpiringEntry::close);
    }

    // takes out this very entry, not one that has since taken its place
    public void forget(V entry) {
        this.map.removeIf((other) -> other == entry);
    }

    public V get(long key) {
        return this.map.get(key);
    }

    public V computeIfAbsent(long key, LongFunction<V> func) {
        return this.map.computeIfAbsent(key, func);
    }

    public int size() {
        return this.map.size();
    }

    /**
     * Forget least-recently-read entries without closing their contents. Readers may still hold them;
     * the GC, rather than a reusable-array pool, must reclaim published worldgen data safely.
     */
    public int trimWithoutClosing(int capacity, java.util.function.Predicate<V> eligible) {
        int excess = this.map.size() - capacity;
        if (excess <= 0) return 0;
        var candidates = new java.util.ArrayList<V>();
        this.map.removeIf(entry -> {
            if (eligible.test(entry)) candidates.add(entry);
            return false;
        });
        candidates.sort(java.util.Comparator.comparingLong(ExpiringEntry::getTimestamp));
        var discard = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<V, Boolean>());
        candidates.stream().limit(excess).forEach(discard::add);
        return this.map.removeIf(discard::contains);
    }

    public void poll() {
        this.timeout = System.currentTimeMillis() - this.lifetimeMS;
        this.map.removeIf((entry) -> entry.getTimestamp() < this.timeout);
    }

	public boolean isPolling() {
		return this.poll != null && !this.poll.isDone();
	}

	@Override
	public void close() {
		if (this.poll != null) this.poll.cancel(false);
	}
}
