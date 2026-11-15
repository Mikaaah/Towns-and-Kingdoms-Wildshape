package nl.townsandkingdoms.wildshape.platform.neoforge;

import net.neoforged.fml.loading.FMLLoader;

public class ModLoaderUtilImpl {
	
	public static boolean isLoaded(String modId) {
		return FMLLoader.getLoadingModList().getModFileById(modId) != null;
	}

	public static boolean isDedicatedServer() {
		return FMLLoader.getDist().isDedicatedServer();
	}

	public static String loaderName() {
		return "neoforge";
	}
}
