package nl.townsandkingdoms.wildshape.compat.skylands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import nl.townsandkingdoms.wildshape.WildshapeCommon;
import nl.townsandkingdoms.wildshape.registries.WildshapeRegistries;

/**
 * SkyLands 0.9.4 places its islands as features, above the normal noise terrain.
 * A saved Wildshape preset otherwise shadows its Overworld dimension expansion.
 * Bind the extra build space when registries load, including existing worlds,
 * without changing the saved terrain preset, noise height or other dimensions.
 */
public final class SkylandsDimensionResources {
    // SkyLands 0.9.4: min_y=-64, height=592; islands/foliage extend to Y=527.
    static final int MINIMUM_MAX_Y = 528;
    private static final ResourceLocation OVERWORLD = ResourceLocation.withDefaultNamespace("overworld");
    private static final ResourceLocation DIMENSION_FILE = ResourceLocation.withDefaultNamespace("dimension_type/overworld.json");

    private SkylandsDimensionResources() {}

    public static Resource patch(ResourceManager resources, ResourceLocation id, Resource original,
            boolean skylandsLoaded) throws IOException {
        if (!skylandsLoaded || !OVERWORLD.equals(id)) return original;
        var presetFile = WildshapeCommon.location(Registries.elementsDirPath(WildshapeRegistries.PRESET) + "/preset.json");
        if (resources.getResource(presetFile).isEmpty()) return original;

        int requiredMaxY = MINIMUM_MAX_Y;
        // Preserve a higher ceiling requested by the installed SkyLands resources
        // or another enabled pack. Never depend on a loader-specific pack ID.
        for (Resource layer : resources.getResourceStack(DIMENSION_FILE)) {
            try (var reader = layer.openAsReader()) {
                var value = JsonParser.parseReader(reader);
                if (value.isJsonObject()) {
                    var json = value.getAsJsonObject();
                    if (json.has("min_y") && json.has("height"))
                        requiredMaxY = Math.max(requiredMaxY,
                                Math.addExact(json.get("min_y").getAsInt(), json.get("height").getAsInt()));
                }
            }
        }
        try (var reader = original.openAsReader()) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            if (!expand(json, requiredMaxY)) return original;
            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            return new Resource(original.source(), () -> new ByteArrayInputStream(bytes), original::metadata);
        }
    }

    static boolean expand(JsonObject dimension, int requiredMaxY) {
        int minY = dimension.get("min_y").getAsInt();
        int height = dimension.get("height").getAsInt();
        if ((long) minY + height >= requiredMaxY) return false;
        int expandedHeight = Math.multiplyExact(Math.floorDiv(Math.addExact(requiredMaxY - minY, 15), 16), 16);
        dimension.addProperty("height", expandedHeight);
        // Wildshape uses the full build height as its logical height. Retain that
        // invariant so portal searches can reach the newly available sky layer.
        dimension.addProperty("logical_height", expandedHeight);
        return true;
    }
}
