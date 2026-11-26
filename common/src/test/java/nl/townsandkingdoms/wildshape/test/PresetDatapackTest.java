package nl.townsandkingdoms.wildshape.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.preset.option.PresetOptions;

// The datapack a preset turns into when a world is created must build and serialize, and reflect the settings
public class PresetDatapackTest {
	private static final String PRESET_FILE = "data/wildshape/wildshape/worldgen/preset/preset.json";
	private static final String NOISE_SETTINGS = "data/minecraft/worldgen/noise_settings/overworld.json";
	private static final String CAVE_CARVER = "data/minecraft/worldgen/configured_carver/cave.json";

	@BeforeAll
	static void bootstrap() {
		TestBootstrap.init();
	}

	@TestFactory
	Stream<DynamicTest> everyBuiltinPresetGeneratesAValidDatapack() {
		return BuiltinPresetRenderTest.presets().entrySet().stream().map((entry) -> DynamicTest.dynamicTest(entry.getKey(), () -> {
			Preset preset = entry.getValue().get();
			PresetDatapack.Result pack = PresetDatapack.generate(preset);
			assertTrue(pack.errors().isEmpty(), String.join("\n", pack.errors()));
			for (String file : List.of(PRESET_FILE, NOISE_SETTINGS, CAVE_CARVER, "data/minecraft/dimension_type/overworld.json")) {
				assertTrue(pack.files().containsKey(file), "missing " + file);
			}
			// the preset travels with the world and reads back unchanged
			Preset stored = Preset.CODEC.parse(JsonOps.INSTANCE, pack.files().get(PRESET_FILE)).getOrThrow((error) -> new AssertionError(error));
			assertEquals(PresetOptionsTest.encode(preset), PresetOptionsTest.encode(stored));
		}));
	}

	// other dimensions, modded ones especially, build on vanilla's overworld functions: replacing those gave them the
	// overworld's terrain
	@Test
	void vanillaDensityFunctionsAreLeftAlone() {
		PresetDatapack.Result pack = PresetDatapack.generate(defaultPreset());
		assertTrue(pack.files().keySet().stream().noneMatch((file) -> file.startsWith("data/minecraft/worldgen/density_function/")), String.join("\n", pack.files().keySet()));
		assertTrue(pack.files().containsKey("data/wildshape/worldgen/density_function/overworld/sloped_cheese.json"));
		// the overworld reads Wildshape's, and vanilla's only where they're unchanged
		String noise = pack.files().get(NOISE_SETTINGS).toString();
		assertTrue(noise.contains("\"wildshape:overworld/sloped_cheese\""), noise);
		for (String replaced : List.of("continents", "erosion", "ridges", "offset", "factor", "depth", "jaggedness", "sloped_cheese", "caves/noodle", "caves/entrances", "caves/spaghetti_2d")) {
			assertTrue(!noise.contains("\"minecraft:overworld/" + replaced + "\""), replaced);
		}
	}

    @Test
    void waterSpringsStayDisabledEvenForAnOlderEnabledPreset() throws Exception {
        Preset preset = defaultPreset();
        preset.miscellaneous().vanillaSprings = true;
        preset.caveFeatures().springs.enabled = true;
        preset.caveFeatures().springs.frequency = 1.0F;
        JsonObject spring = file(preset, "data/minecraft/worldgen/placed_feature/spring_water.json").getAsJsonObject();
        var placements = spring.getAsJsonArray("placement");
        assertEquals(1, placements.size());
        assertEquals("minecraft:overworld", placements.get(0).getAsJsonObject().getAsJsonArray("blacklist").get(0).getAsString());
        var context = nl.townsandkingdoms.wildshape.world.worldgen.GeneratorContext.makeUncached(preset, 1234, 1, 0, 1);
        var caves = new nl.townsandkingdoms.wildshape.world.worldgen.cave.CaveFeatures(context);
        var field = caves.getClass().getDeclaredField("features");
        field.setAccessible(true);
        for (Object feature : (java.util.List<?>) field.get(caves))
            assertTrue(!feature.getClass().getSimpleName().equals("Springs"));
    }

	@Test
	void worldHeightReachesTheDimension() {
		Preset preset = defaultPreset();
		PresetOptions.WORLD_HEIGHT.set(preset, 512);
		PresetOptions.WORLD_DEPTH.set(preset, 128);
		JsonObject noise = file(preset, NOISE_SETTINGS).getAsJsonObject().getAsJsonObject("noise");
		assertEquals(-128, noise.get("min_y").getAsInt());
		assertEquals(640, noise.get("height").getAsInt());
	}

	@Test
	void caveSettingsReachTheCarvers() {
		Preset preset = defaultPreset();
		preset.caves().caveCarverProbability = 0.5F;
		assertEquals(0.5F, file(preset, CAVE_CARVER).getAsJsonObject().getAsJsonObject("config").get("probability").getAsFloat());
	}

	@Test
	void defaultsDoNotExportStructureSets() {
		assertTrue(PresetDatapack.generate(defaultPreset()).files().keySet().stream().noneMatch((file) -> file.contains("/worldgen/structure_set/")));
	}

	@Test
	void legacyStructureOverridesAreIgnored() {
		JsonObject defaults = PresetOptionsTest.encode(defaultPreset());
		JsonObject json = defaults.deepCopy();
		json.add("structures", JsonParser.parseString("""
				{
				  "minecraft:villages": {"spacing": 20, "separation": 25},
				  "minecraft:pillager_outposts": {"enabled": false},
				  "dungeons_arise:major_structures": {"spacing": 32, "separation": 24}
				}
				"""));
		Preset preset = Preset.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
		PresetDatapack.Result pack = PresetDatapack.generate(preset);
		assertTrue(pack.errors().isEmpty(), String.join("\n", pack.errors()));
		assertEquals(defaults, pack.files().get(PRESET_FILE));
		assertTrue(pack.files().keySet().stream().noneMatch((file) -> file.contains("/worldgen/structure_set/")));
	}

	private static JsonElement file(Preset preset, String path) {
		PresetDatapack.Result pack = PresetDatapack.generate(preset);
		assertTrue(pack.errors().isEmpty(), String.join("\n", pack.errors()));
		return pack.files().get(path);
	}

	private static Preset defaultPreset() {
		return BuiltinPresetRenderTest.presets().get("default").get();
	}
}
