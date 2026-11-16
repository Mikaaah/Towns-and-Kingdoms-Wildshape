package nl.townsandkingdoms.wildshape.data.preset.tags;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Append-only bridges checked against Blooming Nature 1.1.12, Farmer's Delight 1.3.4 and
 * Thaumaturge's inspected 1.21.1 JAR, Naturalist 2.0.7 and Archie's Dinosaurs 1.0.6. Preset export and append-only packaged overlays share these optional references.
 * Do not duplicate native feature modifiers: doing so changes their rates and can create feature cycles.
 */
final class CurrentPackHabitats {
    private CurrentPackHabitats() {}

    static void addTo(Map<String, List<String>> bridges) {
        // Blooming Nature provides the older c:is_hot / c:is_wet families. Farmer's Delight
        // consumes the scoped common tags; explicit native surface IDs avoid importing other dimensions.
        append(bridges, "c:is_hot/overworld", "bloomingnature:cypress_fields", "bloomingnature:brushland",
                "bloomingnature:baobab_savanna", "bloomingnature:desert_oasis",
                "bloomingnature:desert_river", "bloomingnature:jungle_river");
        append(bridges, "c:is_wet/overworld", "bloomingnature:jungle_river", "bloomingnature:fen",
                "bloomingnature:marshland");
        append(bridges, "c:is_cold/overworld", "bloomingnature:cold_river", "bloomingnature:cold_grassland",
                "bloomingnature:larch_forest", "bloomingnature:highland_woods", "bloomingnature:fen");
        append(bridges, "c:is_dry/overworld", "bloomingnature:cypress_fields", "bloomingnature:brushland",
                "bloomingnature:baobab_savanna", "bloomingnature:desert_oasis", "bloomingnature:desert_river");
        append(bridges, "c:is_tree/coniferous", "bloomingnature:cypress_fields", "bloomingnature:cold_grassland",
                "bloomingnature:larch_forest", "bloomingnature:highland_woods");
        append(bridges, "c:is_tree/deciduous", "bloomingnature:forest_edge", "bloomingnature:flower_glade",
                "bloomingnature:golden_glade", "bloomingnature:aspen_forest");

        // Retain Blooming Nature's native flowers/vines/grass rates in verified surface equivalents.
        append(bridges, "bloomingnature:spawns_flowering_lily_pad", "regions_unexplored:bayou",
                "regions_unexplored:old_growth_bayou", "regions_unexplored:fen",
                "natures_spirit:marsh", "natures_spirit:cypress_fields");
        append(bridges, "bloomingnature:spawns_jungle_fern", "#c:is_jungle");
        append(bridges, "bloomingnature:spawns_pampas_grass", "#c:is_savanna");
        append(bridges, "bloomingnature:spawns_wild_vines", "regions_unexplored:bayou",
                "regions_unexplored:old_growth_bayou", "natures_spirit:marsh", "#c:is_jungle");

        // HAPI 1.1.3 has explicit river/marsh lists; Blooming's river IDs are otherwise absent.
        append(bridges, "hapi:cold_rivers", "bloomingnature:cold_river");
        append(bridges, "hapi:tropical_rivers", "bloomingnature:jungle_river", "bloomingnature:desert_river");
        append(bridges, "hapi:marshes", "bloomingnature:fen", "bloomingnature:marshland");
        append(bridges, "starcatcher:is_cold_river", "bloomingnature:cold_river");
        append(bridges, "starcatcher:is_warm_river", "bloomingnature:jungle_river", "bloomingnature:desert_river");

        // These are the two inspected Thaumaturge selectors that are still vanilla-ID-only.
        String[] deserts = {"regions_unexplored:joshua_desert", "regions_unexplored:saguaro_desert",
                "natures_spirit:stratified_desert", "natures_spirit:lively_dunes", "bloomingnature:desert_oasis"};
        append(bridges, "thaumaturge:has_cinderpearl", deserts);
        append(bridges, "thaumaturge:has_spawn/brainy_husk", deserts);
        // Naturalist 2.0.7's turkey list has no common forest/taiga selectors. Its custom
        // add_animals modifier still owns spawn weights, blacklists and pack-level disabling.
        append(bridges, "naturalist:has_turkey", "regions_unexplored:deciduous_forest",
                "regions_unexplored:temperate_grove", "regions_unexplored:maple_forest",
                "regions_unexplored:boreal_taiga", "regions_unexplored:pine_taiga",
                "natures_spirit:aspen_forest", "natures_spirit:maple_woodlands",
                "natures_spirit:boreal_taiga", "natures_spirit:fir_forest",
                "bloomingnature:forest_edge", "bloomingnature:aspen_forest", "bloomingnature:flower_glade",
                "bloomingnature:larch_forest", "bloomingnature:highland_woods");
        append(bridges, "naturalist:has_ant_hill", "#c:is_jungle", "#c:is_savanna");

        // Archie's Dinosaurs 1.0.6 uses its own template setup, native excavation processors,
        // cave/Nether placement and creature-specific rules. Extend habitats, never replace those setups.
        append(bridges, "ec_ad:amber_fossil_valid", deserts);
        append(bridges, "ec_ad:spawns_therimoss", "#c:is_jungle");
        append(bridges, "ec_ad:spawns_coralosaurs", "hybrid_aquatic:coral_reef",
                "hybrid_aquatic:deep_coral_reef", "hybrid_aquatic:tropical_deep_coral_reef",
                "hybrid_aquatic:deep_warm_ocean");
        // Its cave biome is in is_overworld; mark it underground so surface feature filters can exclude it.
        append(bridges, "c:is_cave", "thaumaturge:magical_forest_caves");
        append(bridges, "c:is_underground", "thaumaturge:magical_forest_caves");
    }

    private static void append(Map<String, List<String>> bridges, String target, String... references) {
        var merged = new LinkedHashSet<>(bridges.getOrDefault(target, List.of()));
        merged.addAll(List.of(references));
        bridges.put(target, List.copyOf(merged));
    }
}
