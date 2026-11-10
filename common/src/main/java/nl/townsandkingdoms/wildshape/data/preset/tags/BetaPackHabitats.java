package nl.townsandkingdoms.wildshape.data.preset.tags;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Verified Beta 1.0 habitats. Optional biome references preserve native modifiers and rates. */
final class BetaPackHabitats {
    private static final List<String> PLAINS = List.of(
            "regions_unexplored:barley_fields",
            "regions_unexplored:clover_plains",
            "regions_unexplored:flower_fields",
            "regions_unexplored:poppy_fields",
            "natures_spirit:prairie",
            "natures_spirit:shrubland");
    private static final List<String> FOREST = List.of(
            "regions_unexplored:alpha_grove",
            "regions_unexplored:deciduous_forest",
            "regions_unexplored:maple_forest",
            "regions_unexplored:temperate_grove",
            "natures_spirit:aspen_forest",
            "natures_spirit:maple_woodlands");
    private static final List<String> FLOWER_FOREST = List.of(
            "regions_unexplored:wisteria_grove",
            "regions_unexplored:mauve_hills",
            "natures_spirit:lavender_fields",
            "natures_spirit:carnation_fields",
            "natures_spirit:floral_ridges");
    private static final List<String> BIRCH_FOREST = List.of(
            "regions_unexplored:silver_birch_forest",
            "natures_spirit:aspen_forest");
    private static final List<String> DARK_FOREST = List.of(
            "regions_unexplored:old_growth_forest",
            "natures_spirit:coniferous_covert");
    private static final List<String> TAIGA = List.of(
            "regions_unexplored:blackwood_taiga",
            "regions_unexplored:boreal_taiga",
            "regions_unexplored:pine_taiga",
            "natures_spirit:boreal_taiga",
            "natures_spirit:fir_forest");
    private static final List<String> JUNGLE = List.of(
            "regions_unexplored:rainforest",
            "regions_unexplored:sparse_rainforest",
            "regions_unexplored:eucalyptus_forest",
            "natures_spirit:tropical_woods",
            "natures_spirit:sparse_tropical_woods",
            "natures_spirit:bamboo_wetlands");
    private static final List<String> SWAMP = List.of(
            "regions_unexplored:bayou",
            "regions_unexplored:fen",
            "regions_unexplored:old_growth_bayou",
            "natures_spirit:marsh",
            "natures_spirit:cypress_fields");
    private static final List<String> DESERT = List.of(
            "regions_unexplored:joshua_desert",
            "regions_unexplored:saguaro_desert",
            "natures_spirit:stratified_desert",
            "natures_spirit:lively_dunes");
    private static final List<String> SAVANNA = List.of(
            "regions_unexplored:baobab_savanna",
            "regions_unexplored:steppe",
            "natures_spirit:arid_savanna",
            "natures_spirit:oak_savanna",
            "natures_spirit:wooded_drylands");
    private static final List<String> GROVE = List.of(
            "regions_unexplored:cold_boreal_taiga",
            "natures_spirit:snowy_fir_forest");
    private static final List<String> WINDSWEPT_FOREST = List.of(
            "regions_unexplored:windswept_maple_forest",
            "natures_spirit:windswept_sugi_forest");

    private BetaPackHabitats() {}

    static void addTo(Map<String, List<String>> bridges) {
        add(bridges, List.of(BIRCH_FOREST, DARK_FOREST, FLOWER_FOREST, FOREST),
                "croptopia:has_crop/spinach");
        add(bridges, List.of(BIRCH_FOREST, DARK_FOREST, FLOWER_FOREST, FOREST, GROVE),
                "croptopia:has_crop/blackbean",
                "croptopia:has_crop/cantaloupe",
                "croptopia:has_crop/cauliflower",
                "croptopia:has_crop/celery",
                "croptopia:has_crop/elderberry",
                "croptopia:has_crop/grape",
                "croptopia:has_crop/radish",
                "croptopia:has_crop/tea_leaves",
                "croptopia:has_crop/tomatillo",
                "croptopia:has_crop/tomato");
        add(bridges, List.of(BIRCH_FOREST, DARK_FOREST, FLOWER_FOREST, FOREST, GROVE, TAIGA),
                "croptopia:has_crop/blackberry",
                "croptopia:has_crop/blueberry",
                "croptopia:has_crop/raspberry",
                "croptopia:has_crop/strawberry");
        add(bridges, List.of(DARK_FOREST),
                "croptopia:has_tree/almond",
                "croptopia:has_tree/cashew",
                "croptopia:has_tree/pecan",
                "croptopia:has_tree/walnut");
        add(bridges, List.of(DESERT),
                "croptopia:has_crop/saguaro");
        add(bridges, List.of(FLOWER_FOREST, FOREST, PLAINS, WINDSWEPT_FOREST),
                "croptopia:has_tree/orange");
        add(bridges, List.of(FLOWER_FOREST, FOREST, WINDSWEPT_FOREST),
                "croptopia:has_tree/apricot",
                "croptopia:has_tree/avocado",
                "croptopia:has_tree/cherry",
                "croptopia:has_tree/lemon",
                "croptopia:has_tree/lime",
                "croptopia:has_tree/nectarine",
                "croptopia:has_tree/pear",
                "croptopia:has_tree/persimmon",
                "croptopia:has_tree/plum",
                "croptopia:has_tree/starfruit");
        add(bridges, List.of(JUNGLE),
                "croptopia:has_crop/basil",
                "croptopia:has_crop/coffee_beans",
                "croptopia:has_crop/eggplant",
                "croptopia:has_crop/garlic",
                "croptopia:has_crop/greenonion",
                "croptopia:has_crop/honeydew",
                "croptopia:has_crop/onion",
                "croptopia:has_crop/peanut",
                "croptopia:has_crop/pineapple",
                "croptopia:has_crop/rhubarb",
                "croptopia:has_crop/rice",
                "croptopia:has_crop/turnip",
                "croptopia:has_crop/vanilla",
                "croptopia:has_tree/banana",
                "croptopia:has_tree/cinnamon",
                "croptopia:has_tree/coconut",
                "croptopia:has_tree/date",
                "croptopia:has_tree/dragonfruit",
                "croptopia:has_tree/fig",
                "croptopia:has_tree/grapefruit",
                "croptopia:has_tree/kumquat",
                "croptopia:has_tree/mango",
                "croptopia:has_tree/nutmeg");
        add(bridges, List.of(PLAINS),
                "croptopia:has_crop/bellpepper",
                "croptopia:has_crop/broccoli",
                "croptopia:has_crop/cabbage",
                "croptopia:has_crop/chile_pepper",
                "croptopia:has_crop/corn",
                "croptopia:has_crop/cucumber",
                "croptopia:has_crop/greenbean",
                "croptopia:has_crop/kale",
                "croptopia:has_crop/lettuce",
                "croptopia:has_crop/mustard",
                "croptopia:has_crop/oat",
                "croptopia:has_crop/pepper",
                "croptopia:has_crop/soybean",
                "croptopia:has_crop/sweetpotato",
                "croptopia:has_tree/apple",
                "croptopia:has_tree/peach");
        add(bridges, List.of(PLAINS, TAIGA),
                "croptopia:has_crop/barley");
        add(bridges, List.of(SAVANNA),
                "croptopia:has_crop/ginger",
                "croptopia:has_crop/hops",
                "croptopia:has_crop/kiwi",
                "croptopia:has_crop/leek",
                "croptopia:has_crop/olive",
                "croptopia:has_crop/turmeric",
                "croptopia:has_crop/yam",
                "croptopia:has_crop/zucchini");
        add(bridges, List.of(SAVANNA, TAIGA),
                "croptopia:has_crop/rutabaga",
                "croptopia:has_crop/squash");
        add(bridges, List.of(SWAMP),
                "croptopia:has_crop/artichoke",
                "croptopia:has_crop/asparagus",
                "croptopia:has_crop/cranberry",
                "croptopia:has_crop/currant");

        // Keep each linked dungeon and its surface obelisk on identical added habitats.
        add(bridges, List.of(PLAINS, FOREST, BIRCH_FOREST, TAIGA, SAVANNA),
                "epic:large_dungeon", "epic:plains_obelisk",
                "epic:medium_plains_dungeon", "epic:small_plains_dungeon");
        add(bridges, List.of(DESERT),
                "epic:large_sand_dungeon", "epic:sand_obelisk",
                "epic:medium_sand_dungeon", "epic:small_sand_dungeon");
        List<String> snowy = List.of("regions_unexplored:frozen_tundra", "regions_unexplored:icy_heights",
                "regions_unexplored:frozen_pine_taiga", "natures_spirit:tundra",
                "natures_spirit:snowy_fir_forest", "natures_spirit:snowy_redwood_forest",
                "natures_spirit:sleeted_slopes", "natures_spirit:snowcapped_red_peaks");
        add(bridges, List.of(snowy),
                "epic:large_ice_dungeon", "epic:ice_obelisk",
                "epic:medium_ice_dungeon", "epic:small_ice_dungeon");
        add(bridges, List.of(List.of("#natures_spirit:is_tropical_woods")),
                "epic:epic_temple", "epic:epic_temple_large", "epic:epic_temple_ruin",
                "apple_cows:enchanted_golden_apple_cow_spawn_biomes");
    }

    private static void add(Map<String, List<String>> bridges, List<List<String>> families, String... targets) {
        var references = new LinkedHashSet<String>();
        families.forEach(references::addAll);
        List<String> values = List.copyOf(references);
        for (String target : targets) {
            if (bridges.putIfAbsent(target, values) != null)
                throw new IllegalStateException("Duplicate habitat bridge: " + target);
        }
    }
}
