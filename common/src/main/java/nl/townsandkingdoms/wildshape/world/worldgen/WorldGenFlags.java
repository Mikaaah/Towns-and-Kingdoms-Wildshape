package nl.townsandkingdoms.wildshape.world.worldgen;

import java.util.function.Supplier;

/** Accurate generation on every thread; only explicitly scoped searches use approximate terrain. */
public class WorldGenFlags {
	private static final ThreadLocal<Boolean> FAST_LOOKUP = ThreadLocal.withInitial(() -> false);
	private static final ThreadLocal<Boolean> STRUCTURE_SEARCH = ThreadLocal.withInitial(() -> false);

	public static boolean structureSearch() {
		return STRUCTURE_SEARCH.get();
	}

	/** A locator may inspect thousands of candidates; it must not survey their river networks. */
	public static <T> T locatingStructure(Supplier<T> search) {
		boolean previous = STRUCTURE_SEARCH.get();
		STRUCTURE_SEARCH.set(true);
		try {
			return approximately(search);
		} finally {
			STRUCTURE_SEARCH.set(previous);
		}
	}

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
