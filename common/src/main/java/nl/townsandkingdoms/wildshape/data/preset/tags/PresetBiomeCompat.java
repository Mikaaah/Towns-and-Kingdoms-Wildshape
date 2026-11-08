package nl.townsandkingdoms.wildshape.data.preset.tags;

import java.util.List;
import java.util.Map;

/**
 * Append-only habitat bridges, exported with the world's preset rather than applied to unrelated worlds.
 * All references are optional. Native modifiers still decide spawn weights, group sizes and placement checks.
 * Registry names were checked against the TNK3 dependency JARs; see the November 2026 audit.
 */
final class PresetBiomeCompat {
    static final Map<String, List<String>> BRIDGES = Map.ofEntries(
            // Alex's Mobs 1.22.17 still queries these older flat common-tag names.
            Map.entry("c:is_hot_overworld", List.of("#c:is_hot/overworld")),
            Map.entry("c:is_cold_overworld", List.of("#c:is_cold/overworld")),
            Map.entry("c:is_dry_overworld", List.of("#c:is_dry/overworld")),
            Map.entry("c:is_dense_overworld", List.of("#c:is_dense_vegetation/overworld")),
            Map.entry("c:is_coniferous", List.of("#c:is_tree/coniferous")),
            // Integrated API's older collections predate the current c:is_* convention.
            Map.entry("integrated_api:collections/any_forests", List.of("#c:is_forest")),
            Map.entry("integrated_api:collections/birch_forests", List.of("#c:is_birch_forest")),
            Map.entry("integrated_api:collections/deserts", List.of("#c:is_desert")),
            Map.entry("integrated_api:collections/floral", List.of("#c:is_floral", "#natures_spirit:is_flowering")),
            Map.entry("integrated_api:collections/flower_forests", List.of("#c:is_flower_forest")),
            Map.entry("integrated_api:collections/snowy_plains", List.of("#c:is_snowy_plains")),
            Map.entry("integrated_api:collections/swamps", List.of("#c:is_swamp")),
            Map.entry("integrated_api:collections/rivers", List.of("#minecraft:is_river")),
            // HAPI already covers RU's reef, so keep it in the native coral habitat.
            Map.entry("hapi:deep_temperate_oceans", List.of("regions_unexplored:hyacinth_deeps")),
            Map.entry("hapi:temperate_oceans", List.of("regions_unexplored:hyacinth_deeps")),
            Map.entry("hapi:hapi_deep_oceans", List.of("regions_unexplored:hyacinth_deeps")),
            Map.entry("hapi:swamp", List.of("regions_unexplored:old_growth_bayou")),
            Map.entry("hapi:marshes", List.of("regions_unexplored:fen", "regions_unexplored:fungal_fen", "natures_spirit:marsh")),
            Map.entry("hapi:tropical_rivers", List.of("natures_spirit:savanna_river", "natures_spirit:lush_river")),
            Map.entry("hapi:sandy_beaches", List.of("natures_spirit:tropical_shores", "natures_spirit:lukewarm_beach")),
            // Starcatcher uses these tags for fishing habitats; retain its existing overlap between water types.
            Map.entry("starcatcher:is_cold_lake", List.of("#c:is_snowy")),
            Map.entry("starcatcher:is_cold_river", List.of("#hapi:cold_rivers", "natures_spirit:tundra_river", "natures_spirit:alpine_river")),
            Map.entry("starcatcher:is_warm_river", List.of("#hapi:tropical_rivers", "natures_spirit:arid_river", "natures_spirit:xeric_river")),
            Map.entry("starcatcher:is_normal_ocean", List.of("#hapi:temperate_oceans", "#hapi:cold_oceans", "#hapi:lukewarm_oceans")),
            Map.entry("starcatcher:is_warm_ocean", List.of("#hapi:warm_oceans", "#hapi:lukewarm_oceans", "#hapi:coral_reef")),
            Map.entry("starcatcher:is_frozen_ocean", List.of("#hapi:frozen_oceans")),
            // Boss arenas retain their native spacing and encounter rules in matching modded habitats.
            Map.entry("block_factorys_bosses:sandworm_nest", List.of("#c:is_desert")),
            Map.entry("block_factorys_bosses:yeti_hideout", List.of("#c:is_snowy_plains")),
            Map.entry("block_factorys_bosses:dragon_tower", List.of("#c:is_plains", "#c:is_savanna", "#c:is_jungle", "#c:is_swamp")),
            Map.entry("block_factorys_bosses:kraken_ship", List.of("regions_unexplored:hyacinth_deeps")),
            Map.entry("legendary_monsters:has_structure/abandoned_crypt_biomes", List.of("#c:is_mountain")),
            Map.entry("legendary_monsters:has_structure/cloudy_temple_biomes", List.of("#c:is_mountain")),
            Map.entry("legendary_monsters:has_structure/ancient_stronghold_biomes", List.of("#c:is_taiga")),
            Map.entry("legendary_monsters:has_structure/collapsed_kingdom_biomes", List.of("#c:is_plains", "#c:is_birch_forest")),
            Map.entry("legendary_monsters:has_structure/frostbitten_temple_biomes", List.of("#c:is_snowy_plains")),
            Map.entry("legendary_monsters:has_structure/mossy_temple_biomes", List.of("#c:is_jungle")),
            Map.entry("legendary_monsters:has_structure/ruined_pyramid_biomes", List.of("#c:is_desert")),
            // Plains/desert specialists retain their own native spawn modifiers.
            Map.entry("wildernature:spawns_dog", List.of("#c:is_plains")),
            Map.entry("wildernature:spawns_raccoon", List.of("#c:is_plains")),
            Map.entry("wildernature:spawns_scorpion", List.of("#c:is_desert")),
            Map.entry("wildernature:spawns_squirrel", List.of("#minecraft:is_forest", "#minecraft:is_taiga")),
            Map.entry("cataclysm:koboleton_spawn", List.of("#c:is_desert")),
            Map.entry("cataclysm:has_structure/cursed_pyramid_biomes", List.of("#c:is_desert")),
            Map.entry("cataclysm:has_structure/frosted_prison_biomes", List.of("#c:is_snowy_plains")),
            // Explicit ocean IDs avoid admitting Alex's Caves' protected abyssal biome.
            Map.entry("cataclysm:deeplings_spawn", List.of("regions_unexplored:hyacinth_deeps",
                    "hybrid_aquatic:deep_coral_reef", "hybrid_aquatic:tropical_deep_coral_reef", "hybrid_aquatic:deep_warm_ocean")),
            Map.entry("cataclysm:has_structure/sunken_city_biomes", List.of("regions_unexplored:hyacinth_deeps",
                    "hybrid_aquatic:deep_coral_reef", "hybrid_aquatic:tropical_deep_coral_reef", "hybrid_aquatic:deep_warm_ocean"))
    );

    private PresetBiomeCompat() {}
}
