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

/** Startup-only tuning of explicitly selected native dungeon grids, without rewriting a save's preset. */
public final class DungeonDensityPolicy {
    public static final double DEFAULT_MULTIPLIER = 1.25D;
    private static final String RESOURCE = "/config/wildshape-dungeon-density.json";
    private static final String APPLIED = "wildshape_density_applied";
    private static volatile Settings settings = bundled(DEFAULT_MULTIPLIER);

    /** The multiplier controls expected candidate opportunities per area, not a guaranteed number of built dungeons. */
    public record Settings(double multiplier, Map<ResourceLocation, Double> selectedSets,
            Set<String> placementTypes, Map<ResourceLocation, Set<ResourceLocation>> companions) {
        public Settings {
            selectedSets = Map.copyOf(selectedSets); placementTypes = Set.copyOf(placementTypes);
            Map<ResourceLocation,Set<ResourceLocation>> frozenCompanions = new HashMap<>();
            companions.forEach((id, group) -> frozenCompanions.put(id, Set.copyOf(group)));
            companions = Map.copyOf(frozenCompanions);
        }
        public double multiplier(ResourceLocation id) {
            Double override = selectedSets.get(id);
            return override == null ? 1.0D : override == 0.0D ? multiplier : override;
        }
        public boolean selected(ResourceLocation id, Set<ResourceLocation> explicitPresets) {
            if (!selectedSets.containsKey(id) || multiplier(id) == 1.0D || explicitPresets.contains(id)) return false;
            for (ResourceLocation companion : companions.getOrDefault(id, Set.of()))
                if (explicitPresets.contains(companion)) return false;
            return true;
        }
    }

    public record Grid(int spacing, int separation) {}
    private DungeonDensityPolicy() {}
    public static Settings settings() { return settings; }

    /** Load before worldgen registries decode. Loading preserves authored JSON and never replaces an existing file. */
    public static synchronized void configure(double multiplier, Path path) throws IOException {
        validate(multiplier);
        Settings next;
        if (path != null && Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                next = parse(JsonParser.parseReader(reader).getAsJsonObject(), multiplier);
            } catch (RuntimeException invalid) { throw new IOException("Invalid Wildshape dungeon density policy: " + path, invalid); }
        } else {
            next = bundled(multiplier);
            if (path != null) {
                Files.createDirectories(path.toAbsolutePath().getParent());
                try (var input = DungeonDensityPolicy.class.getResourceAsStream(RESOURCE)) {
                    if (input == null) throw new IOException("Missing bundled dungeon density policy");
                    Files.copy(input, path);
                }
            }
        }
        settings = next;
    }

    public static boolean selected(ResourceLocation id, Set<ResourceLocation> explicitPresets) {
        return settings.selected(id, explicitPresets);
    }

    /** Called on a fresh source resource at each registry load; the world/datapack on disk remains untouched. */
    public static boolean patch(ResourceLocation id, JsonObject data, Set<ResourceLocation> explicitPresets) {
        return patch(settings, id, data, explicitPresets);
    }

    static boolean patch(Settings policy, ResourceLocation id, JsonObject data, Set<ResourceLocation> explicitPresets) {
        if (!policy.selected(id, explicitPresets) || data.has(APPLIED) || !data.has("placement") || !data.get("placement").isJsonObject()) return false;
        JsonObject placement = data.getAsJsonObject("placement");
        if (!placement.has("type") || !policy.placementTypes.contains(placement.get("type").getAsString())
                || !placement.has("spacing") || !placement.has("separation")) return false;
        int spacing = placement.get("spacing").getAsInt(), separation = placement.get("separation").getAsInt();
        if (spacing < 1 || spacing > 4096 || separation < 0 || separation >= spacing) return false;
        Grid grid = grid(spacing, separation, policy.multiplier(id));
        if (grid.spacing == spacing && grid.separation == separation) return false;
        placement.addProperty("spacing", grid.spacing);
        placement.addProperty("separation", grid.separation);
        // Codec-ignored metadata makes repeated patching of the same in-memory JSON idempotent.
        data.addProperty(APPLIED, true);
        return true;
    }

    public static Grid grid(int spacing, int separation, double opportunityMultiplier) {
        validate(opportunityMultiplier);
        if (spacing < 1 || spacing > 4096 || separation < 0 || separation >= spacing) throw new IllegalArgumentException("Invalid native dungeon grid");
        double scale = 1.0D / Math.sqrt(opportunityMultiplier);
        int adjustedSpacing = Math.max(1, Math.min(4096, (int)Math.round(spacing * scale)));
        int adjustedSeparation = Math.max(0, Math.min(adjustedSpacing - 1, (int)Math.round(separation * scale)));
        return new Grid(adjustedSpacing, adjustedSeparation);
    }

    private static Settings bundled(double multiplier) {
        try (var input = DungeonDensityPolicy.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing bundled dungeon density policy");
            return parse(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject(), multiplier);
        } catch (IOException invalid) { throw new IllegalStateException("Cannot read dungeon density defaults", invalid); }
    }

    static Settings parse(JsonObject root, double multiplier) {
        validate(multiplier);
        Map<ResourceLocation, Double> selected = new HashMap<>();
        for (var entry : root.getAsJsonObject("sets").entrySet()) {
            ResourceLocation id = ResourceLocation.parse(entry.getKey());
            JsonObject profile = entry.getValue().getAsJsonObject();
            double override = 0.0D;
            if (profile.has("candidateOpportunityMultiplier")) { override = profile.get("candidateOpportunityMultiplier").getAsDouble(); validate(override); }
            selected.put(id, override);
        }
        Set<String> types = new HashSet<>();
        for (var type : root.getAsJsonArray("placementTypes")) types.add(ResourceLocation.parse(type.getAsString()).toString());
        Map<ResourceLocation, Set<ResourceLocation>> companions = new HashMap<>();
        if (root.has("linkedGroups")) for (var group : root.getAsJsonObject("linkedGroups").entrySet()) {
            Set<ResourceLocation> members = new HashSet<>();
            for (var member : group.getValue().getAsJsonArray()) members.add(ResourceLocation.parse(member.getAsString()));
            boolean targeted = members.stream().anyMatch(selected::containsKey);
            if (!targeted) continue;
            if (!selected.keySet().containsAll(members)) throw new IllegalArgumentException("Select all linked dungeon companions together: " + group.getKey());
            Double expected = null;
            for (ResourceLocation id : members) {
                double value = selected.get(id) == 0.0D ? multiplier : selected.get(id);
                if (expected != null && Double.compare(expected,value) != 0) throw new IllegalArgumentException("Linked dungeon companions require the same multiplier: " + group.getKey());
                expected = value;
                companions.put(id, Set.copyOf(members));
            }
        }
        return new Settings(multiplier, selected, types, companions);
    }

    private static void validate(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 0.25D || multiplier > 4.0D)
            throw new IllegalArgumentException("Dungeon opportunity multiplier must be finite and in 0.25..4.0");
    }
}
