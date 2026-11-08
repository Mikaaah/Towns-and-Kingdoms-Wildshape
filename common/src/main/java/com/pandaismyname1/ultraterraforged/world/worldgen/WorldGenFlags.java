package com.pandaismyname1.ultraterraforged.world.worldgen;

import java.util.function.Supplier;

/** Accurate generation on every thread; only explicitly scoped searches use approximate terrain. */
public class WorldGenFlags {
	private static final ThreadLocal<Boolean> FAST_LOOKUP = ThreadLocal.withInitial(() -> false);

	public static void setFastCellLookups(boolean fastLookups) {
		FAST_LOOKUP.set(fastLookups);
	}

	public static boolean fastLookups() {
		return FAST_LOOKUP.get();
	}

	public static <T> T approximately(Supplier<T> search) {
		boolean previous = FAST_LOOKUP.get();
		FAST_LOOKUP.set(true);
		try {
			return search.get();
		} finally {
			FAST_LOOKUP.set(previous);
		}
	}
}
