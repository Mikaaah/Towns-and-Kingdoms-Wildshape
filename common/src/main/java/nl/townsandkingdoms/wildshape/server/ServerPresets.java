package nl.townsandkingdoms.wildshape.server;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Supplier;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.registries.VanillaRegistries;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.data.PresetPacks;
import nl.townsandkingdoms.wildshape.data.preset.PresetLibrary;
import nl.townsandkingdoms.wildshape.data.preset.WildshapeWorldPresets;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.platform.ConfigUtil;

/**
 * Dedicated server support: a new world whose server.properties has {@code level-type=wildshape:wildshape}
 * gets the preset from {@code config/wildshape/server-preset.json} installed as a datapack before the world's
 * datapacks load. The file is created from the modpack's default preset if it doesn't exist yet. Vanilla enables new
 * packs in a world's datapacks folder automatically, and the pack then stays with the world like any other.
 */
public final class ServerPresets {
	public static final String PRESET_FILE = "server-preset.json";

	/**
	 * Called with a world's datapacks folder while the server sets up its pack repository.
	 */
	public static void installIfNewWorld(Path datapackDir) {
		Path worldDir = datapackDir.toAbsolutePath().getParent();
		if (worldDir == null || Files.exists(worldDir.resolve("level.dat"))) {
			return;
		}
		if (!isWildshapeLevelType(Path.of("server.properties"))) {
			return;
		}
		try {
			Preset preset = loadOrCreatePreset(ConfigUtil.wildshape(PRESET_FILE), PresetLibrary::defaultPreset);
			// Prepare the core preset from vanilla; modded structure rules bind when pack resources load.
			PresetPacks.export(preset, VanillaRegistries.createLookup(), datapackDir.resolve(PresetPacks.WORLD_PACK_NAME));
			WildshapeCommon.LOGGER.info("Creating a Wildshape world with the preset from {}", ConfigUtil.wildshape(PRESET_FILE));
		} catch (IOException | RuntimeException e) {
			// failing here would silently produce vanilla terrain, so stop instead
			throw new IllegalStateException("Couldn't set up the Wildshape preset for the new world", e);
		}
	}

	static boolean isWildshapeLevelType(Path serverProperties) {
		if (!Files.exists(serverProperties)) {
			return false;
		}
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(serverProperties)) {
			properties.load(reader);
		} catch (IOException e) {
			WildshapeCommon.LOGGER.warn("Couldn't read {}", serverProperties, e);
			return false;
		}
		String levelType = properties.getProperty("level-type", "").trim().toLowerCase(Locale.ROOT);
		return levelType.equals(WildshapeWorldPresets.WILDSHAPE.location().toString());
	}

	static Preset loadOrCreatePreset(Path file, Supplier<PresetLibrary.Entry> defaultPreset) throws IOException {
		if (!Files.exists(file)) {
			PresetLibrary.Entry defaults = defaultPreset.get();
			Preset preset = defaults.create();
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				JsonElement json = Preset.CODEC.encodeStart(JsonOps.INSTANCE, preset).getOrThrow();
				new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
			}
			WildshapeCommon.LOGGER.info("Wrote the {} preset to {}; edit it to change the terrain of new worlds", defaults.id(), file);
			return preset;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			return Preset.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow((error) -> new IllegalStateException("Invalid preset in " + file + ": " + error));
		}
	}

	private ServerPresets() {
	}
}
