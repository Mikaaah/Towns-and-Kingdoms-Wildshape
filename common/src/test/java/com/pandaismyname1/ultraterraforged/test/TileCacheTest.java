package com.pandaismyname1.ultraterraforged.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.pandaismyname1.ultraterraforged.world.worldgen.GeneratorContext;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.Tile;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileCache;
import com.pandaismyname1.ultraterraforged.world.worldgen.tile.TileGenerator;

/**
 * The tile cache lets a tile go once every chunk of it is done with it, and doesn't keep tiles that failed (audit,
 * discussion #12: a failed tile failed everyone who asked for it, and kept itself from expiring).
 */
public class TileCacheTest {
	private static final int TILE_SIZE = 3;

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@Test
	void aFailedTileIsMadeAgain() {
		TileGenerator generator = context().generator;
		AtomicInteger calls = new AtomicInteger();
		TileGenerator failingOnce = new TileGenerator(null, null, TILE_SIZE, 1, 1) {
			@Override
			public CompletableFuture<Tile> generate(int tileX, int tileZ) {
				if (calls.getAndIncrement() == 0) {
					return CompletableFuture.failedFuture(new IllegalStateException("first try fails"));
				}
				return generator.generate(tileX, tileZ);
			}
		};
		TileCache cache = new TileCache(TILE_SIZE, false, failingOnce);
		assertThrows(RuntimeException.class, () -> cache.provide(2, 5));
		assertNull(cache.provideIfPresent(2, 5), "the failed tile stayed in the cache");
		assertNotNull(cache.provide(2, 5));
	}

	@Test
	void aChunkDoneTwiceCountsOnce() {
		TileCache cache = new TileCache(TILE_SIZE, false, context().generator);
		int tileX = -1;
		int tileZ = 4;
		Tile tile = cache.provide(tileX, tileZ);
		int chunks = 1 << TILE_SIZE;
		int firstX = tileX * chunks;
		int firstZ = tileZ * chunks;
		for (int i = 0; i < chunks * chunks; i++) {
			cache.dropAtChunk(firstX + 3, firstZ + 6);
		}
		assertSame(tile, cache.provideIfPresent(tileX, tileZ), "one chunk done many times let the tile go");

		for (int z = 0; z < chunks; z++) {
			for (int x = 0; x < chunks; x++) {
				cache.dropAtChunk(firstX + x, firstZ + z);
			}
		}
		assertNull(cache.provideIfPresent(tileX, tileZ), "every chunk is done, but the tile stayed");
		assertTrue(tile.lookup(firstX << 4, firstZ << 4).height > 0,
				"finishing chunks reset published cells still held by a column reader");
	}

	@Test
	void partialTilesAreBoundedWithoutInvalidatingExistingReaders() {
		TileCache cache = new TileCache(TILE_SIZE, false, context().generator, 2);
		Tile retained = cache.provide(0, 0);
		float height = retained.lookup(4, 4).height;
		for (int x = 1; x <= 8; x++) cache.provide(x, 0);
		assertTrue(cache.retainedTileCount() <= 2, "lookup-only tiles accumulated without a limit");
		assertNull(cache.provideIfPresent(0, 0));
		assertEquals(height, retained.lookup(4, 4).height, "eviction recycled a reader's cells");
		assertEquals(height, cache.provide(0, 0).lookup(4, 4).height, "regeneration changed terrain");
	}

	private static GeneratorContext context() {
		return GeneratorContext.makeCached(BuiltinPresetRenderTest.presets().get("default").get(), PresetRenderer.SEED, TILE_SIZE, 1, false);
	}
}
