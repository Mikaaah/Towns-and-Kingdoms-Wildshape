package com.pandaismyname1.ultraterraforged.data.preset;

import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;

/** Verified TNK3 Overworld dungeon sets. Unknown sets keep their native placement. */
final class StructureSetPolicy {
    private static final Map<ResourceLocation, Integer> MINIMUM_SPACING = Map.ofEntries(
            Map.entry("idas:idas_common", 36),
            Map.entry("idas:idas_small", 40),
            Map.entry("idas:idas_rare", 64),
            Map.entry("idas:idas_underground", 40),
            Map.entry("idas:idas_underground_rare", 72),
            Map.entry("idas:idas_ocean", 48),
            Map.entry("dungeons_arise:major_structures", 64),
            Map.entry("dungeons_arise:minor_structures", 48),
            Map.entry("integrated_villages:regular_villages", 40),
            Map.entry("betterdungeons:small_dungeons", 16),
            Map.entry("betterdungeons:skeleton_dungeons", 56),
            Map.entry("betterdungeons:spider_dungeons", 56),
            Map.entry("betterdungeons:zombie_dungeons", 64),
            Map.entry("betterjungletemples:jungle_temples", 36),
            Map.entry("betterdeserttemples:desert_temples", 40),
            Map.entry("betterwitchhuts:witch_hut", 40),
            Map.entry("betterwitchhuts:witch_circle", 48),
            Map.entry("betteroceanmonuments:ocean_monument", 64),
            Map.entry("irons_spellbooks:mountain_tower", 56),
            Map.entry("irons_spellbooks:mangrove_hut", 36),
            Map.entry("apotheosis:towers", 40)
    ).entrySet().stream().collect(Collectors.toUnmodifiableMap(
            entry -> ResourceLocation.parse(entry.getKey()), Map.Entry::getValue));

    private StructureSetPolicy() {}

    static int spacing(ResourceLocation id, int nativeSpacing) {
        return Math.max(nativeSpacing, MINIMUM_SPACING.getOrDefault(id, nativeSpacing));
    }
}
