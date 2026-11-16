package nl.townsandkingdoms.wildshape.world.worldgen.structure.placement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Immutable startup policy. No dependency classes or optional registry references are resolved here. */
public final class StructurePlacementProfiles {
    public record Profile(int clearance, String layer, int maxSlopeDelta, boolean rejectWater, String linkedGroup, int maxAnchorOffset) {
        public Profile(int clearance, String layer, int maxSlopeDelta, boolean rejectWater, String linkedGroup) { this(clearance,layer,maxSlopeDelta,rejectWater,linkedGroup,0); }
        public boolean surface() { return layer.equals("surface"); }
    }
    public record Settings(boolean enabled, int maxCandidateChecks, boolean approximateBiomeScreening, Map<String, Profile> namespaces,
            Map<String, Profile> sets, Map<String, Profile> structures) {
        public Settings {
            namespaces = Map.copyOf(namespaces); sets = Map.copyOf(sets); structures = Map.copyOf(structures);
        }
        public Profile set(ResourceLocation id) {
            return sets.getOrDefault(id.toString(), namespaces.get(id.getNamespace()));
        }
        public Profile structure(ResourceLocation id) { return structures.get(id.toString()); }
    }
    private static volatile Settings settings = bundled(256, 112, 8192);
    private StructurePlacementProfiles() {}
    public static Settings settings() { return settings; }

    /** Call once before worlds are opened; existing world's immutable index is intentionally not rewritten. */
    public static synchronized void configure(boolean enabled, int majorClearance, int minorClearance,
            int maxCandidateChecks, Path overrides) throws IOException {
        validateBounds(majorClearance, minorClearance, maxCandidateChecks);
        Settings next = bundled(majorClearance, minorClearance, maxCandidateChecks);
        if (overrides != null) {
            if (Files.exists(overrides)) {
                try (var reader = Files.newBufferedReader(overrides)) {
                    next = parse(JsonParser.parseReader(reader).getAsJsonObject(), majorClearance, minorClearance, maxCandidateChecks);
                } catch (RuntimeException invalid) {
                    throw new IOException("Invalid Wildshape structure placement policy: " + overrides, invalid);
                }
            } else {
                Files.createDirectories(overrides.toAbsolutePath().getParent());
                try (var input = StructurePlacementProfiles.class.getResourceAsStream("/config/wildshape-structure-placement.json")) {
                    if (input == null) throw new IOException("Missing bundled structure policy");
                    Files.copy(input, overrides);
                }
            }
        }
        settings = new Settings(enabled, maxCandidateChecks, next.approximateBiomeScreening, next.namespaces, next.sets, next.structures);
    }
    private static Settings bundled(int major, int minor, int budget) {
        try (var input = StructurePlacementProfiles.class.getResourceAsStream("/config/wildshape-structure-placement.json")) {
            if (input == null) throw new IllegalStateException("Missing bundled structure policy");
            return parse(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject(), major, minor, budget);
        } catch (IOException e) { throw new IllegalStateException("Cannot read bundled structure policy", e); }
    }
    static Settings parse(JsonObject root, int major, int minor, int budget) {
        validateBounds(major, minor, budget);
        return new Settings(true, budget, root.has("approximateBiomeScreening") && root.get("approximateBiomeScreening").getAsBoolean(), profiles(root, "namespaces", major, minor), profiles(root, "sets", major, minor), profiles(root, "structures", major, minor));
    }
    private static Map<String, Profile> profiles(JsonObject root, String section, int major, int minor) {
        Map<String, Profile> entries = new HashMap<>();
        if (!root.has(section)) return entries;
        for (var entry : root.getAsJsonObject(section).entrySet()) {
            String id = entry.getKey();
            // Detect typos before world generation, rather than silently ignoring an invalid profile.
            if (section.equals("namespaces")) ResourceLocation.fromNamespaceAndPath(id, "validation");
            else ResourceLocation.parse(id);
            JsonObject value = entry.getValue().getAsJsonObject();
            String size = value.has("size") ? value.get("size").getAsString() : "major";
            if (!size.equals("major") && !size.equals("minor")) throw new IllegalArgumentException("Unknown size " + size);
            int clearance = value.has("clearance") ? value.get("clearance").getAsInt() : size.equals("minor") ? minor : major;
            String layer = value.has("layer") ? value.get("layer").getAsString() : "native";
            if (!java.util.Set.of("native", "surface", "underground", "ocean", "sky", "disabled").contains(layer))
                throw new IllegalArgumentException("Unknown layer " + layer);
            int slope = value.has("maxSlopeDelta") ? value.get("maxSlopeDelta").getAsInt() : 32;
            if (clearance < 0 || clearance > 512 || slope < 0 || slope > 256) throw new IllegalArgumentException("Unbounded structure policy " + id);
            boolean water = value.has("rejectWater") && value.get("rejectWater").getAsBoolean();
            String group = value.has("linkedGroup") ? value.get("linkedGroup").getAsString() : "";
            int anchor = value.has("maxAnchorOffset") ? value.get("maxAnchorOffset").getAsInt() : 0;
            if (anchor < 0 || anchor > 256) throw new IllegalArgumentException("Unbounded anchor offset " + id);
            entries.put(id, new Profile(clearance, layer, slope, water, group, anchor));
        }
        return entries;
    }
    private static void validateBounds(int major, int minor, int budget) {
        if (major < 0 || major > 512 || minor < 0 || minor > 512 || budget < 64 || budget > 32768)
            throw new IllegalArgumentException("Structure clearance must be 0..512 blocks and work budget 64..32768");
    }
}
