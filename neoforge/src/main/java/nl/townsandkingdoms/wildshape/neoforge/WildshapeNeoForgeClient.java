package nl.townsandkingdoms.wildshape.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.client.gui.screen.presetconfig.PresetConfigScreen;
import nl.townsandkingdoms.wildshape.data.preset.WildshapeWorldPresets;

// client only, so dedicated servers never load the screens
@Mod(value = WildshapeCommon.MOD_ID, dist = Dist.CLIENT)
public class WildshapeNeoForgeClient {

	public WildshapeNeoForgeClient(IEventBus modBus) {
		modBus.addListener(WildshapeNeoForgeClient::registerPresetEditors);
	}

	private static void registerPresetEditors(RegisterPresetEditorsEvent event) {
		event.register(WildshapeWorldPresets.WILDSHAPE, (screen, ctx) -> new PresetConfigScreen(screen));
	}
}
