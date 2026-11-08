package nl.townsandkingdoms.wildshape.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.VanillaRegistries;
import com.google.gson.JsonParser;
import nl.townsandkingdoms.wildshape.data.preset.tags.PresetBiomeTagsProvider;

class OptionalHabitatDatapackTest {
    @BeforeAll static void bootstrap() { TestBootstrap.init(); }

    @Test void habitatBridgesExportWithoutAnyOptionalMods(@TempDir Path directory) throws Exception {
        var preset = BuiltinPresetRenderTest.presets().get("default").get();
        var pack = PresetDatapack.generate(preset);
        assertTrue(pack.errors().isEmpty(), String.join("\n", pack.errors()));
        var generator = new DataGenerator(directory.resolve("cache"), SharedConstants.getCurrentVersion(), true);
        var generated = generator.new PackGenerator(true, "preset", new PackOutput(directory.resolve("pack")));
        generated.addProvider(output -> new PresetBiomeTagsProvider(preset, output,
                CompletableFuture.completedFuture(VanillaRegistries.createLookup())));
        generator.run();
        for (String path : List.of("c/is_hot_overworld", "integrated_api/collections/swamps",
                "hapi/deep_temperate_oceans", "wildernature/spawns_scorpion")) {
            String[] split = path.split("/", 2);
            var tag = JsonParser.parseString(Files.readString(directory.resolve("pack/data/" + split[0]
                    + "/tags/worldgen/biome/" + split[1] + ".json"))).getAsJsonObject();
            assertFalse(tag.has("replace") && tag.get("replace").getAsBoolean());
            for (var reference : tag.getAsJsonArray("values")) {
                assertTrue(reference.isJsonObject());
                assertFalse(reference.getAsJsonObject().get("required").getAsBoolean());
            }
        }
        assertTrue(pack.files().keySet().stream().noneMatch(path -> path.contains("/biome_modifier/")
                && !path.contains("/wildshape/")), "compatibility must not duplicate native spawn modifiers");
    }
}
