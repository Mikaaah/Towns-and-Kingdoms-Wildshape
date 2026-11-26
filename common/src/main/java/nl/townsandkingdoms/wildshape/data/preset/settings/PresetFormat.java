package nl.townsandkingdoms.wildshape.data.preset.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

/**
 * Versioning for serialized presets, both preset files and the preset stored in a world's datapack.
 *
 * Encoded presets carry a "version" field. Older layouts are upgraded before decoding so the settings classes
 * only have to understand the current layout. Files written
 * before versioning existed (Wildshape 0.0.7 and earlier) count as version 0.
 */
public final class PresetFormat {
	public static final int CURRENT_VERSION = 2;
	public static final String VERSION_KEY = "version";

	public static Codec<Preset> versioned(Codec<Preset> codec) {
		return new Codec<>() {

			@Override
			public <T> DataResult<Pair<Preset, T>> decode(DynamicOps<T> ops, T input) {
				JsonElement json = ops.convertTo(JsonOps.INSTANCE, input);
				return upgrade(json).flatMap((upgraded) -> codec.parse(JsonOps.INSTANCE, upgraded)).map((preset) -> Pair.of(preset, input));
			}

			@Override
			public <T> DataResult<T> encode(Preset preset, DynamicOps<T> ops, T prefix) {
				return codec.encodeStart(JsonOps.INSTANCE, preset).flatMap((json) -> {
					if (!json.isJsonObject()) {
						return DataResult.error(() -> "Preset didn't encode to an object: " + json);
					}
					// version goes first so it's easy to spot in the file
					JsonObject versioned = new JsonObject();
					versioned.addProperty(VERSION_KEY, CURRENT_VERSION);
					json.getAsJsonObject().entrySet().forEach((entry) -> versioned.add(entry.getKey(), entry.getValue()));
					T encoded = JsonOps.INSTANCE.convertTo(ops, versioned);
					if (prefix.equals(ops.empty())) {
						return DataResult.success(encoded);
					}
					return ops.getMap(encoded).flatMap((map) -> ops.mergeToMap(prefix, map));
				});
			}

			@Override
			public String toString() {
				return "VersionedPreset[" + codec + "]";
			}
		};
	}

	/**
	 * Brings a serialized preset of any supported version up to {@link #CURRENT_VERSION}, without the version field.
	 */
	public static DataResult<JsonObject> upgrade(JsonElement json) {
		if (!json.isJsonObject()) {
			return DataResult.error(() -> "Preset must be a JSON object, got: " + json);
		}
		JsonObject object = json.getAsJsonObject().deepCopy();
		JsonElement versionElement = object.remove(VERSION_KEY);
		int version;
		try {
			version = versionElement == null ? 0 : versionElement.getAsInt();
		} catch (RuntimeException e) {
			return DataResult.error(() -> "Invalid preset version: " + versionElement);
		}
		if (version < 0) {
			return DataResult.error(() -> "Invalid preset version: " + version);
		}
		if (version > CURRENT_VERSION) {
			return DataResult.error(() -> "Preset version " + version + " was made with a newer Wildshape (this version reads up to " + CURRENT_VERSION + ")");
		}
		if (version == 0) {
			renameMushroomIslandPoints(object);
		}
		object.remove("structures");
		return DataResult.success(object);
	}

	// 0 -> 1: the mushroom island control points were renamed to island points
	private static void renameMushroomIslandPoints(JsonObject preset) {
		JsonObject controlPoints = getObject(preset, "world", "controlPoints");
		if (controlPoints != null) {
			rename(controlPoints, "mushroomFieldsInland", "islandInland");
			rename(controlPoints, "mushroomFieldsCoast", "islandCoast");
		}
	}

	private static void rename(JsonObject object, String from, String to) {
		JsonElement value = object.remove(from);
		if (value != null && !object.has(to)) {
			object.add(to, value);
		}
	}

	private static JsonObject getObject(JsonObject object, String... path) {
		JsonObject current = object;
		for (String key : path) {
			JsonElement child = current.get(key);
			if (child == null || !child.isJsonObject()) {
				return null;
			}
			current = child.getAsJsonObject();
		}
		return current;
	}

	private PresetFormat() {
	}
}
