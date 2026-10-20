package com.pandaismyname1.ultraterraforged.world.worldgen.tile;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
	
	public TileCache(int tileSize, boolean queue, TileGenerator generator) {
		this.tileSize = tileSize;
		this.queue = queue;
		// Tile arrays belong to chunks still using them: retire only after drop().
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
            return entry.get().tile;
        } catch (RuntimeException failure) {
            this.discardFailed(key, entry);
            throw failure;
        }
	}
	
	@Override
	public void queue(int tileX, int tileZ) {
		if(this.queue) {
			this.computeEntry(tileX, tileZ);
		}
	}

	@Override
	public void drop(int tileX, int tileZ) {
		long packedTilePos = PosUtil.pack(tileX, tileZ);
		CacheEntry<Entry> entry = this.cache.get(packedTilePos);
		if(entry != null && entry.get().drop()) {
			// out of the cache first, so nothing new picks the tile up while it's being closed
			this.cache.remove(packedTilePos);
			entry.get().tile.close();
		}
	}

    // A failed tile must not poison the same coordinates forever.
    // Check identity to avoid evicting a replacement entry that another thread created.
    private void discardFailed(long key, CacheEntry<Entry> failed) {
        if (failed.isDone() && this.cache.get(key) == failed) this.cache.remove(key);
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
		private int chunkCount;
		private Tile tile;
		
		public Entry(Tile tile) {
			int tileSize = tile.getChunkSize().size();
			this.refCount = new AtomicInteger();
			this.chunkCount = tileSize * tileSize;
			this.tile = tile;
		}
		
		// whether every chunk of the tile is done with it; true once only
		public boolean drop() {
			return this.refCount.incrementAndGet() == this.chunkCount;
		}
	}
}