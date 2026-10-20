package com.pandaismyname1.ultraterraforged.concurrent.cache;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import com.pandaismyname1.ultraterraforged.concurrent.cache.map.LongMap;
import com.pandaismyname1.ultraterraforged.concurrent.cache.map.StampedBoundLongMap;

public class CacheManager {
	private static final ConcurrentLinkedQueue<WeakReference<Cache<?>>> CACHES = new ConcurrentLinkedQueue<>();
	
	public static <V extends ExpiringEntry> Cache<V> createCache(int capacity, long expireTime, long pollInterval, TimeUnit unit) {
		return createCache(capacity, expireTime, pollInterval, unit, StampedBoundLongMap::new);
	}
	
	public static <V extends ExpiringEntry> Cache<V> createCache(int capacity, long expireTime, long pollInterval, TimeUnit unit, IntFunction<LongMap<V>> mapFunc) {
		Cache<V> cache = new Cache<>(capacity, expireTime, pollInterval, unit, mapFunc);
		CACHES.add(new WeakReference<>(cache));
        if (CACHES.size() > 512) CACHES.removeIf(reference -> reference.get() == null);
		return cache;
	}
	
	public static void clear() {
		for (WeakReference<Cache<?>> reference : CACHES) {
            Cache<?> cache = reference.get();
            if (cache != null) cache.close();
        }
		
		CACHES.clear();
	}
}