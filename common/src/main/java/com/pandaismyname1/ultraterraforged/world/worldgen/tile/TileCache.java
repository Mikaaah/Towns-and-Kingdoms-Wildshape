package com.pandaismyname1.ultraterraforged.world.worldgen.tile;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import org.jetbrains.annotations.Nullable;

import com.pandaismyname1.ultraterraforged.concurrent.cache.Cache;
import com.pandaismyname1.ultraterraforged.concurrent.cache.CacheEntry;
import com.pandaismyname1.ultraterraforged.concurrent.cache.CacheManager;
import com.pandaismyname1.ultraterraforged.concurrent.cache.map.StampedLongMap;
import com.pandaismyname1.ultraterraforged.world.worldgen.util.PosUtil;

public class TileCache implements TileFactory {
	private int tileSize;
	private boolean queue;
	private Cache<CacheEntry<Entry>> cache;
	private TileGenerator generator;
	private final int retainedTileLimit;

	public TileCache(int tileSize, boolean queue, TileGenerator generator) {
		this(tileSize, queue, generator, Math.max(2, Integer.getInteger("wildshape.tileCacheLimit", 64)));
	}

	public TileCache(int tileSize, boolean queue, TileGenerator generator, int retainedTileLimit) {
		this.tileSize = tileSize;
		this.queue = queue;
		this.retainedTileLimit = Math.max(1, retainedTileLimit);
		// Published cells may be held by readers outside the cache. Forgetting an
		// entry must never reset/recycle its arrays while those readers are active.
        this.cache = CacheManager.createCache(256, Long.MAX_VALUE, 20L, TimeUnit.MINUTES, StampedLongMap::new);
		this.generator = generator;
	}

	public TileGenerator getGenerator() {
		return this.generator;
	}

	@Nullable
	public Tile provideIfPresent(int tileX, int tileZ) {
		@Nullable
		CacheEntry<Entry> entry = this.cache.get(PosUtil.pack(tileX, tileZ));
		if (entry == null || !entry.isDone()) return null;
        try {
            return entry.get().tile;
        } catch (RuntimeException failure) {
            this.discardFailed(PosUtil.pack(tileX, tileZ), entry);
            throw failure;
        }
	}

	@Nullable
	public Tile provideAtChunkIfPresent(int chunkX, int chunkZ) {
		return this.provideIfPresent(this.chunkToTile(chunkX), this.chunkToTile(chunkZ));
	}

	@Override
	public Tile provide(int tileX, int tileZ) {
		long key = PosUtil.pack(tileX, tileZ);
        CacheEntry<Entry> entry = this.computeEntry(tileX, tileZ);
        try {
            Tile tile = entry.get().tile;
            this.trim(entry);
            return tile;
        } catch (RuntimeException failure) {
            this.discardFailed(key, entry);
            throw failure;
        }
	}

	@Override
	public void queue(int tileX, int tileZ) {
		if(this.queue) {
			this.trim(this.computeEntry(tileX, tileZ));
		}
	}

	@Override
	public void dropAtChunk(int chunkX, int chunkZ) {
		long packedTilePos = PosUtil.pack(this.chunkToTile(chunkX), this.chunkToTile(chunkZ));
		CacheEntry<Entry> entry = this.cache.get(packedTilePos);
		if(entry != null && entry.get().drop(chunkX, chunkZ)) {
			// Identity removal cannot discard a replacement installed concurrently.
			// A late column reader can still hold this tile: leave its arrays to GC.
			this.cache.forget(entry);
		}
	}

	public int retainedTileCount() {
		return this.cache.size();
	}

	private void trim(CacheEntry<Entry> keep) {
		this.cache.trimWithoutClosing(this.retainedTileLimit, entry -> entry != keep && entry.isDone());
	}

    // A failed tile must not poison the same coordinates forever.
    // Check identity to avoid evicting a replacement entry that another thread created.
    private void discardFailed(long key, CacheEntry<Entry> failed) {
        if (failed.isDone()) this.cache.forget(failed);
    }

	@Override
	public int chunkToTile(int chunkCoord) {
		return chunkCoord >> this.tileSize;
	}

	private CacheEntry<Entry> computeEntry(int tileX, int tileZ) {
		return this.cache.computeIfAbsent(PosUtil.pack(tileX, tileZ), (k) -> {
			return CacheEntry.supply(this.generator.generate(tileX, tileZ).thenApply(Entry::new));
		});
	}

	private class Entry {
		private AtomicInteger refCount;
		// The chunks done with a tile, one bit each. Repeated feature passes count
		// once; finishing the full tile releases its cache entry before the capacity limit.
		private AtomicLongArray dropped;
		private int chunkCount;
		private Tile tile;

		public Entry(Tile tile) {
			int tileSize = tile.getChunkSize().size();
			this.refCount = new AtomicInteger();
			this.chunkCount = tileSize * tileSize;
			this.dropped = new AtomicLongArray((this.chunkCount + 63) >> 6);
			this.tile = tile;
		}

		// whether every chunk of the tile is done with it; true once only
		public boolean drop(int chunkX, int chunkZ) {
			Size size = this.tile.getChunkSize();
			int index = size.mask(chunkZ) * size.size() + size.mask(chunkX);
			long bit = 1L << (index & 63);
			if((this.dropped.getAndAccumulate(index >> 6, bit, (bits, chunk) -> bits | chunk) & bit) != 0L) {
				return false;
			}
			return this.refCount.incrementAndGet() == this.chunkCount;
		}
	}
}
