package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Immutable terrain suitability profiles. Native structure placement is never rewritten here. */
public final class StructurePlacementProfiles {
    public record Profile(String layer, int maxSlopeDelta, boolean rejectWater, int maxAnchorOffset, boolean rejectCavernVoid) {
        public Profile(String layer, int maxSlopeDelta, boolean rejectWater) { this(layer, maxSlopeDelta, rejectWater, 0, false); }
        public Profile(String layer, int maxSlopeDelta, boolean rejectWater, int maxAnchorOffset) { this(layer, maxSlopeDelta, rejectWater, maxAnchorOffset, false); }
        public boolean surface() { return layer.equals("surface"); }
    }

    public record Settings(boolean enabled, Map<String, Profile> structures, Set<String> legacyDisabledFallbackNamespaces) {
        public Settings {
            structures = Map.copyOf(structures);
            legacyDisabledFallbackNamespaces = Set.copyOf(legacyDisabledFallbackNamespaces);
        }
        public Profile structure(ResourceLocation id) {
            Profile explicit = structures.get(id.toString());
            if (explicit != null) return explicit;
            if (legacyDisabledFallbackNamespaces.contains(id.getNamespace())) return null;
            // Older copied profiles may not contain the targeted catacomb safety
            // entry. An explicit profile can still opt out with rejectCavernVoid=false.
            return id.toString().equals("irons_spellbooks:catacombs") ? CATACOMBS_FOUNDATION : null;
        }
    }

    private static final String RESOURCE = "/config/wildshape-structure-placement.json";
    private static final Profile CATACOMBS_FOUNDATION = new Profile("underground", 0, false, 0, true);
    private static volatile Settings settings = bundled();
    private StructurePlacementProfiles() {}
    public static Settings settings() { return settings; }

    public static synchronized void configure(boolean enabled, Path overrides) throws IOException {
        Settings next = bundled();
        if (overrides != null) {
            if (Files.exists(overrides)) {
                try (var reader = Files.newBufferedReader(overrides)) {
                    next = parse(JsonParser.parseReader(reader).getAsJsonObject());
                } catch (RuntimeException invalid) {
                    throw new IOException("Invalid Wildshape structure terrain profiles: " + overrides, invalid);
                }
            } else {
                Files.createDirectories(overrides.toAbsolutePath().getParent());
                try (var input = StructurePlacementProfiles.class.getResourceAsStream(RESOURCE)) {
                    if (input == null) throw new IOException("Missing bundled structure terrain profiles");
                    Files.copy(input, overrides);
                }
            }
        }
        settings = new Settings(enabled, next.structures, next.legacyDisabledFallbackNamespaces);
    }

    private static Settings bundled() {
        try (var input = StructurePlacementProfiles.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing bundled structure terrain profiles");
            return parse(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException e) { throw new IllegalStateException("Cannot read bundled structure terrain profiles", e); }
    }

    static Settings parse(JsonObject root) {
        Map<String, Profile> structures = new HashMap<>();
        if (root.has("structures")) for (var entry : root.getAsJsonObject("structures").entrySet()) {
            String id = entry.getKey();
            ResourceLocation.parse(id);
            JsonObject value = entry.getValue().getAsJsonObject();
            String layer = value.has("layer") ? value.get("layer").getAsString() : "native";
            if (!Set.of("native", "surface", "underground", "ocean", "sky", "disabled").contains(layer))
                throw new IllegalArgumentException("Unknown terrain profile layer " + layer);
            int slope = value.has("maxSlopeDelta") ? value.get("maxSlopeDelta").getAsInt() : 32;
            int anchor = value.has("maxAnchorOffset") ? value.get("maxAnchorOffset").getAsInt() : 0;
            if (slope < 0 || slope > 256 || anchor < 0 || anchor > 256)
                throw new IllegalArgumentException("Unbounded structure terrain profile " + id);
            boolean water = value.has("rejectWater") && value.get("rejectWater").getAsBoolean();
            boolean cavern = value.has("rejectCavernVoid") ? value.get("rejectCavernVoid").getAsBoolean()
                    : id.equals("irons_spellbooks:catacombs");
            structures.put(id, new Profile(layer, slope, water, anchor, cavern));
        }
        // Old namespace profiles never applied terrain tests. Preserve only their
        // explicit suppression of the fallback above, ignoring retired spacing data.
        Set<String> disabledFallbackNamespaces = new HashSet<>();
        if (root.has("namespaces")) for (var entry : root.getAsJsonObject("namespaces").entrySet()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            if (value.has("layer") && value.get("layer").getAsString().equals("disabled"))
                disabledFallbackNamespaces.add(entry.getKey());
        }
        return new Settings(true, structures, disabledFallbackNamespaces);
    }
}
