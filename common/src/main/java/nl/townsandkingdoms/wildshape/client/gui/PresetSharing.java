package nl.townsandkingdoms.wildshape.client.gui;

import java.nio.file.Path;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.Component;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.client.data.WildshapeTranslationKeys;
import nl.townsandkingdoms.wildshape.data.preset.PresetShareCode;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.platform.ConfigUtil;

// Copying and pasting preset codes through the clipboard, and where players' own presets are kept
public final class PresetSharing {

	public static Path presetFolder() {
		return ConfigUtil.wildshape("presets");
	}

	public static void copy(Preset preset) {
		Minecraft.getInstance().keyboardHandler.setClipboard(PresetShareCode.encode(preset));
		Toasts.notify(WildshapeTranslationKeys.GUI_SHARE_COPIED, Component.empty(), SystemToastId.PERIODIC_NOTIFICATION);
	}

	public static Optional<Preset> paste() {
		String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
		return PresetShareCode.decode(clipboard).resultOrPartial((error) -> {
			WildshapeCommon.LOGGER.warn("Couldn't read the preset code in the clipboard: {}", error);
			Toasts.notify(WildshapeTranslationKeys.GUI_SHARE_INVALID, Component.literal(error), SystemToastId.PACK_LOAD_FAILURE);
		}).map((preset) -> {
			Toasts.notify(WildshapeTranslationKeys.GUI_SHARE_PASTED, Component.empty(), SystemToastId.PERIODIC_NOTIFICATION);
			return preset;
		});
	}

	private PresetSharing() {
	}
}
