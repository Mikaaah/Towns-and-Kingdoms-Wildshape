package nl.townsandkingdoms.wildshape.neoforge;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.client.data.WildshapeLanguageProvider;
import nl.townsandkingdoms.wildshape.client.data.WildshapeTranslationKeys;
import nl.townsandkingdoms.wildshape.platform.neoforge.RegistryUtilImpl;
import nl.townsandkingdoms.wildshape.server.WildshapeMinecraftServer;

@Mod(WildshapeCommon.MOD_ID)
public class WildshapeNeoForge {

    public WildshapeNeoForge(IEventBus modBus, ModContainer modContainer) {
        try {
            nl.townsandkingdoms.wildshape.platform.ConfigMigration.migrate(
                net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot initialize Wildshape configuration", e);
        }
	WildshapeCommon.bootstrap();
        if (Boolean.getBoolean("wildshape.compatSmoke")) {
            nl.townsandkingdoms.wildshape.neoforge.compat.CompatibilitySmokeChecks.register();
        }
        // Native currents are a built-in, optional NeoForge feature.
        if (ModList.get().isLoaded("streamsreflowing")) {
            nl.townsandkingdoms.wildshape.compat.streams.WildshapeStreams.initialize(modBus, modContainer);
        }

	modBus.addListener(WildshapeNeoForge::gatherData);
	NeoForge.EVENT_BUS.addListener(WildshapeNeoForge::addReloadListeners);

	RegistryUtilImpl.register(modBus);
    }

    private static void gatherData(GatherDataEvent event) {
	boolean includeClient = event.includeClient();
	DataGenerator generator = event.getGenerator();
	PackOutput output = generator.getPackOutput();

	generator.addProvider(includeClient, new WildshapeLanguageProvider.EnglishUS(output));
	generator.addProvider(includeClient, PackMetadataGenerator.forFeaturePack(output, Component.translatable(WildshapeTranslationKeys.METADATA_DESCRIPTION)));
    }

    // /reload: templates are read again from the new resources. The first load needs nothing: the server reads them
    // when it first asks for its template manager.
    private static void addReloadListeners(AddReloadListenerEvent event) {
	event.addListener((ResourceManagerReloadListener) (resourceManager) -> {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server instanceof WildshapeMinecraftServer wildshapeServer) {
			wildshapeServer.getFeatureTemplateManager().onReload(resourceManager);
		}
	});
    }
}
