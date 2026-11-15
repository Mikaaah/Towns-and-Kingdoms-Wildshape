package nl.townsandkingdoms.wildshape.compat.worldpreview;

import nl.townsandkingdoms.wildshape.platform.ModLoaderUtil;

public class WPCompat {
	
	public static boolean isEnabled() {
		return ModLoaderUtil.isLoaded("world_preview");
	}
}
