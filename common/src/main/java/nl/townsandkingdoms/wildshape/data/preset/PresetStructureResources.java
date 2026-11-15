package nl.townsandkingdoms.wildshape.data.preset;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.data.preset.settings.Preset;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;

/** World-scoped late binding for dedicated-server presets, whose initial export sees only vanilla registries. */
public final class PresetStructureResources {
    private static final Map<ResourceManager, Optional<Preset>> PRESETS = new WeakHashMap<>();

    private PresetStructureResources() {}

    public static Resource patch(ResourceManager resources, ResourceLocation id, Resource original) throws IOException {
        var preset = preset(resources);
        if (preset.isEmpty()) return original;
        if (!preset.get().structures().entries.containsKey(id) && !StructureSetPolicy.selected(id)) return original;
        try (var reader = original.openAsReader()) {
            var json = JsonParser.parseReader(reader);
            if (!StructureSetJsonPatcher.patch(preset.get(), id, json)) return original;
            byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);
            return new Resource(original.source(), () -> new ByteArrayInputStream(data), original::metadata);
        }
    }

    private static Optional<Preset> preset(ResourceManager resources) throws IOException {
        synchronized (PRESETS) {
            var cached = PRESETS.get(resources);
            if (cached != null) return cached;
            // Registries.elementsDirPath also handles NeoForge's namespace prefix for custom registries.
            var id = WildshapeCommon.location(Registries.elementsDirPath(WildshapeRegistries.PRESET) + "/preset.json");
            var resource = resources.getResource(id);
            Optional<Preset> result = Optional.empty();
            if (resource.isPresent()) {
                try (var reader = resource.get().openAsReader()) {
                    result = Optional.of(Preset.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow());
                }
            }
            PRESETS.put(resources, result);
            return result;
        }
    }
}
